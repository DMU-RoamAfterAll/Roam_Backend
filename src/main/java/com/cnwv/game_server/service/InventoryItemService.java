package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.InventoryItemRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean insertItem(String username, String itemCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();
        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(invId);
        id.setItemCode(itemCode);

        InventoryItem existing = itemRepository.findById(id).orElse(null);
        if (existing == null) {
            InventoryItem item = new InventoryItem();
            item.setId(id);
            item.setInventory(user.getInventory());
            item.setAmount(amount);
            itemRepository.save(item);
        } else {
            existing.setAmount(existing.getAmount() + amount); // ✅ 중복 시 수량 증가
            itemRepository.save(existing);
        }
        return true;
    }

    public List<InventoryItem> getItems(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return itemRepository.findByInventoryId(user.getInventory().getId());
    }

    @Transactional
    public boolean updateItem(String username, String itemCode, int amount) {
        if (amount < 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(user.getInventory().getId());
        id.setItemCode(itemCode);

        InventoryItem item = itemRepository.findById(id).orElse(null);
        if (item == null) return false;

        item.setAmount(amount); // 절대값 수정
        itemRepository.save(item);
        return true;
    }

    @Transactional
    public boolean deleteItem(String username, String itemCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(user.getInventory().getId());
        id.setItemCode(itemCode);

        InventoryItem item = itemRepository.findById(id).orElse(null);
        if (item == null) {
            // 수량이 없으면 요청 무시
            return true;
        }

        int remain = item.getAmount() - amount;
        if (remain > 0) {
            item.setAmount(remain);
            itemRepository.save(item);
        } else {
            // 0 이하가 되면 행 삭제
            itemRepository.deleteById(id);
        }
        return true;
    }
}
