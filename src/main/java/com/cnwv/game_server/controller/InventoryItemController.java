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
    @Operation(summary = "아이템 삽입 (중복시 수량 증가)", description = "동일 키가 존재하면 amount 만큼 수량을 증가시킵니다.")
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
    @Operation(summary = "아이템 목록 조회", description = "사용자의 인벤토리에서 아이템을 모두 조회합니다.")
    public ResponseEntity<List<InventoryItem>> getItems(@RequestParam String username) {
        return ResponseEntity.ok(itemService.getItems(username));
    }

    @PutMapping
    @Operation(summary = "아이템 수량 설정", description = "아이템의 수량을 절대값으로 설정합니다.")
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
    @Operation(summary = "아이템 수량 차감/삭제", description = "amount 만큼 수량을 차감하며, 0 이하이면 행을 삭제합니다. 수량이 없으면 요청은 무시됩니다.")
    public ResponseEntity<?> deleteItem(
            @RequestParam String username,
            @RequestParam String itemCode,
            @RequestParam int amount
    ) {
        boolean success = itemService.deleteItem(username, itemCode, amount);
        return success ? ResponseEntity.ok("✅ 아이템 차감/삭제 처리 완료")
                : ResponseEntity.badRequest().body("❌ 삭제 실패");
    }
}
