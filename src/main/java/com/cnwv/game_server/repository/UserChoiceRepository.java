package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.UserChoice;
import com.cnwv.game_server.Entity.UserChoiceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserChoiceRepository extends JpaRepository<UserChoice, UserChoiceId> {
    List<UserChoice> findByUserId(Long userId);
}