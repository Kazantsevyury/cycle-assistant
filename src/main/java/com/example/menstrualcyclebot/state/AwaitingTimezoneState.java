package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class AwaitingTimezoneState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        try {
            int timezoneOffset = Integer.parseInt(messageText);
            //bot.updateTimezone(update.getMessage().getChatId(), timezoneOffset);
        } catch (NumberFormatException e) {
            bot.sendMessage(update.getMessage().getChatId(), "Пожалуйста, введите корректное число для часового пояса.");
        }
    }
}
