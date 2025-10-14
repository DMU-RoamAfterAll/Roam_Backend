package com.cnwv.game_server.service;

import com.cnwv.game_server.dto.LoginResponse;
import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.ShardContext;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public String extractUsername(String token) {
        try {
            return jwtUtil.extractUsername(token);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰");
        }
    }

    /** 로그인: username으로 샤드 라우팅(Aspect), 비번검증 → 토큰발급(userId 포함) */
    @WithUserShard(userIdParam = "username")
    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디/비번 확인"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디/비번 확인");
        }

        String access  = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        user.setRefreshToken(refresh);
        userRepository.save(user);

        var res = new LoginResponse();
        res.setAccessToken(access);
        res.setRefreshToken(refresh);
        return res;
    }

    /**
     * 리프레시: 클라이언트는 refreshToken만 보냄.
     * - 토큰에서 userId/username 추출 → userId로 샤드 결정 → 해당 샤드에서 유저 조회
     * - 저장된 refresh와 일치/유효성 확인 → 새 access 발급(필요 시 refresh 회전)
     */
    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        // 1) 기본 검증
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 누락");
        }
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 만료/무효");
        }

        // 2) 토큰에서 사용자 정보 추출
        Long userId;
        String username;
        try {
            userId = jwtUtil.extractUserId(refreshToken);
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 파싱 실패");
        }
        if (userId == null || username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 클레임 누락");
        }

        // 3) 샤드 설정(userId 기반). s0/s1 2샤드 가정: userId % 2
        String shard = (userId % 2 == 0) ? "s0" : "s1";
        ShardContext.set(shard);
        try {
            // 4) 해당 샤드에서 유저 조회(가능하면 id로 조회가 더 안전)
            var userOpt = userRepository.findById(userId);
            var user = userOpt.orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정 없음"));

            // 5) 저장된 refresh와 일치하는지 확인
            if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 불일치");
            }

            // 6) 새 access 발급 (필요하면 refresh도 회전)
            String newAccess = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
            // 회전 사용 시:
            // String newRefresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
            // user.setRefreshToken(newRefresh);
            // userRepository.save(user);

            var res = new LoginResponse();
            res.setAccessToken(newAccess);
            // res.setRefreshToken(newRefresh); // 회전 시
            res.setRefreshToken(refreshToken);   // 회전 안 할 때
            return res;

        } finally {
            ShardContext.clear();
        }
    }

    /**
     * 로그아웃: refreshToken만 송신한다고 가정.
     * - 토큰에서 userId/username 추출 → 샤드 설정 → 해당 유저의 refreshToken 무효화
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 누락");
        }
        Long userId;
        String username;
        try {
            userId = jwtUtil.extractUserId(refreshToken);
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 파싱 실패");
        }
        if (userId == null || username == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 클레임 누락");
        }

        String shard = (userId % 2 == 0) ? "s0" : "s1";
        ShardContext.set(shard);
        try {
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "계정 없음"));

            if (!refreshToken.equals(user.getRefreshToken())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 불일치");
            }
            user.setRefreshToken(null);
            userRepository.save(user);
        } finally {
            ShardContext.clear();
        }
    }
}
