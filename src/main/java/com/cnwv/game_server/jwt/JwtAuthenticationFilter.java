package com.cnwv.game_server.jwt;

import com.cnwv.game_server.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT] skip (no header) {} {}", method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String username = null;

        try {
            username = jwtUtil.extractUsername(token);
            log.debug("[JWT] header OK, username='{}' {} {}", username, method, uri);
        } catch (Exception e) {
            log.warn("[JWT] extractUsername failed: {} | {} {}", e.getMessage(), method, uri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var userDetails = userDetailsService.loadUserByUsername(username);

                boolean valid = jwtUtil.isTokenValid(token);
                log.debug("[JWT] isTokenValid={} for user='{}' {} {}", valid, username, method, uri);

                if (valid) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JWT] setAuthentication OK for user='{}' {} {}", username, method, uri);

                    // === ★★★ MDC 주입 (로그에 쓰일 컨텍스트) ★★★
                    MDC.put("username", username);
                    // userId를 토큰에서 바로 뽑을 수 있으면 같이 넣기 (없으면 생략)
                    try {
                        Long userId = jwtUtil.extractUserId(token); // JwtUtil에 이 메서드가 없다면 제거 또는 구현
                        if (userId != null) {
                            MDC.put("userId", String.valueOf(userId));
                        }
                    } catch (Exception ignore) {
                        // userId 클레임이 없는 토큰이면 무시
                    }
                } else {
                    log.warn("[JWT] token invalid (signature/exp) for user='{}' {} {}", username, method, uri);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // ⚠️ RequestLoggingFilter 등에서 clear()를 책임진다면 아래 remove는 생략 가능
            // 여기서 지워두면 스레드 재사용에 따른 누수 방지에 도움이 됨
            MDC.remove("username");
            MDC.remove("userId");
        }
    }
}
