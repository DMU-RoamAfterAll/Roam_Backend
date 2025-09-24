package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.dto.WeaponDataResponse;
import com.cnwv.game_server.service.InventoryWeaponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/weapons")
@Tag(name = "Inventory Weapon API", description = "무기 삽입/조회/수정/삭제 + 전역 조회(UNION ALL)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryWeaponController {

    private final InventoryWeaponService weaponService;
    private final JdbcTemplate jdbcTemplate; // 전역 조회(UNION ALL)용

    // ---------- 단일 유저(샤드 라우팅) ----------
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
    @Operation(
            summary = "무기 목록 조회(내 데이터)",
            description = "{ weaponCode, amount }만 반환"
    )
    public ResponseEntity<List<WeaponDataResponse>> getWeapons(@RequestParam String username) {
        List<InventoryWeapon> list = weaponService.getWeapons(username);
        List<WeaponDataResponse> dtos = list.stream()
                .map(w -> new WeaponDataResponse(
                        w.getId().getWeaponCode(),
                        w.getAmount()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
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

    // ---------- 전역 조회(UNION ALL) ----------
    @GetMapping("/global/search")
    @Operation(summary = "전역 무기 검색(UNION ALL)",
            description = "weaponCode 필터 + created_at DESC 정렬 + limit/offset 페이징. username 포함 반환")
    public ResponseEntity<List<Map<String, Object>>> searchWeaponsGlobal(
            @RequestParam String weaponCode,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        limit = Math.max(1, Math.min(limit, 200));
        offset = Math.max(0, offset);

        String sql =
                "SELECT 's0' AS shard, u.username, w.weapon_code, w.amount, w.created_at " +
                        "  FROM cnwvdb_s0.inventory_weapons w " +
                        "  JOIN cnwvdb_s0.inventories inv ON inv.id = w.inventory_id " +
                        "  JOIN cnwvdb_s0.users u ON u.id = inv.user_id " +
                        " WHERE w.weapon_code = ? " +
                        "UNION ALL " +
                        "SELECT 's1' AS shard, u.username, w.weapon_code, w.amount, w.created_at " +
                        "  FROM cnwvdb_s1.inventory_weapons w " +
                        "  JOIN cnwvdb_s1.inventories inv ON inv.id = w.inventory_id " +
                        "  JOIN cnwvdb_s1.users u ON u.id = inv.user_id " +
                        " WHERE w.weapon_code = ? " +
                        "ORDER BY created_at DESC " +
                        "LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(sql, weaponCode, weaponCode, limit, offset);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/global/top")
    @Operation(summary = "전역 무기 TOP-N(샤드 합산)",
            description = "weapon_code별 amount 합계를 샤드 합산으로 내림차순 상위 N개")
    public ResponseEntity<List<Map<String, Object>>> topWeaponsGlobal(
            @RequestParam(defaultValue = "10") int limit
    ) {
        limit = Math.max(1, Math.min(limit, 200));

        String sql =
                "SELECT weapon_code, SUM(amount) AS total_amount " +
                        "FROM ( " +
                        "  SELECT w.weapon_code, w.amount FROM cnwvdb_s0.inventory_weapons w " +
                        "  UNION ALL " +
                        "  SELECT w.weapon_code, w.amount FROM cnwvdb_s1.inventory_weapons w " +
                        ") t " +
                        "GROUP BY weapon_code " +
                        "ORDER BY total_amount DESC " +
                        "LIMIT ?";

        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, limit));
    }
}
