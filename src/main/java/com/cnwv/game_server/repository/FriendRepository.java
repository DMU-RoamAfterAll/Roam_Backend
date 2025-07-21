package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.Friend;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    @Query("SELECT COUNT(f) > 0 FROM Friend f " +
            "WHERE f.user.nickname = :nickname1 " +
            "AND f.friend.nickname = :nickname2 " +
            "AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("nickname1") String nickname1,
                       @Param("nickname2") String nickname2);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
            "(f.user.id = :idA AND f.friend.id = :idB) OR " +
            "(f.user.id = :idB AND f.friend.id = :idA)")
    boolean isFriendById(Long idA, Long idB);

}

