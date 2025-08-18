package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FlagDataResponse {

    @Schema(description = "플래그 코드")
    private String flagCode;

    @Schema(description = "플래그 상태")
    private boolean flagState;

}
