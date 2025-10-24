package com.cnwv.game_server.Entity;

import com.cnwv.game_server.jpa.StringListJsonConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "player_saves")
@Getter @Setter
@NoArgsConstructor
public class PlayerSave {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "player_name", nullable = false, length = 100)
    private String playerName;

    @Column(name = "origin_seed", nullable = false)
    private int originSeed;

    @Column(name = "pos_x", nullable = false)
    private double posX;

    @Column(name = "pos_y", nullable = false)
    private double posY;

    @Column(name = "pos_z", nullable = false)
    private double posZ;

    @Column(name = "current_section_id", nullable = false, length = 100)
    private String currentSectionId = "";

    @Column(name = "pre_section_id", nullable = false, length = 100)
    private String preSectionId = "";

    @Column(name = "tutorial_clear", nullable = false)
    private boolean tutorialClear = false;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "cleared_section_ids", columnDefinition = "TEXT")
    private List<String> clearedSectionIds = List.of();

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
