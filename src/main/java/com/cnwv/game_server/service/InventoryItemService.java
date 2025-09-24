package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.InventoryItemRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final UserRepository userRepository;

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean insertItem(String username, String itemCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();
        // 원자적 업서트(증가)
        itemRepository.upsertIncrease(invId, itemCode, amount);
        return true;
    }

    @WithUserShard(userIdParam = "username")
    public List<InventoryItem> getItems(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return itemRepository.findByInventoryId(user.getInventory().getId());
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean updateItem(String username, String itemCode, int amount) {
        if (amount < 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();
        int updated = itemRepository.setAmount(invId, itemCode, amount);
        return updated > 0;
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean deleteItem(String username, String itemCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();

        // 1) 차감 (음수 허용 → 이후 0 이하 정리)
        itemRepository.decrement(invId, itemCode, amount);
        // 2) 0 이하이면 삭제
        itemRepository.deleteIfZeroOrLess(invId, itemCode);

        return true;
    }
}
