package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.PlayerStatsDeltaRequest;
import com.cnwv.game_server.dto.PlayerStatsResponse;
import com.cnwv.game_server.dto.PlayerStatsUpdateRequest;
import com.cnwv.game_server.service.PlayerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 기본 베이스: /api/player-stats
 * - GET  /{userId}                : 조회(없으면 생성)
 * - PUT  /{userId}                : 절대값 업데이트(부분 허용)
 * - PATCH/{userId}                : 증감치 적용(+/-)
 */
@RestController
@RequestMapping("/api/player-stats")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService service;

    @GetMapping("/{userId}")
    @Operation(summary = "조회(없으면 생성)")
    public ResponseEntity<PlayerStatsResponse> get(@PathVariable long userId) {
        return ResponseEntity.ok(service.getOrCreate(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "절대값 업데이트(부분 허용)")
    public ResponseEntity<PlayerStatsResponse> update(
            @PathVariable long userId,
            @RequestBody PlayerStatsUpdateRequest req) {
        return ResponseEntity.ok(service.update(userId, req));
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "증감치 적용(+/-)")
    public ResponseEntity<PlayerStatsResponse> delta(
            @PathVariable long userId,
            @RequestBody PlayerStatsDeltaRequest req) {
        return ResponseEntity.ok(service.applyDelta(userId, req));
    }
}
