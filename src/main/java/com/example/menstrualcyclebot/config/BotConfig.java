package com.example.menstrualcyclebot.config;

import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.repository.UserRepository;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.UserEditService;
import com.example.menstrualcyclebot.service.sbservices.DatabaseService;
import com.example.menstrualcyclebot.service.sbservices.CycleService;
import com.example.menstrualcyclebot.service.sbservices.UserCycleManagementService;
import com.example.menstrualcyclebot.service.sbservices.UserService;
import com.example.menstrualcyclebot.utils.CycleCalculator;
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
    public TelegramBotsApi telegramBotsApi(Bot bot) {
        TelegramBotsApi botsApi = null;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return botsApi;
    }

    @Bean
    public Bot menstrualCycleBot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            CycleService cycleService,
            UserCycleManagementService userCycleManagementService,
            CalendarService calendarService,
            DatabaseService databaseService,
            CycleCalculator cycleCalculator,
            UserEditService userEditService) {
        return new Bot(botToken, botUsername, userService, cycleService, userCycleManagementService, calendarService, databaseService, cycleCalculator, userEditService);
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