package com.cnwv.game_server.service;

import com.cnwv.game_server.repository.FriendRepository;
import org.springframework.stereotype.Service;

@Service
public class FriendService {

    private final FriendRepository friendRepository;

    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    public boolean areFriends(String nickname1, String nickname2) {
        return friendRepository.areFriends(nickname1, nickname2);
    }
}
