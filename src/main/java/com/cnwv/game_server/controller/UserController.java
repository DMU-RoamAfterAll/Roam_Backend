package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.*;
import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.service.UserRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API")
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserRegistrationService registrationService;

    @PostMapping("/register")
    @Operation(summary = "회원가입(홈 샤드 단일 쓰기)", description = "username으로 샤드 라우팅 후 users + inventories 생성")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "409", description = "중복")
    public String register(@RequestBody RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username 누락");
        }
        return registrationService.registerOnHomeShard(request.getUsername(), request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "username/password 검증 후 JWT 발급")
    public JwtResponse login(@RequestBody LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username/password 누락");
        }
        User foundUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자"));
        // password 검증은 SecurityConfig의 PasswordEncoder 사용 (서비스에서 수행해도 OK)
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "패스워드 검증 로직은 기존 코드에 맞춰 사용하세요.");
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급")
    public JwtResponse refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        String username;
        try { username = jwtUtil.extractUsername(refreshToken); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."); }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저 없음"));

        if (!refreshToken.equals(user.getRefreshToken()) || !jwtUtil.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않음");
        }

        String newAccessToken = jwtUtil.generateAccessToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);
        return new JwtResponse(newAccessToken, newRefreshToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token 제거")
    public String logout(@RequestBody LogoutRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        String username;
        try { username = jwtUtil.extractUsername(refreshToken); }
        catch (Exception e) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."); }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "유저 없음"));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token이 일치하지 않습니다.");
        }
        user.setRefreshToken(null);
        userRepository.save(user);
        return "로그아웃 완료";
    }
}
