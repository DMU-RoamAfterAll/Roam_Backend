package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.PlayerSave;
import com.cnwv.game_server.Entity.PlayerVisitedSection;
import com.cnwv.game_server.Entity.PlayerCanMoveSection;
import com.cnwv.game_server.dto.SaveGameRequest;
import com.cnwv.game_server.dto.SaveGameResponse;
import com.cnwv.game_server.repository.PlayerSaveRepository;
import com.cnwv.game_server.repository.PlayerVisitedSectionRepository;
import com.cnwv.game_server.repository.PlayerCanMoveSectionRepository; // 🆕
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
    private final PlayerCanMoveSectionRepository canMoveRepo; // 🆕
    private final UserRepository userRepository;

    private Long getUserIdOr404(String username) {
        return userRepository.findByUsername(username)
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

    @WithUserShard(userIdParam = "username")
    @Transactional(readOnly = true)
    public SaveGameResponse get(String username) {
        Long userId = getUserIdOr404(username);

        PlayerSave save = saveRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "save not found"));

        var visited = visitedRepo.findByIdUserId(userId);
        var visitedIds = visited.stream().map(v -> v.getId().getSectionId()).toList();

        var canMoves = canMoveRepo.findByIdUserId(userId); // 🆕
        var canMoveIds = canMoves.stream().map(c -> c.getId().getSectionId()).toList();

        return toResponse(save, visitedIds, canMoveIds);
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public SaveGameResponse create(String username, SaveGameRequest req) {
        Long userId = getUserIdOr404(username);
        if (saveRepo.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "save already exists");
        }

        PlayerSave s = new PlayerSave();
        s.setUserId(userId);
        s = saveRepo.save(s);

        // 방문 섹션
        saveVisitedList(userId, req.getVisitedSectionIds());

        // 이동 가능 섹션
        saveCanMoveList(userId, req.getCanMoveSectionIds());

        var visitedIds = visitedRepo.findByIdUserId(userId).stream().map(v -> v.getId().getSectionId()).toList();
        var canMoveIds = canMoveRepo.findByIdUserId(userId).stream().map(c -> c.getId().getSectionId()).toList();
        return toResponse(s, visitedIds, canMoveIds);
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

        save = saveRepo.save(save);

        // 방문 섹션 리스트가 요청에 있으면 전체 교체
        if (req.getVisitedSectionIds() != null) {
            visitedRepo.deleteByIdUserId(userId);
            saveVisitedList(userId, req.getVisitedSectionIds());
        }

        // canMove 리스트가 요청에 있으면 전체 교체
        if (req.getCanMoveSectionIds() != null) {
            canMoveRepo.deleteByIdUserId(userId);
            saveCanMoveList(userId, req.getCanMoveSectionIds());
        }

        var visitedIds = visitedRepo.findByIdUserId(userId).stream().map(v -> v.getId().getSectionId()).toList();
        var canMoveIds = canMoveRepo.findByIdUserId(userId).stream().map(c -> c.getId().getSectionId()).toList();
        return toResponse(save, visitedIds, canMoveIds);
    }

    private void saveVisitedList(Long userId, List<String> list) {
        if (list == null || list.isEmpty()) return;
        List<PlayerVisitedSection> bulk = new ArrayList<>(list.size());
        for (String sec : list) {
            if (sec == null || sec.isBlank()) continue;
            bulk.add(new PlayerVisitedSection(
                    new PlayerVisitedSection.Id(userId, sec), null
            ));
        }
        if (!bulk.isEmpty()) {
            try { visitedRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
        }
    }

    private void saveCanMoveList(Long userId, List<String> list) { // 🆕
        if (list == null || list.isEmpty()) return;
        List<PlayerCanMoveSection> bulk = new ArrayList<>(list.size());
        for (String sec : list) {
            if (sec == null || sec.isBlank()) continue;
            bulk.add(new PlayerCanMoveSection(
                    new PlayerCanMoveSection.Id(userId, sec), null
            ));
        }
        if (!bulk.isEmpty()) {
            try { canMoveRepo.saveAll(bulk); } catch (DataIntegrityViolationException ignore) {}
        }
    }

    private SaveGameResponse toResponse(PlayerSave s, List<String> visited, List<String> canMove) { // 🆕
        return SaveGameResponse.builder()
                .playerName(s.getPlayerName())
                .originSeed(s.getOriginSeed())
                .playerPos(SaveGameResponse.PlayerPos.builder()
                        .x(s.getPosX()).y(s.getPosY()).z(s.getPosZ()).build())
                .currentSectionId(s.getCurrentSectionId())
                .preSectionId(s.getPreSectionId())
                .tutorialClear(s.isTutorialClear())
                .visitedSectionIds(visited == null ? List.of() : visited)
                .canMoveSectionIds(canMove == null ? List.of() : canMove) // 🆕
                .version(s.getVersion())
                .build();
    }

    private static <T> T nvl(T v, T def) { return v != null ? v : def; }
    private static int nvl(Integer v, int def) { return v != null ? v : def; }
    private static double nvl(Double v, double def) { return v != null ? v : def; }
    private static boolean nvl(Boolean v, boolean def) { return v != null ? v : def; }
}
