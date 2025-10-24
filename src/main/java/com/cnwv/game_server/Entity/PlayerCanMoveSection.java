package com.cnwv.game_server.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_can_move_sections")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCanMoveSection {

    @EmbeddedId
    private Id id;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class Id implements Serializable {
        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "section_id", nullable = false, length = 100)
        private String sectionId;
    }
}
