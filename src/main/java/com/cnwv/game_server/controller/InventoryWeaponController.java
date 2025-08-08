package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.service.InventoryWeaponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/weapons")
@Tag(name = "Inventory Weapon API", description = "무기 삽입/조회/수정/삭제 API")
public class InventoryWeaponController {

    private final InventoryWeaponService weaponService;

    @PostMapping
    @Operation(summary = "무기 삽입", description = "사용자의 인벤토리에 무기를 삽입합니다.")
    public ResponseEntity<?> insertWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode,
            @RequestParam int amount
    ) {
        boolean success = weaponService.insertWeapon(username, weaponCode, amount);
        return success ? ResponseEntity.ok("✅ 무기 삽입 완료") : ResponseEntity.badRequest().body("❌ 삽입 실패");
    }

    @GetMapping
    @Operation(summary = "무기 목록 조회", description = "사용자의 인벤토리에서 무기를 모두 조회합니다.")
    public ResponseEntity<List<InventoryWeapon>> getWeapons(@RequestParam String username) {
        return ResponseEntity.ok(weaponService.getWeapons(username));
    }

    @PutMapping
    @Operation(summary = "무기 수정", description = "무기의 수량을 수정합니다.")
    public ResponseEntity<?> updateWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode,
            @RequestParam int amount
    ) {
        boolean success = weaponService.updateWeapon(username, weaponCode, amount);
        return success ? ResponseEntity.ok("✅ 무기 수정 완료") : ResponseEntity.badRequest().body("❌ 수정 실패");
    }

    @DeleteMapping
    @Operation(summary = "무기 삭제", description = "무기를 인벤토리에서 제거합니다.")
    public ResponseEntity<?> deleteWeapon(
            @RequestParam String username,
            @RequestParam String weaponCode
    ) {
        boolean success = weaponService.deleteWeapon(username, weaponCode);
        return success ? ResponseEntity.ok("✅ 무기 삭제 완료") : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
