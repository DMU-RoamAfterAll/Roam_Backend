package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.PlayerSkill;
import com.cnwv.game_server.Entity.PlayerSkillId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PlayerSkillRepository extends JpaRepository<PlayerSkill, PlayerSkillId> {

    List<PlayerSkill> findByIdUserId(Long userId);

    @Transactional
    @Modifying
    @Query(value = """
        INSERT INTO player_skills (user_id, skill_code, skill_level)
        VALUES (:userId, :skillCode, :delta)
        ON DUPLICATE KEY UPDATE skill_level = GREATEST(0, skill_level + VALUES(skill_level))
        """, nativeQuery = true)
    void upsertIncrease(@Param("userId") Long userId,
                        @Param("skillCode") String skillCode,
                        @Param("delta") int delta);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE player_skills
           SET skill_level = :level
         WHERE user_id = :userId AND skill_code = :skillCode
        """, nativeQuery = true)
    int setLevel(@Param("userId") Long userId,
                 @Param("skillCode") String skillCode,
                 @Param("level") int level);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE player_skills
           SET skill_level = skill_level - :delta
         WHERE user_id = :userId AND skill_code = :skillCode
        """, nativeQuery = true)
    int decrement(@Param("userId") Long userId,
                  @Param("skillCode") String skillCode,
                  @Param("delta") int delta);

    @Transactional
    @Modifying
    @Query(value = """
        DELETE FROM player_skills
         WHERE user_id = :userId AND skill_code = :skillCode AND skill_level <= 0
        """, nativeQuery = true)
    int deleteIfZeroOrLess(@Param("userId") Long userId,
                           @Param("skillCode") String skillCode);
}
