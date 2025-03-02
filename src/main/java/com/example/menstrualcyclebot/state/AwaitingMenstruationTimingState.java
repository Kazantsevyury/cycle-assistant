package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
public class AwaitingMenstruationTimingState implements UserStateHandler {
    private final long chatId;
    private final Integer messageId;

    // ✅ Правильный конструктор, принимающий messageId
    public AwaitingMenstruationTimingState(long chatId, Integer messageId) {
        this.chatId = chatId;
        this.messageId = messageId;
    }

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String inputText = update.getMessage().getText();

        try {
            // ✅ Проверяем формат времени
            LocalTime time = LocalTime.parse(inputText, DateTimeFormatter.ofPattern("HH:mm"));
            bot.getUserService().updateMenstruationTiming(chatId, time.toString());

            bot.sendMessage(chatId, "Время уведомлений обновлено на " + time + ".");

            // ✅ Проверяем, есть ли messageId перед обновлением меню
            if (messageId != null && messageId > 0) {
                try {
                    EditMessageReplyMarkup replyMarkup =
                            bot.getNotificationService().createMenstruationWindowMenu(chatId, messageId);
                    bot.execute(replyMarkup);
                } catch (TelegramApiException e) {
                    bot.sendMessage(chatId, "Не удалось обновить меню. Попробуйте снова.");
                    log.error("Error updating menu for chatId {}: {}", chatId, e.getMessage());
                }
            } else {
                bot.sendMessage(chatId, "Обновление меню невозможно, отправьте /start, чтобы обновить настройки.");
            }

            // ✅ Сбрасываем состояние пользователя
            bot.changeUserState(chatId, new NoneState());

        } catch (DateTimeParseException e) {
            bot.sendMessage(chatId, "Некорректный формат. Введите время в формате HH:mm (например, 08:30).");
        }
    }
}
