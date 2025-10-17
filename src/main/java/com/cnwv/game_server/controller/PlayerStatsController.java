package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.PlayerStatsDeltaRequest;
import com.cnwv.game_server.dto.PlayerStatsResponse;
import com.cnwv.game_server.dto.PlayerStatsUpdateRequest;
import com.cnwv.game_server.service.PlayerStatsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player-stats")
@RequiredArgsConstructor
public class PlayerStatsController {

    private final PlayerStatsService service;

    @GetMapping("/{username}")
    @Operation(summary = "조회(없으면 생성)")
    public ResponseEntity<PlayerStatsResponse> get(@PathVariable String username) {
        return ResponseEntity.ok(service.getOrCreate(username));
    }

    @PutMapping("/{username}")
    @Operation(summary = "절대값 업데이트(부분 허용)")
    public ResponseEntity<PlayerStatsResponse> update(
            @PathVariable String username,
            @RequestBody PlayerStatsUpdateRequest req) {
        return ResponseEntity.ok(service.update(username, req));
    }

    @PatchMapping("/{username}")
    @Operation(summary = "증감치 적용(+/-)")
    public ResponseEntity<PlayerStatsResponse> delta(
            @PathVariable String username,
            @RequestBody PlayerStatsDeltaRequest req) {
        return ResponseEntity.ok(service.applyDelta(username, req));
    }
}
