package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    @Schema(description = "사용자 ID", example = "cnwvid")
    private String username;
    @Schema(description = "Password", example = "cnwvpw")
    private String password;
}