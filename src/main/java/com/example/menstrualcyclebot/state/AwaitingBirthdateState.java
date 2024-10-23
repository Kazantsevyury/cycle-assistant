package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AwaitingBirthdateState implements UserStateHandler {

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();
        try {
            LocalDate birthdate = LocalDate.parse(messageText);
            if (birthdate.isAfter(LocalDate.now())) {
                bot.sendMessage(chatId, "Дата рождения не может быть в будущем. Пожалуйста, введите корректную дату.");
                return;
            }
            // Сохраняем дату рождения через UserEditService
            bot.userEditService.updateBirthdate(chatId, birthdate);

            bot.sendMessage(chatId, "Дата рождения успешно обновлена на: " + birthdate);
        } catch (DateTimeParseException e) {
            bot.sendMessage(chatId, "Пожалуйста, введите дату в формате ГГГГ-ММ-ДД.");
            return;
        }
        // Получаем обновлённую клавиатуру с новыми данными
        InlineKeyboardMarkup updatedKeyboard = bot.userEditService.getUserEditor(chatId);

        // Отправляем сообщение с обновлённой клавиатурой
        bot.sendMessageWithKeyboard(chatId, "Настройки профиля обновлены:", updatedKeyboard);

        // Сброс состояния
        bot.changeUserState(chatId, new NoneState());
    }
}
