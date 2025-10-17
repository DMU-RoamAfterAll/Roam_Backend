package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.PlayerStats;
import com.cnwv.game_server.dto.PlayerStatsDeltaRequest;
import com.cnwv.game_server.dto.PlayerStatsResponse;
import com.cnwv.game_server.dto.PlayerStatsUpdateRequest;
import com.cnwv.game_server.repository.PlayerStatsRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerStatsRepository repo;
    private final UserRepository userRepository;

    private long getUserIdOr404(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    /** 기본값 생성 또는 조회 */
    @WithUserShard(userIdParam = "username")
    @Transactional
    public PlayerStatsResponse getOrCreate(String username) {
        long userId = getUserIdOr404(username);
        PlayerStats stats = repo.findById(userId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setUserId(userId);
            // 기본값은 엔티티 필드 디폴트(100/10/...) 그대로 사용
            return repo.save(s);
        });
        return toDto(stats);
    }

    /** 전체/부분 필드 절대값 업데이트 (음수 방지 clamp) */
    @WithUserShard(userIdParam = "username")
    @Transactional
    public PlayerStatsResponse update(String username, PlayerStatsUpdateRequest req) {
        long userId = getUserIdOr404(username);
        PlayerStats stats = repo.findById(userId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setUserId(userId);
            return s;
        });

        if (req.getHp() != null) stats.setHp(clampNonNegative(req.getHp()));
        if (req.getAtk() != null) stats.setAtk(clampNonNegative(req.getAtk()));
        if (req.getSpd() != null) stats.setSpd(clampNonNegative(req.getSpd()));
        if (req.getHitRate() != null) stats.setHitRate(clampPercent(req.getHitRate()));
        if (req.getEvasionRate() != null) stats.setEvasionRate(clampPercent(req.getEvasionRate()));
        if (req.getCounterRate() != null) stats.setCounterRate(clampPercent(req.getCounterRate()));

        try {
            return toDto(repo.save(stats));
        } catch (OptimisticLockException e) {
            throw e;
        }
    }

    /** 증감치(+, -) 적용 */
    @WithUserShard(userIdParam = "username")
    @Transactional
    public PlayerStatsResponse applyDelta(String username, PlayerStatsDeltaRequest req) {
        long userId = getUserIdOr404(username);
        PlayerStats stats = repo.findById(userId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setUserId(userId);
            return s;
        });

        if (req.getHpDelta() != null) stats.setHp(clampNonNegative(stats.getHp() + req.getHpDelta()));
        if (req.getAtkDelta() != null) stats.setAtk(clampNonNegative(stats.getAtk() + req.getAtkDelta()));
        if (req.getSpdDelta() != null) stats.setSpd(clampNonNegative(stats.getSpd() + req.getSpdDelta()));
        if (req.getHitRateDelta() != null) stats.setHitRate(clampPercent(stats.getHitRate() + req.getHitRateDelta()));
        if (req.getEvasionRateDelta() != null) stats.setEvasionRate(clampPercent(stats.getEvasionRate() + req.getEvasionRateDelta()));
        if (req.getCounterRateDelta() != null) stats.setCounterRate(clampPercent(stats.getCounterRate() + req.getCounterRateDelta()));

        try {
            return toDto(repo.save(stats));
        } catch (OptimisticLockException e) {
            throw e;
        }
    }

    private int clampNonNegative(int v) { return Math.max(0, v); }
    private int clampPercent(int v) { return Math.max(0, Math.min(100, v)); }

    private PlayerStatsResponse toDto(PlayerStats s) {
        PlayerStatsResponse dto = new PlayerStatsResponse();
        dto.setHp(s.getHp());
        dto.setAtk(s.getAtk());
        dto.setSpd(s.getSpd());
        dto.setHitRate(s.getHitRate());
        dto.setEvasionRate(s.getEvasionRate());
        dto.setCounterRate(s.getCounterRate());
        return dto;
    }
}
