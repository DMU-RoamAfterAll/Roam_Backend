package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class SaveGameRequest {

    @Schema(example = "Potato")
    private String playerName;

    @Schema(example = "-1803581142")
    private Integer originSeed;

    @Getter @Setter
    public static class PlayerPos {
        @Schema(example = "0.0") private Double x;
        @Schema(example = "4.0") private Double y;
        @Schema(example = "0.0") private Double z;
    }
    private PlayerPos playerPos;

    @Schema(example = "")
    private String currentSectionId;

    @Schema(example = "")
    private String preSectionId;

    @Schema(example = "true")
    private Boolean tutorialClear;

    @Schema(example = "[\"sec-1\",\"sec-2\"]")
    private List<String> visitedSectionIds;

    // 선택: 낙관적 락 버전 검증용(없으면 무시)
    private Long version;
}
