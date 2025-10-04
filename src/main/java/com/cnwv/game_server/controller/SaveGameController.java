package com.cnwv.game_server.controller;

import com.cnwv.game_server.dto.SaveGameRequest;
import com.cnwv.game_server.dto.SaveGameResponse;
import com.cnwv.game_server.service.SaveGameService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/save")
@RequiredArgsConstructor
public class SaveGameController {

    private final SaveGameService saveGameService;

    @Operation(summary = "세이브 조회(없으면 404)")
    @GetMapping("/{userId}")
    public ResponseEntity<SaveGameResponse> get(@PathVariable Long userId) {
        return ResponseEntity.ok(saveGameService.get(userId));
    }

    @Operation(summary = "세이브 최초 생성(이미 있으면 409)")
    @PostMapping("/{userId}")
    public ResponseEntity<SaveGameResponse> create(
            @PathVariable Long userId,
            @RequestBody SaveGameRequest request
    ) {
        var resp = saveGameService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/save/" + userId)).body(resp);
    }

    @Operation(summary = "세이브 전체 저장(업서트)")
    @PutMapping("/{userId}")
    public ResponseEntity<SaveGameResponse> upsert(
            @PathVariable Long userId,
            @RequestBody SaveGameRequest request
    ) {
        return ResponseEntity.ok(saveGameService.upsert(userId, request));
    }
}
