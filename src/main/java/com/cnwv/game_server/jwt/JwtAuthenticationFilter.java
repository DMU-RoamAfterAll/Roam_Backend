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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();
        final String method = request.getMethod();

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JWT] skip (no header) {} {}", method, uri);
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 1) username 추출(토큰 subject = username)
        final String username;
        try {
            username = jwtUtil.extractUsername(token);
            log.debug("[JWT] header OK, username='{}' {} {}", username, method, uri);
        } catch (Exception e) {
            log.warn("[JWT] extractUsername failed: {} | {} {}", e.getMessage(), method, uri);
            chain.doFilter(request, response); // 인증 없이 계속 진행
            return;
        }

        // 2) 실제 인증 세팅
        try {
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // DB 조회(존재 안 하면 UsernameNotFoundException)
                var userDetails = userDetailsService.loadUserByUsername(username);

                // 서명/만료 검사
                boolean valid = jwtUtil.isTokenValid(token);
                log.debug("[JWT] isTokenValid={} for user='{}' {} {}", valid, username, method, uri);

                if (valid) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JWT] setAuthentication OK for user='{}' {} {}", username, method, uri);

                    // MDC(선택)
                    MDC.put("username", username);
                    try {
                        Long userId = jwtUtil.extractUserId(token); // 있으면 사용
                        if (userId != null) MDC.put("userId", String.valueOf(userId));
                    } catch (Exception ignore) {}
                } else {
                    log.warn("[JWT] token invalid (signature/exp) for user='{}' {} {}", username, method, uri);
                }
            }
            chain.doFilter(request, response);

        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException e) {
            // ★ 핵심: 여기서 500 방지
            log.warn("[JWT] user not found for username='{}' {} {}", username, method, uri);
            chain.doFilter(request, response);  // 인증 없이 계속 → 보안 규칙에 따라 401/403
        } catch (Exception e) {
            // 예기치 못한 예외도 500 방지
            log.error("[JWT] unexpected error {} {}: {}", method, uri, e.toString());
            chain.doFilter(request, response);
        } finally {
            MDC.remove("username");
            MDC.remove("userId");
        }
    }
}
