package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.repository.*;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 진행 데이터 전부 삭제만 수행하는 서비스.
 * 기본값 삽입/초기 지급 등은 별도의 API로 처리.
 */
@Service
@RequiredArgsConstructor
public class GameResetService {

    private final UserRepository userRepository;
    private final PlayerSaveRepository playerSaveRepository;
    private final PlayerVisitedSectionRepository visitedSectionRepository;
    private final PlayerStatsRepository playerStatsRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryItemRepository itemRepository;
    private final InventoryWeaponRepository weaponRepository;
    private final FlagDataRepository flagDataRepository;
    private final PlayerSkillRepository playerSkillRepository;

    @WithUserShard(userIdParam = "username")
    @Transactional
    public void hardReset(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return; // 멱등성: 없으면 그냥 끝

        Long userId = user.getId();

        // 1) 방문 섹션
        visitedSectionRepository.deleteByIdUserId(userId);

        // 2) 세이브
        playerSaveRepository.deleteById(userId);

        // 3) 스탯
        playerStatsRepository.deleteById(userId);

        // 4) 플래그
        flagDataRepository.deleteByUserId(userId);

        // 5) 스킬
        playerSkillRepository.deleteByUserId(userId);

        // 6) 인벤토리 하위(아이템/무기) → 인벤토리
        if (user.getInventory() != null) {
            Long invId = user.getInventory().getId();
            weaponRepository.deleteByInventoryId(invId);
            itemRepository.deleteByInventoryId(invId);
            inventoryRepository.deleteById(invId);
        }
    }
}
