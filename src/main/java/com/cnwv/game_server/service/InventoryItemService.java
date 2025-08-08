package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.InventoryItemRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final UserRepository userRepository;

    public boolean insertItem(String username, String itemCode, int amount) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Inventory inventory = user.getInventory();
        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(inventory.getId());
        id.setItemCode(itemCode);

        InventoryItem item = new InventoryItem();
        item.setId(id);
        item.setInventory(inventory);
        item.setAmount(amount);

        itemRepository.save(item);
        return true;
    }

    public List<InventoryItem> getItems(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return itemRepository.findByInventoryId(user.getInventory().getId());
    }

    public boolean updateItem(String username, String itemCode, int amount) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(user.getInventory().getId());
        id.setItemCode(itemCode);

        InventoryItem item = itemRepository.findById(id).orElse(null);
        if (item == null) return false;

        item.setAmount(amount);
        itemRepository.save(item);
        return true;
    }

    public boolean deleteItem(String username, String itemCode) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryItemId id = new InventoryItemId();
        id.setInventoryId(user.getInventory().getId());
        id.setItemCode(itemCode);

        if (!itemRepository.existsById(id)) return false;
        itemRepository.deleteById(id);
        return true;
    }
}