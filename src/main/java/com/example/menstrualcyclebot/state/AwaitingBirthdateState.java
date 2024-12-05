package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.utils.DateParserUtils;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.LocalDate;

public class AwaitingBirthdateState implements UserStateHandler {

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        // Используем DateParserUtils для обработки даты
        LocalDate birthdate = DateParserUtils.parseDateWithMultipleFormats(messageText);

        if (birthdate == null) {
            // Сообщаем пользователю, если дата не была распознана
            bot.sendMessage(chatId, "Пожалуйста, введите дату в одном из поддерживаемых формате: dd.mm.yyyy.");
            return;
        }

        if (birthdate.isAfter(LocalDate.now())) {
            bot.sendMessage(chatId, "Дата рождения не может быть в будущем. Пожалуйста, введите корректную дату.");
            return;
        }

        // Сохраняем дату рождения через UserEditService
        bot.userEditService.updateBirthdate(chatId, birthdate);

        // Отправляем подтверждение пользователю
        bot.sendMessage(chatId, "Дата рождения успешно обновлена на: " + birthdate);

        // Получаем обновлённую клавиатуру с новыми данными
        InlineKeyboardMarkup updatedKeyboard = bot.userEditService.getUserEditor(chatId);

        // Отправляем сообщение с обновлённой клавиатурой
        bot.sendMessageWithKeyboard(chatId, "Настройки профиля обновлены:", updatedKeyboard);

        // Сбрасываем состояние на NoneState
        bot.changeUserState(chatId, new NoneState());
    }
}
