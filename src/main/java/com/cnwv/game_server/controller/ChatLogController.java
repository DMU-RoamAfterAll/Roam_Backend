package com.cnwv.game_server.controller;

import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.repository.FriendRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@SecurityRequirement(name = "bearerAuth") // 클래스 전체에 적용
public class ChatLogController {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

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

    @GetMapping("/unread-count")
    @Operation(summary = "안 읽은 메시지 수 조회", description = "target 사용자와의 채팅 중 안 읽은 메시지 수를 반환합니다.")
    public ResponseEntity<?> getUnreadCount(
            @RequestHeader(name = "Authorization", required = true) String authHeader,
            @RequestParam(name = "target") String targetUsername) {

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

        int unreadCount = redisService.getUnreadCount(username, targetUsername);
        return ResponseEntity.ok(Map.of("unread", unreadCount));
    }

    @GetMapping("/list")
    @Operation(summary = "채팅방 목록 + 마지막 메시지 요약", description = "사용자의 채팅방 목록과 마지막 메시지를 조회합니다.")
    public ResponseEntity<?> getChatList(
            @RequestHeader("Authorization") String authHeader
    ) {
        if (!authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("❌ Authorization 헤더 형식 오류");
        }

        String token = authHeader.replace("Bearer ", "");
        if (!jwtUtil.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("❌ 토큰이 유효하지 않습니다.");
        }

        String username = jwtUtil.extractUsername(token);
        List<String> friends = friendRepository.findAllFriendsByUsername(username);

        List<Map<String, Object>> chatSummaries = new ArrayList<>();

        for (String friend : friends) {
            Map<String, Object> last = redisService.getLastMessageInfo(username, friend);
            if (last != null) {
                String nickname = userRepository.findByUsername(friend)
                        .map(u -> u.getNickname())
                        .orElse("(알 수 없음)");
                last.put("target", friend);
                last.put("nickname", nickname);
                chatSummaries.add(last);
            }
        }

        return ResponseEntity.ok(chatSummaries);
    }

}
