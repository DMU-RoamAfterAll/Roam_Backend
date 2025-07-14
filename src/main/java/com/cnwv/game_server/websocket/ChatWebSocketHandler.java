package com.cnwv.game_server.websocket;

import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.redis.RedisSubscriber;
import com.cnwv.game_server.service.RedisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final StringRedisTemplate redisTemplate;
    private final ChannelTopic topic;

    // 세션 ↔ 사용자 연결 관리
    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    public Map<WebSocketSession, String> getSessionUserMap() {
        return sessionUserMap;
    }

    @PostConstruct
    public void init() {
        RedisSubscriber.setMessageHandler(this::sendToAllSessions);
        log.info("[WebSocket] RedisSubscriber 메시지 핸들러 등록 완료");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("🔌 WebSocket 연결 시도됨 | URI: {}", session.getUri());

        String token = extractToken(session);
        log.info("받은 토큰: {}", token);

        if (token == null || !jwtUtil.isTokenValid(token)) {
            log.warn("❌ 토큰 인증 실패");
            session.sendMessage(new TextMessage("인증 실패. 연결 종료됨."));
            session.close();
            return;
        }

        String username = jwtUtil.extractUsername(token);
        sessionUserMap.put(session, username);
        redisService.setOnline(username);

        session.sendMessage(new TextMessage("✅ 연결 성공 - 사용자: " + username));
        broadcast("📢 [" + username + "] 님이 입장했습니다.");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String sender = sessionUserMap.get(session);
        String content = message.getPayload();
        broadcast("💬 [" + sender + "]: " + content);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = sessionUserMap.remove(session);
        if (username != null) {
            redisService.removeOnline(username);
            broadcast("👋 [" + username + "] 님이 퇴장했습니다.");
        }
    }

    private void broadcast(String msg) {
        redisTemplate.convertAndSend(topic.getTopic(), msg);
    }

    private void sendToAllSessions(String msg) {
        for (WebSocketSession session : sessionUserMap.keySet()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msg));
                }
            } catch (Exception e) {
                log.error("[WebSocket] 메시지 전송 실패: {}", e.getMessage(), e);
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return null;
    }
}
