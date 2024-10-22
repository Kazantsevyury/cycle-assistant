package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AwaitingBirthdateState implements UserStateHandler {
    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        try {
            LocalDate birthdate = LocalDate.parse(messageText);
            if (birthdate.isAfter(LocalDate.now())) {
                bot.sendMessage(update.getMessage().getChatId(), "Дата рождения не может быть в будущем. Пожалуйста, введите корректную дату.");
                return;
            }
          //  bot.updateBirthdate(update.getMessage().getChatId(), birthdate);
        } catch (DateTimeParseException e) {
            bot.sendMessage(update.getMessage().getChatId(), "Пожалуйста, введите дату в формате ГГГГ-ММ-ДД.");
        }
    }
}
