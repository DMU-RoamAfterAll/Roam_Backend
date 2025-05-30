package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.*;
import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.model.User;
import com.cnwv.game_server.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User API", description = "회원가입, 로그인, 토큰 갱신, 로그아웃 관련 API")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "가입 정보를 받아서 회원 등록")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 형식")
    public String register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setBirthDate(LocalDate.parse(request.getBirthDate()));
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now()); // 생략 가능, 필드에 기본값 있음
        userRepository.save(user);
        return "회원가입 성공";
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "username과 password로 로그인 후, JWT access/refresh 토큰을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 토큰 반환")
    @ApiResponse(responseCode = "401", description = "비밀번호가 일치하지 않습니다.")
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
    @Operation(summary = "토큰 재발급", description = "Refresh Token을 이용해 새로운 Access Token과 Refresh Token을 발급합니다.")
    @ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    @ApiResponse(responseCode = "400", description = "Refresh Token이 필요합니다.")
    @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token입니다.")
    @ApiResponse(responseCode = "404", description = "유저 없음")
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

        // 새로운 Access/Refresh Token 생성
        String newAccessToken = jwtUtil.generateAccessToken(username);
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        // 새로운 Refresh Token 저장
        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return new JwtResponse(newAccessToken, newRefreshToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자의 Refresh Token을 제거하여 로그아웃 처리합니다.")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @ApiResponse(responseCode = "400", description = "Refresh Token이 필요합니다.")
    @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token입니다.")
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