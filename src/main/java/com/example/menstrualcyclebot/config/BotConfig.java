package com.example.menstrualcyclebot.config;

import com.example.menstrualcyclebot.presentation.MenstrualCycleBot;
import com.example.menstrualcyclebot.service.CycleService;
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
    public MenstrualCycleBot menstrualCycleBot(CycleService cycleService,
                                               @Value("${telegram.bot.token}") String botToken,
                                               @Value("${telegram.bot.username}") String botUsername) {
        return new MenstrualCycleBot(cycleService, botToken, botUsername);
    }
}
