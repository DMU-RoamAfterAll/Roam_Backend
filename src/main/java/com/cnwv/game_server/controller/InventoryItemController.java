package com.cnwv.game_server.controller;

import com.cnwv.game_server.Entity.InventoryItem;
import com.cnwv.game_server.dto.InventoryItemDto;
import com.cnwv.game_server.service.InventoryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory/items")
@Tag(name = "Inventory Item API", description = "아이템 삽입/조회/수정/삭제 API (username 사용)")
@SecurityRequirement(name = "bearerAuth")
public class InventoryItemController {

    private final InventoryItemService itemService;

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
    @Operation(summary = "아이템 목록 조회 (DTO 응답)", description = "순환참조를 피하기 위해 DTO로 응답합니다.")
    public ResponseEntity<List<InventoryItemDto>> getItems(@RequestParam String username) {
        List<InventoryItem> list = itemService.getItems(username);
        List<InventoryItemDto> dtos = list.stream()
                .map(i -> new InventoryItemDto(
                        i.getId().getInventoryId(),
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
}
