package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updateNotificationSettings(Long chatId, User updatedSettings) {
        User user = userRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + chatId + " не найден."));

        // Обновляем настройки уведомлений
        user.setPhysicalActivityEnabled(updatedSettings.isPhysicalActivityEnabled());
        user.setNutritionEnabled(updatedSettings.isNutritionEnabled());
        user.setWorkProductivityNotification(updatedSettings.isWorkProductivityNotification());
        user.setRelationshipsCommunicationNotification(updatedSettings.isRelationshipsCommunicationNotification());
        user.setCareNotification(updatedSettings.isCareNotification());
        user.setEmotionalWellbeingNotification(updatedSettings.isEmotionalWellbeingNotification());
        user.setSexNotification(updatedSettings.isSexNotification());
        user.setFertilityWindowNotificationEnabled(updatedSettings.isFertilityWindowNotificationEnabled());
        user.setTimingOfFertilityWindowNotifications(updatedSettings.getTimingOfFertilityWindowNotifications());
        user.setDaysBeforeFertilityWindowNotifications(updatedSettings.getDaysBeforeFertilityWindowNotifications());
        user.setMenstruationStartNotificationEnabled(updatedSettings.isMenstruationStartNotificationEnabled());
        user.setTimingOfMenstruationStartNotifications(updatedSettings.getTimingOfMenstruationStartNotifications());
        user.setDaysBeforeMenstruationStartNotifications(updatedSettings.getDaysBeforeMenstruationStartNotifications());

        userRepository.save(user);
    }

    // Метод для получения настроек уведомлений пользователя
    @Transactional(readOnly = true)
    public User getNotificationSettings(Long chatId) {
        return userRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + chatId + " не найден."));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long chatId) {
        return userRepository.findById(chatId);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Cycle> findUserCyclesByChatId(Long chatId) {
        User user = userRepository.findByChatId(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с ID " + chatId + " не найден."));
        return user.getCycles();
    }

    @Transactional
    public void save(User user) {
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(User updatedUser) {
        // Опционально проверяем, существует ли пользователь
        if (!userRepository.existsById(updatedUser.getChatId())) {
            throw new EntityNotFoundException("Пользователь с ID " + updatedUser.getChatId() + " не найден.");
        }
        // Метод save() обновит запись, если она уже существует
        return userRepository.save(updatedUser);
    }

    // Удалить пользователя по ID
    @Transactional
    public void deleteById(Long chatId) {
        userRepository.deleteById(chatId);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long chatId) {
        return userRepository.existsById(chatId);
    }
}
