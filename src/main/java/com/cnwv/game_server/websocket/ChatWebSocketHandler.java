package com.cnwv.game_server.websocket;

import com.cnwv.game_server.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;

    private final Map<WebSocketSession, String> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);

        if (token == null || !jwtUtil.isTokenValid(token)) {
            session.sendMessage(new TextMessage("인증 실패. 연결 종료됨."));
            session.close();
            return;
        }

        String username = jwtUtil.extractUsername(token);
        sessionUserMap.put(session, username);

        session.sendMessage(new TextMessage("✅ 연결 성공 - 사용자: " + username));
        broadcast("📢 [" + username + "] 님이 입장했습니다.", session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sender = sessionUserMap.get(session);
        String content = message.getPayload();
        broadcast("💬 [" + sender + "]: " + content, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = sessionUserMap.remove(session);
        broadcast("👋 [" + username + "] 님이 퇴장했습니다.", session);
    }

    private void broadcast(String msg, WebSocketSession sender) {
        sessionUserMap.keySet().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msg));
                }
            } catch (Exception e) {
                System.out.println("메시지 전송 오류: " + e.getMessage());
            }
        });
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return null;
    }
}
