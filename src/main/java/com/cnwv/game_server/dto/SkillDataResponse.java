package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillDataResponse {
    @Schema(description = "스킬 코드", example = "FIREBALL")
    private String skillCode;

    @Schema(description = "스킬 레벨", example = "3")
    private int skillLevel;
}
