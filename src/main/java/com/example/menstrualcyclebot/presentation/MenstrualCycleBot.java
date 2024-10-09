package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.repository.UserRepository;
import com.example.menstrualcyclebot.utils.UserState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.menstrualcyclebot.utils.CycleCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;
import static com.example.menstrualcyclebot.utils.UIUtils.welcomeKeyboard;


@Slf4j
@Component
public class MenstrualCycleBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;
    private final UserRepository userRepository;
    private final CycleRepository cycleRepository;
    private final Map<Long, MenstrualCycle> dataEntrySessions = new HashMap<>();
    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, MenstrualCycle> partialCycleData = new HashMap<>();


    @Autowired
    public MenstrualCycleBot(String botToken, String botUsername, UserRepository userRepository, CycleRepository cycleRepository) {
        super(new DefaultBotOptions(), botToken);
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userRepository = userRepository;
        this.cycleRepository = cycleRepository;
    }
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Проверяем, находится ли пользователь в процессе ввода данных
            if (userStates.containsKey(chatId) && userStates.get(chatId) != UserState.NONE) {
                handleDataEntrySteps(chatId, messageText);
                return;
            }

            // Иначе обрабатываем команды
            switch (messageText) {
                case "/start":
                    // Показать стартовое меню
                    sendMessageWithKeyboard(chatId, "Добро пожаловать! Выберите опцию ниже:", welcomeKeyboard());
                    break;
                case "ℹ️ info":
                    handleInfo(chatId);
                    break;
                case "✍️ Ввести данные":
                    handleDataEntry(chatId);
                    break;
                case "💡 Получить рекомендацию":
                    handleRecommendation(chatId);
                    break;
                case "📊 Статистика":
                    handleStatistics(chatId);
                    break;
                case "📅 Текущий день цикла":
                    handleCurrentDay(chatId);
                    break;
                case "👤 Настройка профиля":
                    handleProfileSettings(chatId);
                    break;
                case "🔔 Настроить уведомления":
                    handleNotificationSettings(chatId);
                    break;
                case "📆 Календарь":
                    handleCalendar(chatId);
                    break;
                case "🔄 Новый цикл":
                    handleNewCycle(chatId);
                    break;
                default:
                    sendMessage(chatId, "Неизвестная команда. Попробуйте снова.");
            }
        }
    }

    // Заглушки функций
    private void handleInfo(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Информация о боте.");
        // Возврат к стартовому меню
        sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", welcomeKeyboard());
    }

    private void handleDataEntry(long chatId) {
        userStates.put(chatId, UserState.AWAITING_CYCLE_LENGTH);
        partialCycleData.put(chatId, new MenstrualCycle());
        sendMessage(chatId, "Пожалуйста, введите длительность вашего цикла (в днях):");
    }
    private void handleDataEntrySteps(long chatId, String messageText) {
        UserState currentState = userStates.get(chatId);
        MenstrualCycle cycle = partialCycleData.get(chatId);

        try {
            switch (currentState) {
                case AWAITING_CYCLE_LENGTH:
                    int cycleLength = Integer.parseInt(messageText);
                    cycle.setCycleLength(cycleLength);
                    userStates.put(chatId, UserState.AWAITING_PERIOD_LENGTH);
                    sendMessage(chatId, "Введите длительность менструации (в днях):");
                    break;

                case AWAITING_PERIOD_LENGTH:
                    int periodLength = Integer.parseInt(messageText);
                    cycle.setPeriodLength(periodLength);
                    userStates.put(chatId, UserState.AWAITING_START_DATE);
                    sendMessage(chatId, "Введите дату начала последнего цикла (в формате ГГГГ-ММ-ДД):");
                    break;

                case AWAITING_START_DATE:
                    LocalDate startDate = LocalDate.parse(messageText);
                    cycle.setStartDate(startDate);

                    // Завершаем ввод данных
                    userStates.put(chatId, UserState.NONE);

                    // Связываем цикл с пользователем
                    User user = userRepository.findById(chatId).orElseGet(() -> {
                        User newUser = new User();
                        newUser.setChatId(chatId);
                        return userRepository.save(newUser);
                    });
                    cycle.setUser(user);

                    CycleCalculator.calculateCycleFields(cycle);

                    // Сохраняем цикл в репозитории
                    cycleRepository.save(cycle);

                    sendMessage(chatId, "Спасибо! Ваши данные сохранены.");
                    sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());

                    // Очищаем временные данные
                    partialCycleData.remove(chatId);
                    break;

                default:
                    sendMessage(chatId, "Произошла ошибка. Попробуйте снова.");
                    userStates.put(chatId, UserState.NONE);
                    partialCycleData.remove(chatId);
                    break;
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите число.");
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Пожалуйста, введите дату в формате ГГГГ-ММ-ДД.");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleRecommendation(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Получение рекомендации.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleStatistics(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Показать статистику.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleCurrentDay(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Показать текущий день цикла.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleProfileSettings(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Настройка профиля.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleNotificationSettings(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Настройка уведомлений.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleCalendar(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Открыть календарь.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    private void handleNewCycle(long chatId) {
        sendMessage(chatId, "Функционал разрабатывается: Начать новый цикл.");
        // Переход на главное меню
        sendMessageWithKeyboard(chatId, "Выберите опцию в основном меню:", createMenuKeyboard());
    }

    // Метод для отправки сообщений
    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Метод для отправки сообщения с клавиатурой
    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
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
}