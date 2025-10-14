package com.cnwv.game_server.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretBase64; // base64 인코딩된 시크릿 (properties에 이미 있음)

    @Value("${jwt.accessTokenExpiration:7200000}")      // ms
    private long accessTokenExpirationMs;

    @Value("${jwt.refreshTokenExpiration:2592000000}")  // ms
    private long refreshTokenExpirationMs;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public <T> T getClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    /** 일반적으로 sub(subject)에 username을 넣어둔다 */
    public String extractUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    /** === 여기 추가: JWT에 넣어둔 userId 클레임 파싱 === */
    public Long extractUserId(String token) {
        Number n = getClaim(token, c -> c.get("userId", Number.class));
        return (n != null) ? n.longValue() : null;
    }

    public boolean isTokenValid(String token) {
        try {
            Date exp = getClaim(token, Claims::getExpiration);
            return exp == null || exp.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /** AccessToken 생성 — userId를 클레임에 반드시 포함 */
    public String generateAccessToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username) // sub
                .addClaims(Map.of(
                        "userId", userId      // ★ 필수: 추후 필터/로깅/샤딩에서 사용
                ))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** RefreshToken 생성 — 동일하게 userId를 넣어두면 유용 */
    public String generateRefreshToken(Long userId, String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .addClaims(Map.of(
                        "userId", userId,
                        "type", "refresh" // 구분용(선택)
                ))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
