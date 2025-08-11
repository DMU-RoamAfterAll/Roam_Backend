package com.cnwv.game_server.jwt;

import com.cnwv.game_server.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
            } else {
                log.warn("[JWT] token invalid (signature/exp) for user='{}' {} {}", username, method, uri);
            }
        }

        filterChain.doFilter(request, response);
    }
}
