package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryItem;
import com.cnwv.game_server.dto.ItemDataResponse;
import com.cnwv.game_server.service.InventoryItemService;
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
@RequestMapping("/api/inventory/items")
@Tag(name = "Inventory Item API", description = "아이템 삽입/조회/수정/삭제 + 전역 조회(UNION ALL)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryItemController {

    private final InventoryItemService itemService;
    private final JdbcTemplate jdbcTemplate; // 전역 조회(UNION ALL)용

    // ---------- 단일 유저(샤드 라우팅) ----------
    @PostMapping
    @Operation(summary = "아이템 삽입 (중복시 수량 증가)")
    public ResponseEntity<?> insertItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.insertItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 삽입/증가 완료")
                : ResponseEntity.badRequest().body("❌ 삽입 실패");
    }

    @GetMapping
    @Operation(
            summary = "아이템 목록 조회(내 데이터)",
            description = "inventoryId / createdAt 제외, { itemCode, amount }만 반환"
    )
    public ResponseEntity<List<ItemDataResponse>> getItems(@RequestParam String username) {
        List<InventoryItem> list = itemService.getItems(username);
        List<ItemDataResponse> dtos = list.stream()
                .map(i -> new ItemDataResponse(
                        i.getId().getItemCode(),
                        i.getAmount()
                ))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PutMapping
    @Operation(summary = "아이템 수량 설정 (절대값)")
    public ResponseEntity<?> updateItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.updateItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 수량 설정 완료")
                : ResponseEntity.badRequest().body("❌ 수정 실패");
    }

    @DeleteMapping
    @Operation(summary = "아이템 수량 차감/삭제 (amount 만큼 차감, 0 이하 시 삭제)")
    public ResponseEntity<?> deleteItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.deleteItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 차감/삭제 처리 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }

    // ---------- 전역 조회(UNION ALL) ----------
    @GetMapping("/global/search")
    @Operation(summary = "전역 아이템 검색(UNION ALL)",
            description = "itemCode 필터 + created_at DESC 정렬 + limit/offset 페이징. username 포함 반환")
    public ResponseEntity<List<Map<String, Object>>> searchItemsGlobal(
            @RequestParam String itemCode,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        limit = Math.max(1, Math.min(limit, 200));
        offset = Math.max(0, offset);

        String sql =
                "SELECT 's0' AS shard, u.username, i.item_code, i.amount, i.created_at " +
                        "  FROM cnwvdb_s0.inventory_items i " +
                        "  JOIN cnwvdb_s0.inventories inv ON inv.id = i.inventory_id " +
                        "  JOIN cnwvdb_s0.users u ON u.id = inv.user_id " +
                        " WHERE i.item_code = ? " +
                        "UNION ALL " +
                        "SELECT 's1' AS shard, u.username, i.item_code, i.amount, i.created_at " +
                        "  FROM cnwvdb_s1.inventory_items i " +
                        "  JOIN cnwvdb_s1.inventories inv ON inv.id = i.inventory_id " +
                        "  JOIN cnwvdb_s1.users u ON u.id = inv.user_id " +
                        " WHERE i.item_code = ? " +
                        "ORDER BY created_at DESC " +
                        "LIMIT ? OFFSET ?";

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(sql, itemCode, itemCode, limit, offset);
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/global/top")
    @Operation(summary = "전역 아이템 TOP-N(샤드 합산)",
            description = "item_code별 amount 합계를 샤드 합산으로 내림차순 상위 N개")
    public ResponseEntity<List<Map<String, Object>>> topItemsGlobal(
            @RequestParam(defaultValue = "10") int limit
    ) {
        limit = Math.max(1, Math.min(limit, 200));

        String sql =
                "SELECT item_code, SUM(amount) AS total_amount " +
                        "FROM ( " +
                        "  SELECT i.item_code, i.amount FROM cnwvdb_s0.inventory_items i " +
                        "  UNION ALL " +
                        "  SELECT i.item_code, i.amount FROM cnwvdb_s1.inventory_items i " +
                        ") t " +
                        "GROUP BY item_code " +
                        "ORDER BY total_amount DESC " +
                        "LIMIT ?";

        return ResponseEntity.ok(jdbcTemplate.queryForList(sql, limit));
    }
}
