package com.cnwv.game_server.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "player_visited_sections")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlayerVisitedSection {

    @EmbeddedId
    private Id id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "user_id")    private Long userId;
        @Column(name = "section_id") private String sectionId;
    }
}
