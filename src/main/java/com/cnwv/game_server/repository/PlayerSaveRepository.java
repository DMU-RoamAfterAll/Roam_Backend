package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerSave;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSaveRepository extends JpaRepository<PlayerSave, Long> {

}
