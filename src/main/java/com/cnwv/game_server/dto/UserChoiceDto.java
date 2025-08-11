package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserChoiceDto {

    @Schema(description = "선택지 코드")
    private String choiceCode;

    @Schema(description = "선택지 상태")
    private boolean condition;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;
}
