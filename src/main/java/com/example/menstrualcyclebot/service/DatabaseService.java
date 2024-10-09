package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseService {

    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;

    public DatabaseService(UserRepository userRepository, CycleRepository cycleRepository) {
        this.userRepository = userRepository;
        this.cycleRepository = cycleRepository;
    }

    @Transactional
    public void deleteAllData() {
        // Удаляем все данные из таблицы менструальных циклов
        cycleRepository.deleteAll();

        // Удаляем все данные из таблицы пользователей
        userRepository.deleteAll();
    }
}
