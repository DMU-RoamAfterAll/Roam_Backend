package com.cnwv.game_server.service;

import com.cnwv.game_server.repository.FriendRepository;
import com.cnwv.game_server.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public void updateLastRead(String user, String target, long timestamp) {
        String key = "last_read:" + user + ":" + target;
        redisTemplate.opsForValue().set(key, String.valueOf(timestamp));
    }

    public long getLastReadTimestamp(String user, String target) {
        String key = "last_read:" + user + ":" + target;
        String value = redisTemplate.opsForValue().get(key);
        return (value != null) ? Long.parseLong(value) : 0L;
    }

    public int getUnreadCount(String user, String target) {
        List<String> users = Arrays.asList(user, target);
        Collections.sort(users);
        String chatKey = "list:chat:" + users.get(0) + ":" + users.get(1);
        String readKey = "last_read:" + user + ":" + target;

        String lastReadStr = redisTemplate.opsForValue().get(readKey);
        long lastReadTime = (lastReadStr != null) ? Long.parseLong(lastReadStr) : 0;

        List<String> messages = redisTemplate.opsForList().range(chatKey, 0, -1);
        if (messages == null) return 0;

        int count = 0;
        for (String json : messages) {
            try {
                Map<String, Object> msg = new ObjectMapper().readValue(json, Map.class);
                if ("chat".equals(msg.get("type")) && msg.containsKey("timestamp")) {
                    long ts = Long.parseLong(msg.get("timestamp").toString());
                    if (ts > lastReadTime) count++;
                }
            } catch (Exception e) {
                log.warn("[getUnreadCount] 메시지 파싱 실패: {}", json);
            }
        }
        return count;
    }

    public Map<String, Object> getLastMessageInfo(String userA, String userB) {
        List<String> users = Arrays.asList(userA, userB);
        Collections.sort(users);
        String chatKey = "list:chat:" + users.get(0) + ":" + users.get(1);
        String lastMsg = redisTemplate.opsForList().index(chatKey, 0);

        if (lastMsg == null) return null;

        try {
            Map<String, Object> msgMap = new ObjectMapper().readValue(lastMsg, Map.class);
            String message = (String) msgMap.get("message");
            long timestamp = Long.parseLong(String.valueOf(msgMap.get("timestamp")));

            // 안 읽은 메시지 수
            int unreadCount = getUnreadCount(userA, userB);

            return Map.of(
                    "lastMessage", message,
                    "timestamp", timestamp,
                    "unreadCount", unreadCount
            );
        } catch (Exception e) {
            log.warn("[getLastMessageInfo] 메시지 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

}
