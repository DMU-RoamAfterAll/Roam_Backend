package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryItem;
import com.cnwv.game_server.service.InventoryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/items")
@Tag(name = "Inventory Item API", description = "아이템 삽입/조회/수정/삭제 API")
public class InventoryItemController {

    private final InventoryItemService itemService;

    @PostMapping
    @Operation(summary = "아이템 삽입", description = "사용자의 인벤토리에 아이템을 삽입합니다.")
    public ResponseEntity<?> insertItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.insertItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 삽입 완료") : ResponseEntity.badRequest().body("❌ 삽입 실패");
    }

    @GetMapping
    @Operation(summary = "아이템 목록 조회", description = "사용자의 인벤토리에서 아이템을 모두 조회합니다.")
    public ResponseEntity<List<InventoryItem>> getItems(@RequestParam String username) {
        return ResponseEntity.ok(itemService.getItems(username));
    }

    @PutMapping
    @Operation(summary = "아이템 수정", description = "아이템의 수량을 수정합니다.")
    public ResponseEntity<?> updateItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.updateItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 수정 완료") : ResponseEntity.badRequest().body("❌ 수정 실패");
    }

    @DeleteMapping
    @Operation(summary = "아이템 삭제", description = "아이템을 인벤토리에서 제거합니다.")
    public ResponseEntity<?> deleteItem(
            @RequestParam String username,
            @RequestParam String itemCode
    ) {
        boolean success = itemService.deleteItem(username, itemCode);
        return success ? ResponseEntity.ok("✅ 아이템 삭제 완료") : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
