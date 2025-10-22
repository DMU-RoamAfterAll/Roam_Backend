package com.cnwv.game_server.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PlayerSkillId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "skill_code", nullable = false, length = 100)
    private String skillCode;
}
