package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Schema(name = "PlayerStatsUpdateRequest", description = "스탯 절대값 업데이트 요청(부분 업데이트 허용)")
public class PlayerStatsUpdateRequest {

    @Schema(description = "체력(절대값, null이면 미변경)", example = "120", minimum = "0")
    private Integer hp;

    @Schema(description = "공격력(절대값, null이면 미변경)", example = "15", minimum = "0")
    private Integer atk;

    @Schema(description = "민첩(절대값, null이면 미변경)", example = "12", minimum = "0")
    private Integer spd;

    @Schema(description = "명중률 % (절대값, 0~100, null이면 미변경)", example = "75", minimum = "0", maximum = "100")
    private Integer hitRate;

    @Schema(description = "회피율 % (절대값, 0~100, null이면 미변경)", example = "20", minimum = "0", maximum = "100")
    private Integer evasionRate;

    @Schema(description = "치명타 확률 % (절대값, 0~100, null이면 미변경)", example = "5", minimum = "0", maximum = "100")
    private Integer counterRate;
}
