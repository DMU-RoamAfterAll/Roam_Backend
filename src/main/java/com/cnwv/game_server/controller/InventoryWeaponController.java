package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.service.InventoryWeaponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/weapons")
@Tag(name = "Inventory Weapon API", description = "무기 삽입/조회/수정/삭제 API (username 사용)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryWeaponController {

    private final InventoryWeaponService weaponService;

    @PostMapping
    @Operation(summary = "무기 삽입 (중복시 수량 증가)")
    public ResponseEntity<?> insertWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode,
            @RequestParam int amount
    ) {
        boolean success = weaponService.insertWeapon(username, weaponCode, amount);
        return success ? ResponseEntity.ok("✅ 무기 삽입/증가 완료")
                : ResponseEntity.badRequest().body("❌ 삽입 실패");
    }

    @GetMapping
    @Operation(summary = "무기 목록 조회")
    public ResponseEntity<List<InventoryWeapon>> getWeapons(@RequestParam String username) {
        return ResponseEntity.ok(weaponService.getWeapons(username));
    }

    @PutMapping
    @Operation(summary = "무기 수량 설정 (절대값)")
    public ResponseEntity<?> updateWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode,
            @RequestParam int amount
    ) {
        boolean success = weaponService.updateWeapon(username, weaponCode, amount);
        return success ? ResponseEntity.ok("✅ 무기 수량 설정 완료")
                : ResponseEntity.badRequest().body("❌ 수정 실패");
    }

    @DeleteMapping
    @Operation(summary = "무기 수량 차감/삭제 (amount 만큼 차감, 0 이하 시 삭제)")
    public ResponseEntity<?> deleteWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode,
            @RequestParam int amount
    ) {
        boolean success = weaponService.deleteWeapon(username, weaponCode, amount);
        return success ? ResponseEntity.ok("✅ 무기 차감/삭제 처리 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
