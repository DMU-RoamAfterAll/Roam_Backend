package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {
    // PK가 userId라서 별도 메서드 없어도 가능
}
