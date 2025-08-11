package com.cnwv.game_server.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth Debug", description = "인증 상태 확인용 (삭제 예정)")
public class AuthDebugController {

    @GetMapping("/me")
    @Operation(summary = "현재 인증 사용자 확인", description = "SecurityContext의 principal/authorities 반환")
    public ResponseEntity<?> me(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body("No authentication in context");
        }
        return ResponseEntity.ok(Map.of(
                "name", authentication.getName(),
                "authenticated", authentication.isAuthenticated(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }
}
