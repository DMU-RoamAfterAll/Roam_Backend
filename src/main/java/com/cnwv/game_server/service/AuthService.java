package com.cnwv.game_server.service;

import com.cnwv.game_server.dto.LoginResponse;
import com.cnwv.game_server.jwt.JwtUtil;
import com.cnwv.game_server.repository.UserRepository;
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

    @WithUserShard(userIdParam = "username")
    @Transactional
    public LoginResponse login(String username, String rawPassword) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디/비번 확인"));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디/비번 확인");
        }

        // ✅ userId와 username 둘 다 전달
        String access  = jwtUtil.generateAccessToken(user.getId(), user.getUsername());
        String refresh = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        user.setRefreshToken(refresh);
        userRepository.save(user);

        var res = new LoginResponse();
        res.setAccessToken(access);
        res.setRefreshToken(refresh);
        return res;
    }

    @WithUserShard(userIdParam = "username")
    @Transactional(readOnly = true)
    public LoginResponse refresh(String username, String refreshToken) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰/계정 확인"));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰/계정 확인");
        }
        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰 만료");
        }

        // ✅ 새 access 발급 시에도 (userId, username) 사용
        var res = new LoginResponse();
        res.setAccessToken(jwtUtil.generateAccessToken(user.getId(), user.getUsername()));
        res.setRefreshToken(refreshToken); // 필요 시 회전(rotate) 전략 사용
        return res;
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public void logout(String username, String refreshToken) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰/계정 확인"));

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰/계정 확인");
        }

        user.setRefreshToken(null);
        userRepository.save(user);
    }
}
