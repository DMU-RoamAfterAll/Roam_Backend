package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryWeaponDto {

    @Schema(description = "인벤토리 ID")
    private Long inventoryId;

    @Schema(description = "무기 코드")
    private String weaponCode;

    @Schema(description = "수량")
    private int amount;
}
