package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AwaitingPeriodLengthState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            int periodLength = Integer.parseInt(messageText);
            if (periodLength <= 0) {
                bot.sendMessage(chatId, "Длина периода должна быть положительным числом. Пожалуйста, попробуйте снова:");
                return;
            }

            cycle.setPeriodLength(periodLength);
            bot.changeUserState(chatId, new AwaitingCycleLengthState());
            bot.sendMessage(chatId, "Пожалуйста, введите длину вашего цикла (в днях):");

        } catch (NumberFormatException e) {
            bot.sendMessage(chatId, "Пожалуйста, введите корректное число.");
        }
    }
}

