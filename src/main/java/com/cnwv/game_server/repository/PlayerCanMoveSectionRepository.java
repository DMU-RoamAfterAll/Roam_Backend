package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerCanMoveSection;
import com.cnwv.game_server.Entity.PlayerCanMoveSection.Id;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerCanMoveSectionRepository extends JpaRepository<PlayerCanMoveSection, Id> {
    List<PlayerCanMoveSection> findByIdUserId(Long userId);
    void deleteByIdUserId(Long userId);
}
