package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.InventoryItem;
import com.cnwv.game_server.Entity.InventoryItemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

    // EmbeddedId 경로(id.inventoryId)를 명시적으로 지정
    @Query("SELECT i FROM InventoryItem i WHERE i.id.inventoryId = :inventoryId")
    List<InventoryItem> findByInventoryId(@Param("inventoryId") Long inventoryId);
}
