package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.FlagData;
import com.cnwv.game_server.Entity.FlagDataId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlagDataRepository extends JpaRepository<FlagData, FlagDataId> {

    List<FlagData> findByIdUserId(Long userId);
}
