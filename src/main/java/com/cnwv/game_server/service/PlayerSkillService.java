package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.PlayerSkill;
import com.cnwv.game_server.Entity.PlayerSkillId;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.dto.SkillDataResponse;
import com.cnwv.game_server.repository.PlayerSkillRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlayerSkillService {

    private final PlayerSkillRepository skillRepo;
    private final UserRepository userRepo;

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean addOrIncrease(String username, String skillCode, int delta) {
        if (delta <= 0) return false;
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return false;
        skillRepo.upsertIncrease(user.getId(), skillCode, delta);
        return true;
    }

    @WithUserShard(userIdParam = "username")
    public List<SkillDataResponse> getSkills(String username) {
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return List.of();
        return skillRepo.findByIdUserId(user.getId()).stream()
                .map(s -> new SkillDataResponse(s.getId().getSkillCode(), s.getSkillLevel()))
                .toList();
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean setLevel(String username, String skillCode, int level) {
        if (level < 0) return false;
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return false;
        int updated = skillRepo.setLevel(user.getId(), skillCode, level);
        return updated > 0;
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean decreaseOrDelete(String username, String skillCode, int delta) {
        if (delta <= 0) return false;
        User user = userRepo.findByUsername(username).orElse(null);
        if (user == null) return false;

        skillRepo.decrement(user.getId(), skillCode, delta);
        skillRepo.deleteIfZeroOrLess(user.getId(), skillCode);
        return true;
    }
}
