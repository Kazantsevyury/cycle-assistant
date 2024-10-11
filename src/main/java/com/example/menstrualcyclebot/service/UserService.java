package com.example.menstrualcyclebot.service;


import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Найти пользователя по ID
    public Optional<User> findById(Long chatId) {
        return userRepository.findById(chatId);
    }

    // Найти всех пользователей
    public List<User> findAll() {
        return userRepository.findAll();
    }

    // Сохранить пользователя
    public User save(User user) {
        return userRepository.save(user);
    }

    // Удалить пользователя по ID
    public void deleteById(Long chatId) {
        userRepository.deleteById(chatId);
    }

    // Обновить пользователя
    public User updateUser(User user) {
        if (userRepository.existsById(user.getChatId())) {
            return userRepository.save(user);
        } else {
            throw new IllegalArgumentException("Пользователь с chatId " + user.getChatId() + " не найден");
        }
    }

    // Найти пользователя с менструальными циклами
    public Optional<User> findUserWithCycles(Long chatId) {
        return userRepository.findUserWithCycles(chatId);
    }
    // Проверить, существует ли пользователь по ID
    public boolean existsById(Long chatId) {
        return userRepository.existsById(chatId);
    }
}