package com.example.menstrualcyclebot.config;

import com.example.menstrualcyclebot.presentation.MenstrualCycleBot;
import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.repository.UserRepository;
import com.example.menstrualcyclebot.service.DatabaseService;
import com.example.menstrualcyclebot.service.CycleService;
import com.example.menstrualcyclebot.service.UserCycleManagementService;
import com.example.menstrualcyclebot.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {
    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Bean
    public TelegramBotsApi telegramBotsApi(MenstrualCycleBot menstrualCycleBot) {
        TelegramBotsApi botsApi = null;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(menstrualCycleBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return botsApi;
    }

    @Bean
    public MenstrualCycleBot menstrualCycleBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            CycleService cycleService,
            UserCycleManagementService userCycleManagementService,
            DatabaseService databaseService) {
        return new MenstrualCycleBot(botToken, botUsername, userService, cycleService, userCycleManagementService, databaseService);
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public CycleService menstrualCycleService(CycleRepository cycleRepository) {
        return new CycleService(cycleRepository);
    }

    @Bean
    public UserCycleManagementService userCycleManagementService(UserService userService, CycleService cycleService) {
        return new UserCycleManagementService(userService, cycleService);
    }
}