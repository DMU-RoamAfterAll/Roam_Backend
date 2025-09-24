package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.Inventory;
import com.cnwv.game_server.Entity.User;
import com.cnwv.game_server.dto.RegisterRequest;
import com.cnwv.game_server.repository.InventoryRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserDualWriteService {

    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate; // 라우팅 DS(기본 s0) 사용. fully-qualified로 s1에도 쓸 수 있음.

    /**
     * 회원가입: s0(JPA) 저장 + s1(native) 동기 삽입 (단일 트랜잭션)
     */
    @Transactional
    public String registerBothShards(RegisterRequest request) {
        // 0) 중복 체크: 두 샤드 모두 확인 (간단히 UNION 방식으로)
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT ( " +
                        "(SELECT COUNT(*) FROM cnwvdb_s0.users WHERE username = ?) + " +
                        "(SELECT COUNT(*) FROM cnwvdb_s1.users WHERE username = ?) " +
                        ") AS total",
                Integer.class, request.getUsername(), request.getUsername()
        );
        if (cnt != null && cnt > 0) {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        }

        // 1) s0에 JPA로 저장 (자동증가 id 확보)
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setBirthDate(LocalDate.parse(request.getBirthDate()));
        user.setEmail(request.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        User saved = userRepository.save(user); // s0에 INSERT
        Long userId = saved.getId();

        // 인벤토리도 s0에 생성
        Inventory inv = new Inventory();
        inv.setUser(saved);
        inventoryRepository.save(inv);
        saved.setInventory(inv);

        // 2) s1에 native SQL로 사용자/인벤토리 동기 삽입 (동일 userId로)
        jdbcTemplate.update(
                "INSERT INTO cnwvdb_s1.users " +
                        " (id, username, password, nickname, birth_date, email, created_at, refresh_token) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NULL)",
                userId,
                request.getUsername(),
                saved.getPassword(),
                request.getNickname(),
                Date.valueOf(saved.getBirthDate()),
                request.getEmail()
        );

        jdbcTemplate.update(
                "INSERT INTO cnwvdb_s1.inventories (user_id, created_at) VALUES (?, NOW())",
                userId
        );

        return "회원가입 성공";
    }
}
