package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.PlayerStats;
import com.cnwv.game_server.dto.PlayerStatsDeltaRequest;
import com.cnwv.game_server.dto.PlayerStatsResponse;
import com.cnwv.game_server.dto.PlayerStatsUpdateRequest;
import com.cnwv.game_server.repository.PlayerStatsRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlayerStatsService {

    private final PlayerStatsRepository repo;

    /** 기본값 생성 또는 조회 */
    @Transactional
    public PlayerStatsResponse getOrCreate(long userId) {
        PlayerStats stats = repo.findById(userId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setUserId(userId);
            return repo.save(s);
        });
        return toDto(stats);
    }

    /** 전체/부분 필드 절대값 업데이트 (음수 방지 clamp) */
    @Transactional
    public PlayerStatsResponse update(long userId, PlayerStatsUpdateRequest req) {
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
            // 필요 시 재시도 전략(간단히 다시 조회 후 실패 응답 등) 추가
            throw e;
        }
    }

    /** 증감치(+, -) 적용 */
    @Transactional
    public PlayerStatsResponse applyDelta(long userId, PlayerStatsDeltaRequest req) {
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

    /** 퍼센트 필드는 0~100으로 제한 (원하면 상한 변경하세요) */
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
