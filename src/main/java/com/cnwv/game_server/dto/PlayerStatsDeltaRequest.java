package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor
@Schema(name = "PlayerStatsDeltaRequest", description = "스탯 증감치(+/-) 업데이트 요청")
public class PlayerStatsDeltaRequest {

    @Schema(description = "체력 증감치(null이면 미변경)", example = "-10")
    private Integer hpDelta;

    @Schema(description = "공격력 증감치(null이면 미변경)", example = "2")
    private Integer atkDelta;

    @Schema(description = "민첩 증감치(null이면 미변경)", example = "1")
    private Integer spdDelta;

    @Schema(description = "명중률 증감치 % (0~100 범위로 클램핑, null이면 미변경)", example = "3")
    private Integer hitRateDelta;

    @Schema(description = "회피율 증감치 % (0~100 범위로 클램핑, null이면 미변경)", example = "5")
    private Integer evasionRateDelta;

    @Schema(description = "치명타 확률 증감치 % (0~100 범위로 클램핑, null이면 미변경)", example = "1")
    private Integer counterRateDelta;
}
