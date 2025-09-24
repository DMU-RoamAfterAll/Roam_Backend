package com.cnwv.game_server.repository;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.Entity.InventoryWeaponId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryWeaponRepository extends JpaRepository<InventoryWeapon, InventoryWeaponId> {

    // 기존 조회 (서비스에서 사용 중)
    List<InventoryWeapon> findByInventoryId(Long inventoryId);

    // ---- 원자적 증가 (업서트) ----
    @Modifying
    @Query(value =
            "INSERT INTO inventory_weapons (inventory_id, weapon_code, amount, created_at, version) " +
                    "VALUES (:invId, :code, :amt, NOW(), 0) " +
                    "ON DUPLICATE KEY UPDATE amount = amount + VALUES(amount), version = version + 1",
            nativeQuery = true)
    int upsertIncrease(@Param("invId") Long invId,
                       @Param("code") String weaponCode,
                       @Param("amt") int amount);

    // ---- 절대값 설정 ----
    @Modifying
    @Query(value =
            "UPDATE inventory_weapons SET amount = :amt, version = version + 1 " +
                    "WHERE inventory_id = :invId AND weapon_code = :code",
            nativeQuery = true)
    int setAmount(@Param("invId") Long invId,
                  @Param("code") String weaponCode,
                  @Param("amt") int amount);

    // ---- 감소(음수까지 허용) ----
    @Modifying
    @Query(value =
            "UPDATE inventory_weapons SET amount = amount - :delta, version = version + 1 " +
                    "WHERE inventory_id = :invId AND weapon_code = :code",
            nativeQuery = true)
    int decrement(@Param("invId") Long invId,
                  @Param("code") String weaponCode,
                  @Param("delta") int delta);

    // ---- 0 이하 정리(삭제) ----
    @Modifying
    @Query(value =
            "DELETE FROM inventory_weapons WHERE inventory_id = :invId AND weapon_code = :code AND amount <= 0",
            nativeQuery = true)
    int deleteIfZeroOrLess(@Param("invId") Long invId,
                           @Param("code") String weaponCode);
}
