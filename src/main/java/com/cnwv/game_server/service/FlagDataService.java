package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.FlagData;
import com.cnwv.game_server.Entity.FlagDataId;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.dto.FlagDataResponse;
import com.cnwv.game_server.repository.FlagDataRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@WithUserShard(userIdParam = "username") // ✅ username 기반으로 샤드 라우팅
public class FlagDataService {

    private final FlagDataRepository flagRepository;
    private final UserRepository userRepository;

    /** 없으면 INSERT, 있으면 UPDATE. 동일 값이면 NO-OP 처리. */
    @Transactional
    public boolean setFlag(String username, String flagCode, boolean flagState) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        FlagDataId id = new FlagDataId(user.getId(), flagCode);

        var existingOpt = flagRepository.findById(id);
        if (existingOpt.isEmpty()) {
            FlagData flag = new FlagData();
            flag.setId(id);
            flag.setUser(user);
            flag.setFlagState(flagState);
            flagRepository.save(flag); // INSERT
            return true;
        } else {
            FlagData existing = existingOpt.get();
            if (existing.isFlagState() == flagState) {
                return true; // 동일 값이면 NO-OP
            }
            existing.setFlagState(flagState);
            flagRepository.save(existing); // UPDATE
            return true;
        }
    }

    public List<FlagDataResponse> getFlags(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();

        return flagRepository.findByIdUserId(user.getId()).stream()
                .map(f -> new FlagDataResponse(
                        f.getId().getFlagCode(), // flagCode
                        f.isFlagState()          // flagState
                ))
                .toList();
    }

    public Boolean getFlagState(String username, String flagCode) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        FlagDataId id = new FlagDataId(user.getId(), flagCode);
        return flagRepository.findById(id)
                .map(FlagData::isFlagState)
                .orElse(null);
    }

    @Transactional
    public boolean deleteFlag(String username, String flagCode) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        FlagDataId id = new FlagDataId(user.getId(), flagCode);
        if (flagRepository.existsById(id)) {
            flagRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
