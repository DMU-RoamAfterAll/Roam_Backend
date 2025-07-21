package com.cnwv.game_server.service;

import com.cnwv.game_server.repository.FriendRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final FriendRepository friendRepository;

    public void setOnline(String username) {
        redisTemplate.opsForValue().set("online:" + username, "true");
    }

    public void removeOnline(String username) {
        redisTemplate.delete("online:" + username);
    }

    public boolean isOnline(String username) {
        return redisTemplate.hasKey("online:" + username);
    }

    public void publishMessage(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
        if (channel.startsWith("chat:")) {
            String listKey = "list:" + channel;
            redisTemplate.opsForList().leftPush(listKey, message);
            redisTemplate.expire(listKey, Duration.ofDays(7));
        }
    }

    public List<String> getChatLog(String userA, String userB) {
        List<String> sorted = Arrays.asList(userA, userB);
        Collections.sort(sorted);
        String key = "list:chat:" + sorted.get(0) + ":" + sorted.get(1);
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    public int getOnlineUserCount() {
        Set<String> keys = redisTemplate.keys("online:*");
        return keys != null ? keys.size() : 0;
    }

    public List<String> getOnlineUserNicknames() {
        Set<String> keys = redisTemplate.keys("online:*");
        if (keys == null) return List.of();
        return keys.stream()
                .map(key -> key.replace("online:", ""))
                .map(username -> userRepository.findByUsername(username)
                        .map(user -> user.getNickname())
                        .orElse("(알 수 없음)"))
                .toList();
    }

    public boolean isFriend(String userA, String userB) {
        log.info("[isFriend] userA: {}, userB: {}", userA, userB);

        var userOpt = userRepository.findByUsername(userA);
        var targetOpt = userRepository.findByUsername(userB);

        if (userOpt.isEmpty() || targetOpt.isEmpty()) {
            log.warn("[isFriend] 사용자 조회 실패: userA={}, targetB={}", userOpt.isPresent(), targetOpt.isPresent());
            return false;
        }

        Long idA = userOpt.get().getId();
        Long idB = targetOpt.get().getId();
        log.info("[isFriend] ID 조회 성공: idA={}, idB={}", idA, idB);

        boolean result = friendRepository.isFriendById(idA, idB);
        log.info("[isFriend] 친구 여부 확인 결과: {}", result);
        return result;
    }

}
