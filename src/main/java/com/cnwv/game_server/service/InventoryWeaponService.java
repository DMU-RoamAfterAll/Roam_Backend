package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.InventoryWeaponRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryWeaponService {

    private final InventoryWeaponRepository weaponRepository;
    private final UserRepository userRepository;

    public boolean insertWeapon(String username, String weaponCode, int amount) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        InventoryWeapon weapon = new InventoryWeapon();
        weapon.setId(id);
        weapon.setInventory(user.getInventory());
        weapon.setAmount(amount);

        weaponRepository.save(weapon);
        return true;
    }

    public List<InventoryWeapon> getWeapons(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return List.of();
        return weaponRepository.findByInventoryId(user.getInventory().getId());
    }

    public boolean updateWeapon(String username, String weaponCode, int amount) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        InventoryWeapon weapon = weaponRepository.findById(id).orElse(null);
        if (weapon == null) return false;

        weapon.setAmount(amount);
        weaponRepository.save(weapon);
        return true;
    }

    public boolean deleteWeapon(String username, String weaponCode) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getInventory() == null) return false;

        InventoryWeaponId id = new InventoryWeaponId();
        id.setInventoryId(user.getInventory().getId());
        id.setWeaponCode(weaponCode);

        if (!weaponRepository.existsById(id)) return false;
        weaponRepository.deleteById(id);
        return true;
    }
}
