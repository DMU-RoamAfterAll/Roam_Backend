package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerVisitedSection;
import com.cnwv.game_server.Entity.PlayerVisitedSection.Id;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerVisitedSectionRepository extends JpaRepository<PlayerVisitedSection, Id> {
    List<PlayerVisitedSection> findByIdUserId(Long userId);
    void deleteByIdUserId(Long userId);
}
