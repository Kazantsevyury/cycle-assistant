package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class AwaitingTimezoneState implements UserStateHandler {

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            // Парсим смещение часового пояса
            int timezoneOffset = Integer.parseInt(messageText);

            // Сохраняем новый часовой пояс через UserEditService
            bot.userEditService.updateTimezone(chatId, timezoneOffset);

            // Отправляем подтверждение пользователю
            bot.sendMessage(chatId, "Ваш часовой пояс успешно изменён на: UTC" + (timezoneOffset >= 0 ? "+" : "") + timezoneOffset);
            // Получаем обновлённую клавиатуру с новыми данными
            InlineKeyboardMarkup updatedKeyboard = bot.userEditService.getUserEditor(chatId);

            // Отправляем сообщение с обновлённой клавиатурой
            bot.sendMessageWithKeyboard(chatId, "Настройки профиля обновлены:", updatedKeyboard);

            // Сбрасываем состояние на NoneState
            bot.changeUserState(chatId, new NoneState());

        } catch (NumberFormatException e) {
            // Если введено некорректное значение, отправляем сообщение об ошибке
            bot.sendMessage(chatId, "Пожалуйста, введите корректное число для часового пояса.");
        }
    }
}
