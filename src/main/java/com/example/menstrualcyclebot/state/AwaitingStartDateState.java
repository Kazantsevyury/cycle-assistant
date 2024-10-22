package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AwaitingStartDateState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        try {
            LocalDate startDate = LocalDate.parse(messageText);
            if (startDate.isAfter(LocalDate.now())) {
                bot.sendMessage(chatId, "Дата не может быть в будущем. Пожалуйста, введите корректную дату:");
                return;
            }

            cycle.setStartDate(startDate);
            bot.changeUserState(chatId, new AwaitingPeriodLengthState());
            bot.sendMessage(chatId, "Пожалуйста, введите длину вашего периода (в днях):");

        } catch (DateTimeParseException e) {
            bot.sendMessage(chatId, "Пожалуйста, введите дату в формате ГГГГ-ММ-ДД.");
        }
    }
}
