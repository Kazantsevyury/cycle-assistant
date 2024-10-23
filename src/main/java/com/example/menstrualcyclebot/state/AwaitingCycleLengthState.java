package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AwaitingCycleLengthState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            int cycleLength = Integer.parseInt(messageText);
            if (cycleLength <= 0) {
                bot.sendMessage(chatId, "Длина цикла должна быть положительным числом. Пожалуйста, попробуйте снова:");
                return;
            }

            cycle.setCycleLength(cycleLength);

            bot.changeUserState(chatId, new NoneState());
        } catch (NumberFormatException e) {
            bot.sendMessage(chatId, "Пожалуйста, введите корректное число.");
        }
    }
}