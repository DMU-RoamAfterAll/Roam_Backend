package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshRequest {
    @Schema(description = "사용자의 Refresh Token", example = "your_refresh_token_here")
    private String refreshToken;
}
