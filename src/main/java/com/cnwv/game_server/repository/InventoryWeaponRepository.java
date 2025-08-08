package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.Entity.InventoryWeaponId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryWeaponRepository extends JpaRepository<InventoryWeapon, InventoryWeaponId> {
    List<InventoryWeapon> findByInventoryId(Long inventoryId);
}