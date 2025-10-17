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
    @GetMapping("/{username}")
    public ResponseEntity<SaveGameResponse> get(@PathVariable String username) {
        return ResponseEntity.ok(saveGameService.get(username));
    }

    @Operation(summary = "세이브 최초 생성(이미 있으면 409)")
    @PostMapping("/{username}")
    public ResponseEntity<SaveGameResponse> create(
            @PathVariable String username,
            @RequestBody SaveGameRequest request
    ) {
        var resp = saveGameService.create(username, request);
        return ResponseEntity.created(URI.create("/api/save/" + username)).body(resp);
    }

    @Operation(summary = "세이브 전체 저장(업서트)")
    @PutMapping("/{username}")
    public ResponseEntity<SaveGameResponse> upsert(
            @PathVariable String username,
            @RequestBody SaveGameRequest request
    ) {
        return ResponseEntity.ok(saveGameService.upsert(username, request));
    }
}
