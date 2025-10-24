package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.PlayerSave;
import com.cnwv.game_server.Entity.PlayerVisitedSection;
import com.cnwv.game_server.dto.SaveGameRequest;
import com.cnwv.game_server.dto.SaveGameResponse;
import com.cnwv.game_server.repository.PlayerSaveRepository;
import com.cnwv.game_server.repository.PlayerVisitedSectionRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveGameService {

    private final PlayerSaveRepository saveRepo;
    private final PlayerVisitedSectionRepository visitedRepo;
    private final UserRepository userRepository;

    /** username → userId(Long) 조회 (없으면 404) */
    private Long getUserIdOr404(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    /** 조회(없으면 404) */
    @WithUserShard(userIdParam = "username")
    @Transactional(readOnly = true)
    public SaveGameResponse get(String username) {
        Long userId = getUserIdOr404(username);

        PlayerSave save = saveRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "save not found"));

        var visitedIds = visitedRepo.findByIdUserId(userId).stream()
                .map(v -> v.getId().getSectionId())
                .toList();

        return toResponse(save, visitedIds);
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public SaveGameResponse create(String username, SaveGameRequest req) {
        Long userId = getUserIdOr404(username);
        if (saveRepo.existsById(userId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "save already exists");

        PlayerSave s = new PlayerSave();
        s.setUserId(userId);

        // 기본 필드 세팅
        s.setPlayerName(nvl(req.getPlayerName(), "Player"));
        s.setOriginSeed(nvl(req.getOriginSeed(), 0));
        if (req.getPlayerPos() != null) {
            s.setPosX(nvl(req.getPlayerPos().getX(), 0d));
            s.setPosY(nvl(req.getPlayerPos().getY(), 0d));
            s.setPosZ(nvl(req.getPlayerPos().getZ(), 0d));
        } else {
            s.setPosX(0); s.setPosY(0); s.setPosZ(0);
        }
        s.setCurrentSectionId(nvl(req.getCurrentSectionId(), ""));
        s.setPreSectionId(nvl(req.getPreSectionId(), ""));
        s.setTutorialClear(nvl(req.getTutorialClear(), true));

        // ✅ clearedSectionIds 반영
        if (req.getClearedSectionIds() != null) {
            s.setClearedSectionIds(req.getClearedSectionIds().stream()
                    .filter(str -> str != null && !str.isBlank()).toList());
        } else {
            s.setClearedSectionIds(List.of());
        }

        s = saveRepo.save(s);

        // 방문 섹션 저장(기존 로직 유지)
        List<String> list = req.getVisitedSectionIds();
        if (list != null && !list.isEmpty()) {
            List<PlayerVisitedSection> bulk = new ArrayList<>(list.size());
            for (String sec : list) {
                if (sec == null || sec.isBlank()) continue;
                bulk.add(new PlayerVisitedSection(new PlayerVisitedSection.Id(userId, sec), null));
            }
            if (!bulk.isEmpty()) {
                try { visitedRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
            }
        }

        var visitedIds = visitedRepo.findByIdUserId(userId).stream()
                .map(v -> v.getId().getSectionId()).toList();
        return toResponse(s, visitedIds);
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public SaveGameResponse upsert(String username, SaveGameRequest req) {
        Long userId = getUserIdOr404(username);

        boolean existed = saveRepo.existsById(userId);
        PlayerSave save = saveRepo.findById(userId).orElseGet(() -> {
            PlayerSave s = new PlayerSave();
            s.setUserId(userId);
            return s;
        });

        if (existed && req.getVersion() != null && req.getVersion() != save.getVersion()) {
            throw new OptimisticLockException("Version mismatch");
        }

        // 기본 필드 upsert
        save.setPlayerName(nvl(req.getPlayerName(), save.getPlayerName() == null ? "Player" : save.getPlayerName()));
        save.setOriginSeed(nvl(req.getOriginSeed(), save.getOriginSeed()));
        if (req.getPlayerPos() != null) {
            save.setPosX(nvl(req.getPlayerPos().getX(), save.getPosX()));
            save.setPosY(nvl(req.getPlayerPos().getY(), save.getPosY()));
            save.setPosZ(nvl(req.getPlayerPos().getZ(), save.getPosZ()));
        }
        save.setCurrentSectionId(nvl(req.getCurrentSectionId(), save.getCurrentSectionId() == null ? "" : save.getCurrentSectionId()));
        save.setPreSectionId(nvl(req.getPreSectionId(), save.getPreSectionId() == null ? "" : save.getPreSectionId()));
        save.setTutorialClear(nvl(req.getTutorialClear(), save.isTutorialClear()));

        // ✅ clearedSectionIds 전체 교체(요청이 온 경우에만)
        if (req.getClearedSectionIds() != null) {
            var normalized = req.getClearedSectionIds().stream()
                    .filter(str -> str != null && !str.isBlank()).toList();
            save.setClearedSectionIds(normalized);
        }

        save = saveRepo.save(save);

        // 방문 섹션 전체 교체(요청이 온 경우에만)
        if (req.getVisitedSectionIds() != null) {
            visitedRepo.deleteByIdUserId(userId);
            if (!req.getVisitedSectionIds().isEmpty()) {
                List<PlayerVisitedSection> bulk = new ArrayList<>(req.getVisitedSectionIds().size());
                for (String sec : req.getVisitedSectionIds()) {
                    if (sec == null || sec.isBlank()) continue;
                    bulk.add(new PlayerVisitedSection(new PlayerVisitedSection.Id(userId, sec), null));
                }
                if (!bulk.isEmpty()) {
                    try { visitedRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
                }
            }
        }

        var visitedIds = visitedRepo.findByIdUserId(userId).stream()
                .map(v -> v.getId().getSectionId()).toList();
        return toResponse(save, visitedIds);
    }

    private SaveGameResponse toResponse(PlayerSave s, List<String> visited) {
        return SaveGameResponse.builder()
                .playerName(s.getPlayerName())
                .originSeed(s.getOriginSeed())
                .playerPos(SaveGameResponse.PlayerPos.builder()
                        .x(s.getPosX()).y(s.getPosY()).z(s.getPosZ()).build())
                .currentSectionId(s.getCurrentSectionId())
                .preSectionId(s.getPreSectionId())
                .tutorialClear(s.isTutorialClear())
                .visitedSectionIds(visited == null ? List.of() : visited)
                /** ✅ 응답에도 그대로 포함 */
                .clearedSectionIds(s.getClearedSectionIds() == null ? List.of() : s.getClearedSectionIds())
                .version(s.getVersion())
                .build();
    }

    private static <T> T nvl(T v, T def) { return v != null ? v : def; }
    private static int nvl(Integer v, int def) { return v != null ? v : def; }
    private static double nvl(Double v, double def) { return v != null ? v : def; }
    private static boolean nvl(Boolean v, boolean def) { return v != null ? v : def; }
}