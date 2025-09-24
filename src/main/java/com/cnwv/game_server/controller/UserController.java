package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.*;
import com.cnwv.game_server.service.AuthService;
import com.cnwv.game_server.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "회원가입, 로그인, 토큰 갱신, 로그아웃")
public class UserController {

    private final AuthService authService;
    private final UserRegistrationService registrationService;

    @PostMapping("/register")
    @Operation(summary = "회원가입(홈 샤드 단일 쓰기)")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username 누락");
        }
        String msg = registrationService.registerOnHomeShard(request.getUsername(), request);
        return ResponseEntity.ok(msg);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인 (액세스/리프레시 발급)")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username/password 누락");
        }
        return ResponseEntity.ok(authService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "리프레시 토큰으로 액세스 재발급")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        // username을 꺼내서 서비스에 넘겨야 AOP 라우팅이 username 기준으로 동작함
        String username = authService.extractUsername(request.getRefreshToken());
        return ResponseEntity.ok(authService.refresh(username, request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 (리프레시 토큰 무효화)")
    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        String username = authService.extractUsername(request.getRefreshToken());
        authService.logout(username, request.getRefreshToken());
        return ResponseEntity.ok("로그아웃 완료");
    }
}
