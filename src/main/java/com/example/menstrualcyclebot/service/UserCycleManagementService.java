package com.example.menstrualcyclebot.service;


import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserCycleManagementService {

    private final UserService userService;
    private final CycleService cycleService;

    public UserCycleManagementService(UserService userService, CycleService cycleService) {
        this.userService = userService;
        this.cycleService = cycleService;
    }

    @Transactional
    public void addCycleToUser(Long chatId, MenstrualCycle cycle) {
        Optional<User> optionalUser = userService.findUserWithCycles(chatId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.getMenstrualCycles().add(cycle);
            cycle.setUser(user);
            userService.save(user);
            cycleService.save(cycle);
        } else {
            throw new IllegalArgumentException("Пользователь с chatId " + chatId + " не найден");
        }
    }

}
