package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.*;
import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.service.UserDualWriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private final UserDualWriteService userDualWriteService;

    @PostMapping("/register")
    @Operation(summary = "회원가입 (양쪽 샤드 동시 반영)", description = "s0에 저장 후 s1에도 동일 id로 동기 삽입")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식")
    public String register(@RequestBody RegisterRequest request) {
        return userDualWriteService.registerBothShards(request);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "username/password로 로그인, JWT 발급")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "401", description = "비밀번호 불일치")
    @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    public JwtResponse login(@RequestBody LoginRequest request) {
        User foundUser = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자"));

        if (!passwordEncoder.matches(request.getPassword(), foundUser.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다");
        }

        String accessToken = jwtUtil.generateAccessToken(foundUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(foundUser.getUsername());

        foundUser.setRefreshToken(refreshToken);
        userRepository.save(foundUser);

        return new JwtResponse(accessToken, refreshToken);
    }

    @PostMapping("/refresh")
    public JwtResponse refresh(@RequestBody RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
        }

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
    public String logout(@RequestBody LogoutRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh Token이 필요합니다.");
        }
        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다.");
        }

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
