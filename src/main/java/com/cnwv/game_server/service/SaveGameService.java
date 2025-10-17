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
import java.util.NoSuchElementException;

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

        var visited = visitedRepo.findByIdUserId(userId);
        var visitedIds = visited.stream().map(v -> v.getId().getSectionId()).toList();
        return toResponse(save, visitedIds);
    }

    /** 최초 생성(이미 있으면 409) */
    @WithUserShard(userIdParam = "username")
    @Transactional
    public SaveGameResponse create(String username, SaveGameRequest req) {
        Long userId = getUserIdOr404(username);

        if (saveRepo.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "save already exists");
        }

        PlayerSave s = new PlayerSave();
        s.setUserId(userId);

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

        s = saveRepo.save(s);

        // 방문 섹션 저장
        List<String> list = req.getVisitedSectionIds();
        if (list != null && !list.isEmpty()) {
            List<PlayerVisitedSection> bulk = new ArrayList<>(list.size());
            for (String sec : list) {
                if (sec == null || sec.isBlank()) continue;
                bulk.add(new PlayerVisitedSection(
                        new PlayerVisitedSection.Id(userId, sec),
                        null
                ));
            }
            if (!bulk.isEmpty()) {
                try { visitedRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
            }
        }

        var visited = visitedRepo.findByIdUserId(userId);
        var visitedIds = visited.stream().map(v -> v.getId().getSectionId()).toList();
        return toResponse(s, visitedIds);
    }

    /** 전체 저장(업서트: 없으면 생성) */
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

        // 기존 row가 있을 때만 버전 체크
        if (existed && req.getVersion() != null && req.getVersion() != save.getVersion()) {
            throw new OptimisticLockException("Version mismatch");
        }

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

        save = saveRepo.save(save);

        if (req.getVisitedSectionIds() != null) {
            visitedRepo.deleteByIdUserId(userId);
            if (!req.getVisitedSectionIds().isEmpty()) {
                List<PlayerVisitedSection> bulk = new ArrayList<>(req.getVisitedSectionIds().size());
                for (String sec : req.getVisitedSectionIds()) {
                    if (sec == null || sec.isBlank()) continue;
                    bulk.add(new PlayerVisitedSection(
                            new PlayerVisitedSection.Id(userId, sec),
                            null
                    ));
                }
                if (!bulk.isEmpty()) {
                    try { visitedRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
                }
            }
        }

        var visited = visitedRepo.findByIdUserId(userId);
        var visitedIds = visited.stream().map(v -> v.getId().getSectionId()).toList();
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
                .version(s.getVersion())
                .build();
    }

    private static <T> T nvl(T v, T def) { return v != null ? v : def; }
    private static int nvl(Integer v, int def) { return v != null ? v : def; }
    private static double nvl(Double v, double def) { return v != null ? v : def; }
    private static boolean nvl(Boolean v, boolean def) { return v != null ? v : def; }
}
