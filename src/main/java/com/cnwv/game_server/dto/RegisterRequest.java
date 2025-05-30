package com.cnwv.game_server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @Schema(description = "사용자 ID", example = "cnwvid")
    private String username;

    @Schema(description = "비밀번호", example = "cnwvpw")
    private String password;

    @Schema(description = "닉네임", example = "cnwvnick")
    private String nickname;

    @Schema(description = "생년월일", example = "2000-01-01")
    private String birthDate; // 파싱 필요

    @Schema(description = "이메일", example = "cnwv@cnwv.com")
    private String email;
}
