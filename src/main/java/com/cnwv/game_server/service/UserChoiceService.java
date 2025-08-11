package com.cnwv.game_server.service;

import com.cnwv.game_server.Entity.*;
import com.cnwv.game_server.repository.UserChoiceRepository;
import com.cnwv.game_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserChoiceService {

    private final UserChoiceRepository choiceRepository;
    private final UserRepository userRepository;

    /**
     * 선택지가 없으면 INSERT, 있으면 UPDATE. 동일 값이면 NO-OP 처리
     */
    @Transactional
    public boolean setChoice(String username, String choiceCode, boolean condition) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;

        UserChoiceId id = new UserChoiceId();
        id.setUserId(user.getId());
        id.setChoiceCode(choiceCode);

        var existingOpt = choiceRepository.findById(id);
        if (existingOpt.isEmpty()) {
            UserChoice choice = new UserChoice();
            choice.setId(id);
            choice.setUser(user);
            choice.setCondition(condition);
            choiceRepository.save(choice); // INSERT
            return true;
        } else {
            UserChoice existing = existingOpt.get();
            if (existing.isCondition() == condition) {
                // 동일 값이면 아무 것도 하지 않고 성공 처리
                return true;
            }
            existing.setCondition(condition);
            choiceRepository.save(existing); // UPDATE
            return true;
        }
    }

    public List<UserChoice> getChoices(String username) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return List.of();
        return choiceRepository.findByUserId(user.getId());
    }

    public Boolean getCondition(String username, String choiceCode) {
        var user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return null;

        UserChoiceId id = new UserChoiceId();
        id.setUserId(user.getId());
        id.setChoiceCode(choiceCode);

        return choiceRepository.findById(id)
                .map(UserChoice::isCondition)
                .orElse(null);
    }
}