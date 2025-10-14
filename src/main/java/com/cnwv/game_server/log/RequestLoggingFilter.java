package com.cnwv.game_server.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        long start = System.currentTimeMillis();
        String reqId = UUID.randomUUID().toString();

        // 기본 MDC
        MDC.put("requestId", reqId);
        MDC.put("method", req.getMethod());
        MDC.put("path", req.getRequestURI());
        MDC.put("clientIp", getClientIp(req));

        try {
            chain.doFilter(req, res);
        } finally {
            MDC.put("status", String.valueOf(res.getStatus()));
            MDC.put("elapsedMs", String.valueOf(System.currentTimeMillis() - start));
            // 라인 하나 남겨두면 검색에 아주 유용
            org.slf4j.LoggerFactory.getLogger("HTTP").info("HTTP access");
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
