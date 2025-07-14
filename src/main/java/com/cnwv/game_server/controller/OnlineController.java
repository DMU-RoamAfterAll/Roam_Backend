package com.cnwv.game_server.controller;

import com.cnwv.game_server.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/online")
@RequiredArgsConstructor
public class OnlineController {

    private final RedisService redisService;

    @GetMapping("/count")
    public int getOnlineUserCount() {
        return redisService.getOnlineUserCount();
    }

    @GetMapping("/list")
    public List<String> getOnlineUserNicknames() {
        return redisService.getOnlineUserNicknames();
    }
}

