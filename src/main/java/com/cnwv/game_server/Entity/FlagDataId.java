package com.cnwv.game_server.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlagDataId implements Serializable {

    @Column(name = "user_id")     // ★ snake_case 매핑
    private Long userId;

    @Column(name = "flag_code")   // ★ snake_case 매핑
    private String flagCode;
}
