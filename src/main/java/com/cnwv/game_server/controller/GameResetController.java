package com.cnwv.game_server.controller;

import com.cnwv.game_server.service.GameResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reset")
@RequiredArgsConstructor
@Tag(name = "Game Reset API", description = "유저 진행 데이터 완전 초기화(삭제 전용)")
@SecurityRequirement(name = "bearerAuth")
public class GameResetController {

    private final GameResetService resetService;

    @PostMapping("/hard")
    @Operation(summary = "완전 초기화(삭제만)", description = "스탯/인벤토리/무기/아이템/플래그/스킬 등 진행 데이터 전부 삭제")
    public ResponseEntity<Void> hardReset(@RequestParam String username) {
        resetService.hardReset(username);
        return ResponseEntity.noContent().build(); // 204
    }
}
