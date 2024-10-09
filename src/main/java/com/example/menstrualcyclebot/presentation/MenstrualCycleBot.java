package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.service.CycleService;
import com.example.menstrualcyclebot.utils.UIUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.exceptions.TelegramApiException;


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
                case "Получить совет на день":
                    sendDailyAdvice(chatId);
                    break;
                case "Текущий день цикла":
                    sendCurrentCycleDay(chatId);
                    break;
                case "Настройка профиля":
                    sendProfileSettings(chatId);
                    break;
                case "Настроить уведомления":
                    sendNotificationSettings(chatId);
                    break;
                case "Календарь":
                    sendCalendar(chatId);
                    break;
                case "Новый цикл":
                    startNewCycle(chatId);
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

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }


    // Отправляет ежедневный совет пользователю
    private void sendDailyAdvice(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Вот ваш совет на день: ...");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Отправляет информацию о текущем дне цикла
    private void sendCurrentCycleDay(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Сегодня N-й день вашего цикла.");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Отправляет настройки профиля пользователю
    private void sendProfileSettings(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Настройки профиля: ...");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Отправляет настройки уведомлений пользователю
    private void sendNotificationSettings(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Настройки уведомлений: ...");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Отправляет календарь пользователю
    private void sendCalendar(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Ваш календарь: ...");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }

    // Начинает новый цикл для пользователя
    private void startNewCycle(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Новый цикл начат.");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения: {}", e.getMessage());
        }
    }
}
