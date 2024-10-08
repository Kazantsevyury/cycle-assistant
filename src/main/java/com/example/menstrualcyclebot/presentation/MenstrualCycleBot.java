package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.CycleInfo;
import com.example.menstrualcyclebot.service.CycleService;
import com.example.menstrualcyclebot.utils.UIUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MenstrualCycleBot extends TelegramLongPollingBot {

    private final CycleService cycleService;
    private final String botToken;
    private final String botUsername;

    public MenstrualCycleBot(CycleService cycleService, String botToken, String botUsername) {
        this.cycleService = cycleService;
        this.botToken = botToken;
        this.botUsername = botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String chatId = update.getMessage().getChatId().toString();
            String receivedMessage = update.getMessage().getText();

            switch (receivedMessage) {
                case "/start":
                    sendWelcomeMessage(chatId);
                    break;
                case "Начать отслеживание":
                    sendTrackingMessage(chatId);
                    break;
                case "Статистика":
                    sendStatistics(chatId);
                    break;
                case "Помощь":
                    sendHelpMessage(chatId);
                    break;
                default:
                    sendTextMessage(chatId, "Неизвестная команда. Пожалуйста, используйте кнопки меню.");
            }
        }
    }

    private void sendWelcomeMessage(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Привет! Я бот для отслеживания менструального цикла. Используйте кнопки ниже для взаимодействия:");
        message.setReplyMarkup(UIUtils.createMenuKeyboard());
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    private void sendTrackingMessage(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Пожалуйста, введите данные в формате: ГГГГ-ММ-ДД <длительность цикла> <длительность менструации>");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    private void sendStatistics(String chatId) {
        // Здесь можно добавить логику для отправки статистики
        sendTextMessage(chatId, "Вот ваша статистика... (в разработке)");
    }

    private void sendHelpMessage(String chatId) {
        sendTextMessage(chatId, "Я помогу вам отслеживать менструальный цикл. Нажмите 'Начать отслеживание' для ввода данных или 'Статистика' для просмотра статистики.");
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }
/*
    private ReplyKeyboardMarkup createMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        // Первая строка клавиатуры
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Начать отслеживание"));
        row1.add(new KeyboardButton("Статистика"));

        // Вторая строка клавиатуры
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Помощь"));

        // Добавление строк на клавиатуру
        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true); // Автоматически подгоняет размер кнопок под экран пользователя
        keyboardMarkup.setOneTimeKeyboard(false); // Меню будет оставаться после использования

        return keyboardMarkup;
    }
*/
    @Override
    public String getBotUsername() {
        return botUsername;  // Возвращаем имя бота
    }

    @Override
    public String getBotToken() {
        return botToken;  // Возвращаем токен бота
    }
}
