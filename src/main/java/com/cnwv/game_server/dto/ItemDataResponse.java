package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemDataResponse {

    @Schema(description = "아이템 코드명")
    private String itemCode;

    @Schema(description = "수량")
    private int amount;

}
