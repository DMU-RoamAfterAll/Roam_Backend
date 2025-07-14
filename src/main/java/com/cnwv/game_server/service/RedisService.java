package com.cnwv.game_server.service;

import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    public void setOnline(String username) {
        redisTemplate.opsForValue().set("online:" + username, "true");
    }

    public void removeOnline(String username) {
        redisTemplate.delete("online:" + username);
    }

    public boolean isOnline(String username) {
        return redisTemplate.hasKey("online:" + username);
    }

    public int getOnlineUserCount() {
        Set<String> keys = redisTemplate.keys("online:*");
        return keys != null ? keys.size() : 0;
    }

    public List<String> getOnlineUserNicknames() {
        Set<String> keys = redisTemplate.keys("online:*");
        if (keys == null) return List.of();

        return keys.stream()
                .map(key -> key.replace("online:", "")) // username
                .map(username -> userRepository.findByUsername(username)
                        .map(user -> user.getNickname())
                        .orElse("(알 수 없음)"))
                .toList();
    }
}
