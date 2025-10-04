package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Schema(name = "PlayerStatsResponse", description = "플레이어 스탯 조회 응답")
public class PlayerStatsResponse {

    @Schema(description = "체력", example = "100", minimum = "0")
    private int hp;

    @Schema(description = "공격력", example = "10", minimum = "0")
    private int atk;

    @Schema(description = "민첩", example = "10", minimum = "0")
    private int spd;

    @Schema(description = "명중률(%)", example = "70", minimum = "0", maximum = "100")
    private int hitRate;

    @Schema(description = "회피율(%)", example = "12", minimum = "0", maximum = "100")
    private int evasionRate;

    @Schema(description = "치명타 확률(%)", example = "3", minimum = "0", maximum = "100")
    private int counterRate;
}
