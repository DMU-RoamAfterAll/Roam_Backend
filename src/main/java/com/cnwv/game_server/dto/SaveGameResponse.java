package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SaveGameResponse {

    private String playerName;
    private Integer originSeed;

    @Getter @Builder
    public static class PlayerPos {
        private Double x;
        private Double y;
        private Double z;
    }
    private PlayerPos playerPos;

    private String currentSectionId;
    private String preSectionId;
    private Boolean tutorialClear;
    private List<String> visitedSectionIds;

    /** 서버측 낙관적 락 버전 */
    private Long version;
}
