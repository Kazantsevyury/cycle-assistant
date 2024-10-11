package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;

import com.example.menstrualcyclebot.service.*;

import com.example.menstrualcyclebot.utils.UserState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.menstrualcyclebot.utils.CycleCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;

@AllArgsConstructor
@Slf4j
@Component
public class MenstrualCycleBot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    private final UserService userService;
    private final CycleService cycleService;
    private final UserCycleManagementService userCycleManagementService;

    private final CalendarService calendarService = new CalendarService();
    private final DatabaseService databaseService;

    private final Map<Long, MenstrualCycle> dataEntrySessions = new HashMap<>();
    private final Map<Long, UserState> userStates = new HashMap<>();
    private final Map<Long, MenstrualCycle> partialCycleData = new HashMap<>();

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            // Проверяем, есть ли пользователь в базе данных
            if (!userService.existsById(chatId)) {
                // Если это первый раз, когда пользователь взаимодействует с ботом, отправляем приветственное сообщение
                //sendMessage(chatId, "Привет! Я твой помощник для отслеживания менструального цикла. Нажми /start, чтобы начать.");
                // Сохраняем пользователя в базу данных, чтобы не отправлять приветственное сообщение снова
                User newUser = new User();
                newUser.setChatId(chatId);
                newUser.setUsername(update.getMessage().getFrom().getUserName());
                userService.save(newUser);
                handleDataEntry(chatId);
                return;
            }

            // Проверяем, находится ли пользователь в процессе ввода данных
            if (userStates.containsKey(chatId) && userStates.get(chatId) != UserState.NONE) {
                handleDataEntrySteps(update, messageText);
                return;
            }

            // Иначе обрабатываем команды
            switch (messageText) {
                case "/start":
                    // Заглушка
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
                case "Удалить базу":
                    deleteAllData(chatId);
                    break;

                default:
                    sendMessage(chatId, "Неизвестная команда. Попробуйте снова.");
            }
        }
        //  Обработка callback data для календаря
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }
    private void deleteAllData(long chatId) {
        sendMessage(chatId, "База стерта");
        databaseService.deleteAllData();
        sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", createMenuKeyboard());
    }

    private void handleDataEntry(long chatId) {
        userStates.put(chatId, UserState.AWAITING_CYCLE_LENGTH);
        partialCycleData.put(chatId, new MenstrualCycle());
        sendMessage(chatId, "Пожалуйста, введите длительность вашего цикла (в днях):");
    }
    @Transactional
    private void handleDataEntrySteps(Update update, String messageText) {
        long chatId = update.getMessage().getChatId();
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
                    User user = userService.findById(chatId).orElseGet(() -> {
                        User newUser = new User();
                        newUser.setChatId(chatId);

                        String username = update.getMessage().getFrom().getUserName();
                        newUser.setUsername(username);
                        return userService.save(newUser);
                    });
                    cycle.setUser(user);

                    CycleCalculator.calculateCycleFields(cycle);

                    // Сохраняем цикл в репозитории
                    cycleService.save(cycle);

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

    @Transactional
    private void handleCalendar(long chatId) {
        // Создаем календарь на октябрь 2024 года (можно поменять на текущий год и месяц)
        InlineKeyboardMarkup calendarMarkup = calendarService.getCalendar(2024, 10);

        // Создаем сообщение с текстом и прикрепляем клавиатуру календаря
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Ваш календарь на октябрь:");
        message.setReplyMarkup(calendarMarkup); // Прикрепляем разметку

        // Отправляем сообщение
        try {
            execute(message); // метод execute отправляет сообщение в чат
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void handleCallback(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        if (callbackData.startsWith("navigate:")) {
            // Извлекаем год и месяц из callbackData
            String[] data = callbackData.split(":");
            int year = Integer.parseInt(data[1]);
            int month = Integer.parseInt(data[2]);

            // Генерируем обновлённый календарь
            EditMessageText editMessage = new EditMessageText();
            editMessage.setChatId(callbackQuery.getMessage().getChatId().toString());
            editMessage.setMessageId(callbackQuery.getMessage().getMessageId());
            editMessage.setText("Календарь:");
            editMessage.setReplyMarkup(calendarService.getCalendar(year, month));

            try {
                execute(editMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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