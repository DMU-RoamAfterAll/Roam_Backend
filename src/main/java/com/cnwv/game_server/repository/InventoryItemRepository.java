package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.InventoryItem;
import com.cnwv.game_server.Entity.InventoryItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {
    List<InventoryItem> findByInventoryId(Long inventoryId);
}
