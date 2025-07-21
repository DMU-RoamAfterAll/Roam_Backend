package com.cnwv.game_server.controller;

import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.service.RedisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth") // 클래스 전체에 적용
public class ChatLogController {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @GetMapping("/log")
    @Operation(summary = "1:1 채팅 로그 조회 (Redis 기반)", description = "target 사용자와의 채팅 로그를 조회합니다. 친구 관계일 때만 허용됩니다.")
    public ResponseEntity<?> getChatLog(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("target") String targetUsername) {

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Authorization 헤더 형식 오류");
        }

        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ 토큰이 유효하지 않습니다.");
        }

        String username = jwtUtil.extractUsername(token);
        if (!redisService.isFriend(username, targetUsername)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ 친구 관계가 아닙니다.");
        }

        List<String> chatLog = redisService.getChatLog(username, targetUsername);
        return ResponseEntity.ok(chatLog);
    }
}
