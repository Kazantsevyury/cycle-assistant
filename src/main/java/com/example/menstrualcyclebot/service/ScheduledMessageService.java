package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.recommendations.MessageGenerationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
@Service
public class ScheduledMessageService {

    private final Bot bot;
    private final UserService userService;
    private final MessageGenerationService messageGenerationService;

    public ScheduledMessageService(Bot bot, UserService userService, MessageGenerationService messageGenerationService) {
        this.bot = bot;
        this.userService = userService;
        this.messageGenerationService = messageGenerationService;
    }

    @Scheduled(cron = "0 10 20 * * *", zone = "Europe/Moscow")
    public void sendScheduledGeneralRecommendations() {
        List<User> users = userService.findAllUsersWithGeneralRecommendations(); // Метод для получения пользователей

        for (User user : users) {
            try {
                String message = messageGenerationService.generateGeneralRecommendationsMessage(user);

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(user.getChatId().toString());
                sendMessage.setText(message);

                bot.execute(sendMessage);
            } catch (Exception e) {
                e.printStackTrace(); // Логируем ошибки для отладки
            }
        }
    }
}
