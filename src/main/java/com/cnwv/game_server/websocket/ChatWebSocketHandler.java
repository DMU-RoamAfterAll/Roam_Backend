package com.cnwv.game_server.websocket;

import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final ObjectMapper objectMapper;

    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, Set<WebSocketSession>> channelSessionMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[WebSocket] 초기화 완료");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        String token = extractQueryParam(uri, "token");
        String target = extractQueryParam(uri, "target");

        if (token == null || !jwtUtil.isTokenValid(token)) {
            session.sendMessage(new TextMessage("인증 실패. 연결 종료됨."));
            session.close();
            return;
        }

        String username = jwtUtil.extractUsername(token);
        sessionUserMap.put(session, username);
        redisService.setOnline(username);

        if (target != null && !redisService.isFriend(username, target)) {
            session.sendMessage(new TextMessage("❌ 친구 관계가 아닙니다. 채팅이 차단됩니다."));
            return;
        }

        String channel = getPrivateChannel(username, target);
        subscribeToChannel(channel);

        channelSessionMap.computeIfAbsent(channel, k -> ConcurrentHashMap.newKeySet()).add(session);

        Map<String, String> connectMsg = Map.of("type", "system", "message", username + " 입장");
        redisService.publishMessage(channel, objectMapper.writeValueAsString(connectMsg));

        session.sendMessage(new TextMessage("✅ 연결 성공 - 사용자: " + username));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sender = sessionUserMap.get(session);
        String target = extractQueryParam(session.getUri(), "target");
        String content = message.getPayload();

        if (content.length() > 200) {
            session.sendMessage(new TextMessage("❌ 메시지가 너무 깁니다."));
            return;
        }

        if (content.length() < 1){
            session.sendMessage(new TextMessage("❌ 공백의 메시지는 전송되지 않습니다."));
            return;
        }

        if (containsForbiddenWords(content)) {
            session.sendMessage(new TextMessage("❌ 부적절한 단어가 포함되어 전송되지 않았습니다."));
            return;
        }

        String channel = getPrivateChannel(sender, target);
        long timestamp = System.currentTimeMillis();

        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "chat");
        msg.put("sender", sender);
        msg.put("message", content);
        msg.put("timestamp", timestamp);

        String jsonMsg = objectMapper.writeValueAsString(msg);
        redisService.publishMessage(channel, jsonMsg);
    }

    private boolean containsForbiddenWords(String content) {
        List<String> forbiddenWords = List.of("욕설", "나쁜말", "씨발", "ㅅㅂ", "fuck", "shit");
        return forbiddenWords.stream().anyMatch(content::contains);
    }

    private void subscribeToChannel(String channel) {
        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                String payload = new String(message.getBody(), StandardCharsets.UTF_8);
                Set<WebSocketSession> sessions = channelSessionMap.getOrDefault(channel, Set.of());
                sessions.forEach(s -> {
                    try {
                        if (s.isOpen()) {
                            s.sendMessage(new TextMessage(payload));
                        }
                    } catch (Exception e) {
                        log.error("[WebSocket] 전송 실패: {}", e.getMessage());
                    }
                });
            }
        };
        listenerContainer.addMessageListener(listener, new ChannelTopic(channel));
    }

    private String extractQueryParam(URI uri, String key) {
        if (uri != null && uri.getQuery() != null) {
            for (String param : uri.getQuery().split("&")) {
                if (param.startsWith(key + "=")) {
                    return param.substring(key.length() + 1);
                }
            }
        }
        return null;
    }

    private String getPrivateChannel(String userA, String userB) {
        List<String> users = Arrays.asList(userA, userB);
        Collections.sort(users);
        return "chat:" + users.get(0) + ":" + users.get(1);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = sessionUserMap.remove(session);
        if (username != null) redisService.removeOnline(username);
    }
}
