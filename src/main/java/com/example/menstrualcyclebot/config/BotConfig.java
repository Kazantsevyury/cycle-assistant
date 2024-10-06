package com.example.menstrualcyclebot.config;

import com.example.menstrualcyclebot.presentation.MenstrualCycleBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

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
}
