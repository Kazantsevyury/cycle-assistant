package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.StatisticsService;
import com.example.menstrualcyclebot.service.UserEditService;
import com.example.menstrualcyclebot.service.sbservices.CycleService;
import com.example.menstrualcyclebot.service.sbservices.DatabaseService;
import com.example.menstrualcyclebot.service.sbservices.UserCycleManagementService;
import com.example.menstrualcyclebot.service.sbservices.UserService;
import com.example.menstrualcyclebot.utils.UserState;
import com.example.menstrualcyclebot.utils.UserUtils;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.menstrualcyclebot.utils.CycleCalculator;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;

@Component
public class Bot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    private final UserService userService;
    private final CycleService cycleService;
    private final UserCycleManagementService userCycleManagementService;

    private final CalendarService calendarService;
    private final DatabaseService databaseService;
    private final CycleCalculator cycleCalculator;
    private final UserEditService userEditService;
    private final StatisticsService statisticsService;


    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Настрой количество потоков

    private final Map<Long, Cycle> dataEntrySessions = new HashMap<>();
    private final Map<Long, UserState> userStates = new HashMap<>();


    private final Map<Long, Cycle> partialCycleData = new HashMap<>();

    // Конструктор с присвоением полей
    public Bot(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.bot.username}") String botUsername,
            UserService userService,
            CycleService cycleService,
            UserCycleManagementService userCycleManagementService,
            CalendarService calendarService,
            DatabaseService databaseService,
            CycleCalculator cycleCalculator,
            UserEditService userEditService,
            StatisticsService statisticsService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;  // Инициализация поля userService
        this.cycleService = cycleService;  // Инициализация поля cycleService
        this.userCycleManagementService = userCycleManagementService;  // Инициализация поля userCycleManagementService
        this.calendarService = calendarService;  // Инициализация поля calendarService
        this.databaseService = databaseService;  // Инициализация поля databaseService
        this.cycleCalculator = cycleCalculator;  // Инициализация поля cycleCalculator
        this.userEditService = userEditService;  // Инициализация поля userEditService
        this.statisticsService = statisticsService;  // Инициализация поля statisticsService
    }


    private void processUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> processUpdate(update)); // Обработка каждого апдейта в отдельном потоке
    }

    public void handleIncomingMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            // Проверяем, находится ли пользователь в процессе ввода данных
            if (userStates.containsKey(chatId) && userStates.get(chatId) != UserState.NONE) {
                handleDataEntrySteps(update, messageText);
                return;
            }

            // Если пользователь в состоянии ожидания нового обращения
            if (userStates.containsKey(chatId) && userStates.get(chatId) == UserState.AWAITING_SALUTATION) {
                updateSalutation(chatId, messageText.trim()); // Вызов нового метода
                return;
            }

            // Проверяем, есть ли пользователь в базе данных
            if (!userService.existsById(chatId)) {
                User newUser = UserUtils.createNewUser(update);
                userService.save(newUser);
            }

            // Получение пользователя из базы данных
            Optional<User> optionalUser = userService.findById(chatId);
            User user = optionalUser.orElseThrow(() -> new IllegalStateException("User not found for chatId: " + chatId));
            String salutation = user.getSalutation() != null ? user.getSalutation() : user.getName();

            // Обработка команд
            switch (messageText) {
                case "/start":
                    sendMessageWithKeyboard(chatId, "Добро пожаловать! Выберите команду для начала.", createMenuKeyboard());
                    break;
                case "✍️ Ввести данные":
                    handleDataEntry(chatId);
                    sendMessage(chatId, "Хорошо, " + salutation + ", приступим к вводу данных.");
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
                    sendMessage(chatId, "Неизвестная команда, " + salutation + ". Попробуйте снова.");
            }
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
    }

    public void deleteAllData(long chatId) {
        try {
            databaseService.deleteAllData();
            sendMessage(chatId, "База стерта");
            sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", createMenuKeyboard());
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при удалении базы данных. Попробуйте снова позже.");
        }
    }

    public void handleDataEntry(long chatId) {
        userStates.put(chatId, UserState.AWAITING_CYCLE_LENGTH);
        partialCycleData.put(chatId, new Cycle());
        sendMessage(chatId, "Please enter your cycle length (in days):");
    }
    @Transactional
    public void handleDataEntrySteps(Update update, String messageText) {
        long chatId = update.getMessage().getChatId();
        UserState currentState = userStates.get(chatId);
        Cycle cycle = partialCycleData.get(chatId);


        try {
            switch (currentState) {
                case AWAITING_CYCLE_LENGTH:
                    processCycleLength(chatId, messageText, cycle);
                    break;
                case AWAITING_PERIOD_LENGTH:
                    processPeriodLength(chatId, messageText, cycle);
                    break;
                case AWAITING_START_DATE:
                    processStartDate(chatId, messageText, cycle);
                    break;
                default:
                    handleUnexpectedState(chatId, currentState);
                    break;
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Пожалуйста, введите корректное число.");
        } catch (DateTimeParseException e) {
            sendMessage(chatId, "Пожалуйста, введите дату в формате ГГГГ-ММ-ДД.");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Пожалуйста, попробуйте позже.");
            clearUserData(chatId);
        }
    }

    public void processCycleLength(long chatId, String messageText, Cycle cycle) {
        int cycleLength = Integer.parseInt(messageText);
        if (cycleLength <= 0) {
            sendMessage(chatId, "Длина цикла должна быть положительным числом. Пожалуйста, попробуйте снова:");
            return;
        }
        cycle.setCycleLength(cycleLength);
        userStates.put(chatId, UserState.AWAITING_PERIOD_LENGTH);
        sendMessage(chatId, "Введите длину вашего периода (в днях):");
    }

    public void processPeriodLength(long chatId, String messageText, Cycle cycle) {
        int periodLength = Integer.parseInt(messageText);
        if (periodLength <= 0) {
            sendMessage(chatId, "Длина периода должна быть положительным числом. Пожалуйста, попробуйте снова:");
            return;
        }
        cycle.setPeriodLength(periodLength);
        userStates.put(chatId, UserState.AWAITING_START_DATE);
        sendMessage(chatId, "Введите дату начала вашего последнего цикла (в формате ГГГГ-ММ-ДД):");
    }

    public void processStartDate(long chatId, String messageText, Cycle cycle) {
        LocalDate startDate = LocalDate.parse(messageText);
        if (startDate.isAfter(LocalDate.now())) {
            sendMessage(chatId, "Дата не может быть в будущем. Пожалуйста, введите корректную дату:");
            return;
        }
        cycle.setStartDate(startDate);
        completeDataEntry(chatId, cycle);
    }

    public void completeDataEntry(long chatId, Cycle cycle) {
        try {
            userStates.put(chatId, UserState.NONE);

            Optional<User> optionalUser = userService.findById(chatId);
            if (optionalUser.isPresent()) {
                cycle.setUser(optionalUser.get());
            } else {
                throw new IllegalStateException("Пользователь не найден для chatId: " + chatId);
            }
            cycleCalculator.calculateCycleFields(cycle);
            cycleService.save(cycle);

            sendMessage(chatId, "Спасибо! Ваши данные сохранены.");
            sendMessageWithKeyboard(chatId, "Пожалуйста, выберите вариант из главного меню:", createMenuKeyboard());

            partialCycleData.remove(chatId);
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при сохранении ваших данных. Пожалуйста, попробуйте позже.");
        }
    }

    public void handleUnexpectedState(long chatId, UserState currentState) {
        sendMessage(chatId, "Произошла ошибка в процессе. Пожалуйста, начните сначала или выберите команду из меню.");
        userStates.put(chatId, UserState.NONE);
    }

    public void clearUserData(long chatId) {
        userStates.remove(chatId);
        partialCycleData.remove(chatId);
    }

    public void handleRecommendation(long chatId) {
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Получение рекомендации.", createMenuKeyboard());
    }

    public void handleStatistics(long chatId) {

        try {
            // Генерация графика
            String chartFilePath = statisticsService.createLineChart(chatId);

            // Отправка графика пользователю
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(chatId));
            sendPhoto.setPhoto(new InputFile(new File(chartFilePath)));
            execute(sendPhoto);

        } catch (IOException e) {
            sendMessage(chatId, "Произошла ошибка при генерации статистики.");
        } catch (TelegramApiException e) {
        }
    }


    public void handleCurrentDay(long chatId) {
        Optional<User> optionalUser = userService.findById(chatId);

        // Получаем список циклов пользователя
        List<Cycle> cycles = optionalUser
                .map(User::getCycles)
                .orElse(Collections.emptyList());

        try {
            // Пытаемся получить актуальный цикл
            Cycle actualCycle = cycleService.getActualCycle(cycles);

            // Вычисляем текущий день цикла
            LocalDate today = LocalDate.now();
            int currentDay = (int) (today.toEpochDay() - actualCycle.getStartDate().toEpochDay()) + 1;

            // Отправляем сообщение пользователю с текущим днем цикла
            sendMessageWithKeyboard(chatId, "Сегодня: " + currentDay + " день цикла.", createMenuKeyboard());

        } catch (IllegalArgumentException e) {
            // Обрабатываем исключение, если актуальных циклов нет
            sendMessageWithKeyboard(chatId, e.getMessage(), createMenuKeyboard());
        }
    }


    public void handleProfileSettings(long chatId) {

        // Получаем клавиатуру для настройки профиля
        InlineKeyboardMarkup keyboard = userEditService.getUserEditor(chatId);

        // Отправляем клавиатуру пользователю
        sendMessageWithKeyboard(chatId, "Настройки профиля: " , keyboard);
    }
    public void updateSalutation(long chatId, String newSalutation) {
        Optional<User> userOptional = userService.findById(chatId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setSalutation(newSalutation);

            try {
                userService.save(user); // Попытка сохранить данные в базе
                userStates.put(chatId, UserState.NONE); // Сброс состояния
                InlineKeyboardMarkup updatedKeyboard = userEditService.getUserEditor(chatId);
                sendMessageWithKeyboard(chatId, "Ваше обращение обновлено.", updatedKeyboard);
            } catch (Exception e) {
                sendMessage(chatId, "Произошла ошибка при сохранении данных: " + e.getMessage());
            }

        } else {
            sendMessage(chatId, "Пользователь не найден.");
        }
    }


    private void handleNotificationSettings(long chatId) {
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Настройка уведомлений.", createMenuKeyboard());
    }

    @Transactional
    public void handleCalendar(long chatId) {
        try {
            sendMessage(chatId, "Ваш календарь на октябрь:");
            sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", calendarService.getCalendar(2024, 10, chatId));
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при генерации календаря. Попробуйте снова позже.");
        }
    }
    public void handleCallback(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        try {
             if (callbackData.equals("edit_salutation")) {
                userStates.put(chatId, UserState.AWAITING_SALUTATION); // Устанавливаем состояние пользователя
                sendMessage(chatId, "Введите новое обращение:"); // Запрашиваем новое обращение

        } else if (callbackData.startsWith("navigate:")) {
                String[] data = callbackData.split(":");
                int year = Integer.parseInt(data[1]);
                int month = Integer.parseInt(data[2]);


                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(String.valueOf(chatId));
                editMessage.setMessageId(callbackQuery.getMessage().getMessageId());
                editMessage.setText("Календарь:");
                editMessage.setReplyMarkup(calendarService.getCalendar(year, month, chatId));

                execute(editMessage);
            }
            else {
                sendMessage(chatId, "Произошла ошибка. Попробуйте снова.");
            }
        } catch (NumberFormatException e) {
            sendMessage(chatId, "Произошла ошибка при обработке команды. Попробуйте снова.");
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
    }


    public void handleNewCycle(long chatId) {
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Начать новый цикл.", createMenuKeyboard());
    }

    public void sendMessage(long chatId, String text) {
        sendMessageWithKeyboard(chatId, text, null);
    }

    public void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboard keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboardMarkup != null) {
            message.setReplyMarkup(keyboardMarkup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
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

    @PreDestroy
    public void shutDown() {
        executorService.shutdown();  // Начинаем завершение ExecutorService
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();  // Принудительное завершение, если долго завершается
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
