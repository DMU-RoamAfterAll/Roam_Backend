package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.InventoryWeapon;
import com.cnwv.game_server.Entity.InventoryWeaponId;
import com.cnwv.game_server.repository.InventoryWeaponRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@WithUserShard(userIdParam = "username") // ✅ username 기반으로 샤드 라우팅
public class InventoryWeaponService {

    private final InventoryWeaponRepository weaponRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean insertWeapon(String username, String weaponCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        var existingOpt = weaponRepository.findById(id);
        if (existingOpt.isEmpty()) {
            InventoryWeapon weapon = new InventoryWeapon();
            weapon.setId(id);
            weapon.setInventory(user.getInventory());
            weapon.setAmount(amount);
            weaponRepository.save(weapon);
        } else {
            InventoryWeapon weapon = existingOpt.get();
            weapon.setAmount(weapon.getAmount() + amount); // ✅ 중복 시 수량 증가
            weaponRepository.save(weapon);
        }
        return true;
    }

    public List<InventoryWeapon> getWeapons(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return weaponRepository.findByInventoryId(user.getInventory().getId());
    }

    @Transactional
    public boolean updateWeapon(String username, String weaponCode, int amount) {
        if (amount < 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        InventoryWeapon weapon = weaponRepository.findById(id).orElse(null);
        if (weapon == null) return false;

        weapon.setAmount(amount); // 절대값 설정
        weaponRepository.save(weapon);
        return true;
    }

    @Transactional
    public boolean deleteWeapon(String username, String weaponCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        InventoryWeapon weapon = weaponRepository.findById(id).orElse(null);
        if (weapon == null) {
            // 수량이 없으면 요청 무시
            return true;
        }

        int remain = weapon.getAmount() - amount;
        if (remain > 0) {
            weapon.setAmount(remain);
            weaponRepository.save(weapon);
        } else {
            weaponRepository.deleteById(id); // 0 이하이면 행 삭제
        }
        return true;
    }
}
