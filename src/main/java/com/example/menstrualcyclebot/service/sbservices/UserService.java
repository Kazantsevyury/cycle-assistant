package com.example.menstrualcyclebot.service.sbservices;


import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;



    @Transactional(readOnly = true)
    public Optional<User> findById(Long chatId) {
        return userRepository.findById(chatId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    // Удалить пользователя по ID
    public void deleteById(Long chatId) {
        userRepository.deleteById(chatId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long chatId) {
        return userRepository.existsById(chatId);
    }

}