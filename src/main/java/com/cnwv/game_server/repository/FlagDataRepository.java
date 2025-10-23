package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.FlagData;
import com.cnwv.game_server.Entity.FlagDataId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlagDataRepository extends JpaRepository<FlagData, FlagDataId> {

    List<FlagData> findByIdUserId(Long userId);

    @Modifying
    @Query("delete from FlagData f where f.id.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
