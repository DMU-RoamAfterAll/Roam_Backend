package com.cnwv.game_server.controller;

import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth")
public class ChatReadController {

    private final RedisService redisService;
    private final JwtUtil jwtUtil;

    @PostMapping("/read")
    @Operation(summary = "채팅 읽음 처리", description = "채팅방에서 마지막으로 읽은 메시지의 timestamp를 기록합니다.")
    public ResponseEntity<?> markAsRead(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("target") String target,
            @RequestParam("timestamp") long timestamp) {

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("❌ Authorization 헤더 오류");
        }

        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(401).body("❌ 토큰이 유효하지 않습니다");
        }

        String username = jwtUtil.extractUsername(token);
        redisService.updateLastRead(username, target, timestamp);

        return ResponseEntity.ok("✅ 읽음 처리 완료");
    }
}
