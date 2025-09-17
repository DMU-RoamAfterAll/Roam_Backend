package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.Entity.InventoryWeaponId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryWeaponRepository extends JpaRepository<InventoryWeapon, InventoryWeaponId> {

    // EmbeddedId 경로(id.inventoryId)를 명시적으로 지정
    @Query("SELECT w FROM InventoryWeapon w WHERE w.id.inventoryId = :inventoryId")
    List<InventoryWeapon> findByInventoryId(@Param("inventoryId") Long inventoryId);
}
