package com.example.menstrualcyclebot.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MenstrualCycleBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if ("/start".equalsIgnoreCase(messageText)) {
                sendWelcomeMessageWithMenu(chatId);
            } else if ("Начать отслеживание".equalsIgnoreCase(messageText)) {
                sendTextMessage(chatId, "Начинаем отслеживание менструального цикла!");
            } else if ("Статистика".equalsIgnoreCase(messageText)) {
                sendTextMessage(chatId, "Здесь будет отображаться ваша статистика.");
            } else if ("Помощь".equalsIgnoreCase(messageText)) {
                sendTextMessage(chatId, "Я могу помочь вам отслеживать менструальный цикл. Вы можете использовать кнопки меню для управления.");
            } else {
                sendTextMessage(chatId, "Извините, я не понимаю эту команду. Пожалуйста, используйте кнопки меню.");
            }
        }
    }

    private void sendWelcomeMessageWithMenu(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Привет! Я бот для отслеживания менструального цикла. Используйте кнопки ниже для взаимодействия со мной:");

        // Установка клавиатуры
        ReplyKeyboardMarkup keyboardMarkup = createMenuKeyboard();
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message with menu: {}", e.getMessage());
        }
    }

    private ReplyKeyboardMarkup createMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true); // Клавиатура будет подстраиваться по размеру экрана
        keyboardMarkup.setOneTimeKeyboard(false); // Клавиатура останется на экране после нажатия кнопки

        // Создание кнопок
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Начать отслеживание"));
        row1.add(new KeyboardButton("Статистика"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Помощь"));

        // Добавление кнопок в список клавиатуры
        keyboard.add(row1);
        keyboard.add(row2);

        // Установка клавиатуры в разметку
        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }
}
