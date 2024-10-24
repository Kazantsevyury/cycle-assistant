package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Arrays;
import java.util.List;

public class AwaitingSalutationState implements UserStateHandler {

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        long chatId = update.getMessage().getChatId();
        String newSalutation = update.getMessage().getText();  // Получаем новое обращение

        if (newSalutation == null || newSalutation.isEmpty()) {
            bot.sendMessage(chatId, "Обращение не может быть пустым. Пожалуйста, введите корректное обращение.");
            return;
        }

        // Список неприемлемых слов
        List<String> forbiddenWords = Arrays.asList("хуй", "Никита", "Хуесос", "пизда", "пидор");

        // Проверяем, если обращение содержит неприемлемые слова
        if (forbiddenWords.stream().anyMatch(newSalutation.toLowerCase()::contains)) {
            newSalutation = "Обожаю лизать очко Юрика";
            bot.sendMessage(chatId, "Поздравляю, хуесос, твое обращение актуализировано.");
        }

        // Сохранение обращения через UserEditService
        try {
            bot.userEditService.updateSalutation(chatId, newSalutation);  // Обновляем обращение
            bot.sendMessage(chatId, "Ваше обращение успешно изменено на: " + newSalutation);
        } catch (Exception e) {
            bot.sendMessage(chatId, "Произошла ошибка при обновлении обращения. Попробуйте снова.");
            return;
        }

        // Получаем обновлённую клавиатуру с новыми данными
        InlineKeyboardMarkup updatedKeyboard = bot.userEditService.getUserEditor(chatId);

        // Отправляем сообщение с обновлённой клавиатурой
        bot.sendMessageWithKeyboard(chatId, "Настройки профиля обновлены:", updatedKeyboard);

        // Сброс состояния на NoneState
        bot.changeUserState(chatId, new NoneState());
    }
}
