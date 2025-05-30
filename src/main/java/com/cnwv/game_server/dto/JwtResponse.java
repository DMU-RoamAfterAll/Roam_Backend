package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class JwtResponse {
    @Schema(description = "발급되는 토큰")
    private String accessToken;
    @Schema(description = "재발급되는 토큰")
    private String refreshToken;

    public JwtResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}