package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerVisitedSection;
import com.cnwv.game_server.Entity.PlayerVisitedSection.Id;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlayerVisitedSectionRepository extends JpaRepository<PlayerVisitedSection, Id> {
    List<PlayerVisitedSection> findByIdUserId(Long userId);

    @Modifying
    @Query("delete from PlayerVisitedSection v where v.id.userId = :userId")
    void deleteByIdUserId(@Param("userId") Long userId);
}
