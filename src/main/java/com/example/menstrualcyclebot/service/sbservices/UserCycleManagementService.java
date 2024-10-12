package com.example.menstrualcyclebot.service.sbservices;


import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
@RequiredArgsConstructor
@Service
public class UserCycleManagementService {

    private final UserService userService;
    private final CycleService cycleService;



    @Transactional
    public void addCycleToUser(Long chatId, Cycle cycle) {
        Optional<User> optionalUser = userService.findById(chatId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.getCycles().add(cycle);
            cycle.setUser(user);
            userService.save(user);
            cycleService.save(cycle);
        } else {
            throw new IllegalArgumentException("Пользователь с chatId " + chatId + " не найден");
        }
    }

}
