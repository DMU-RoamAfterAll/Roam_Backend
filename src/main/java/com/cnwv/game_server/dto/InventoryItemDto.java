package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryItemDto {

    @Schema(description = "인벤토리 ID")
    private Long inventoryId;

    @Schema(description = "아이템 코드")
    private String itemCode;

    @Schema(description = "수량")
    private int amount;
}
