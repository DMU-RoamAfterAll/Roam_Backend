package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.Inventory;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.dto.RegisterRequest;
import com.cnwv.game_server.repository.InventoryRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.cnwv.game_server.shard.WithUserShard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * username 기준으로 홈 샤드에만 단일 쓰기.
     * 글로벌 조회는 UNION ALL 엔드포인트로 처리.
     */
    @Transactional
    @WithUserShard(userIdParam = "username")
    public String registerOnHomeShard(String username, RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null
                || request.getNickname() == null || request.getEmail() == null
                || request.getBirthDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "필수 항목 누락");
        }
        if (!username.equals(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username 불일치");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }

        LocalDate birth;
        try {
            birth = LocalDate.parse(request.getBirthDate());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "birthDate는 YYYY-MM-DD 형식이어야 합니다.");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setBirthDate(birth);
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);

        Inventory inv = new Inventory();
        inv.setUser(user);
        inventoryRepository.save(inv);

        user.setInventory(inv);

        return "회원가입 성공";
    }
}
