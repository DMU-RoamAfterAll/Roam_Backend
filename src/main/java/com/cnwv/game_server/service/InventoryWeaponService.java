package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.InventoryWeaponRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryWeaponService {

    private final InventoryWeaponRepository weaponRepository;
    private final UserRepository userRepository;

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean insertWeapon(String username, String weaponCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();
        // 원자적 업서트(증가)
        weaponRepository.upsertIncrease(invId, weaponCode, amount);
        return true;
    }

    @WithUserShard(userIdParam = "username")
    public List<InventoryWeapon> getWeapons(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return weaponRepository.findByInventoryId(user.getInventory().getId());
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean updateWeapon(String username, String weaponCode, int amount) {
        if (amount < 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();
        int updated = weaponRepository.setAmount(invId, weaponCode, amount);
        return updated > 0;
    }

    @WithUserShard(userIdParam = "username")
    @Transactional
    public boolean deleteWeapon(String username, String weaponCode, int amount) {
        if (amount <= 0) return false;

        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        Long invId = user.getInventory().getId();

        // 1) 차감
        weaponRepository.decrement(invId, weaponCode, amount);
        // 2) 0 이하이면 삭제
        weaponRepository.deleteIfZeroOrLess(invId, weaponCode);

        return true;
    }
}
