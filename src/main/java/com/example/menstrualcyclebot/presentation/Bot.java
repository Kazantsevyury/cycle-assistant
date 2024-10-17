package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.sbservices.CycleService;
import com.example.menstrualcyclebot.service.sbservices.DatabaseService;
import com.example.menstrualcyclebot.service.sbservices.UserCycleManagementService;
import com.example.menstrualcyclebot.service.sbservices.UserService;
import com.example.menstrualcyclebot.utils.UIUtils;
import com.example.menstrualcyclebot.utils.UserState;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.example.menstrualcyclebot.utils.CycleCalculator;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;
import static com.example.menstrualcyclebot.utils.UserState.*;


@Slf4j
public class Bot extends TelegramLongPollingBot {

    private final String botToken;
    private final String botUsername;

    private final UserService userService;
    private final CycleService cycleService;
    private final UserCycleManagementService userCycleManagementService;

    private final CalendarService calendarService;
    private final DatabaseService databaseService;
    private final CycleCalculator cycleCalculator;
    private final Map<Long, Cycle> dataEntrySessions = new HashMap<>();

    private final Map<Long, UserState> userStates = new HashMap<>();


    private final Map<Long, Cycle> partialCycleData = new HashMap<>();

    public Bot(String botToken, String botUsername, UserService userService, CycleService cycleService, UserCycleManagementService userCycleManagementService, CalendarService calendarService, DatabaseService databaseService, CycleCalculator cycleCalculator ) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;
        this.cycleService = cycleService;
        this.userCycleManagementService = userCycleManagementService;
        this.calendarService = calendarService;
        this.databaseService = databaseService;
        this.cycleCalculator = cycleCalculator;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    private void handleIncomingMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            // Проверяем, находится ли пользователь в процессе ввода данных
            if (userStates.containsKey(chatId) && userStates.get(chatId) != UserState.NONE) {
                handleDataEntrySteps(update, messageText);
                return;
            }

            // Проверяем, есть ли пользователь в базе данных
            if (!userService.existsById(chatId)) {
                log.info("Пользователь с chatId {} не найден в базе данных. Создаем нового пользователя.", chatId);
                User newUser = new User();
                newUser.setChatId(chatId);
                newUser.setUsername(update.getMessage().getFrom().getUserName());
                // Заполнение имени, если оно доступно у пользователя Telegram
                if (update.getMessage().getFrom().getFirstName() != null) {
                    newUser.setName(update.getMessage().getFrom().getFirstName());
                }
                // Дата рождения не передается из ТГ
                userService.save(newUser);
            }

            // Обработка команд
            switch (messageText) {
                case "/start":
                    log.info("Пользователь с chatId {} начал взаимодействие с ботом.", chatId);
                    sendMessageWithKeyboard(chatId, "Добро пожаловать! Выберите команду для начала.", createMenuKeyboard());
                    break;
                case "✍️ Ввести данные":
                    handleDataEntry(chatId);
                    log.info("Пользователь с chatId {} выбрал ввод данных.", chatId);
                    break;
                case "💡 Получить рекомендацию":
                    log.info("Пользователь с chatId {} запросил рекомендацию.", chatId);
                    handleRecommendation(chatId);
                    break;
                case "📊 Статистика":
                    log.info("Пользователь с chatId {} запросил статистику.", chatId);
                    handleStatistics(chatId);
                    break;
                case "📅 Текущий день цикла":
                    log.info("Пользователь с chatId {} запросил текущий день цикла.", chatId);
                    handleCurrentDay(chatId);
                    break;
                case "👤 Настройка профиля":
                    log.info("Пользователь с chatId {} выбрал настройку профиля.", chatId);
                    handleProfileSettings(chatId);
                    break;
                case "🔔 Настроить уведомления":
                    log.info("Пользователь с chatId {} выбрал настройку уведомлений.", chatId);
                    handleNotificationSettings(chatId);
                    break;
                case "📆 Календарь":
                    log.info("Пользователь с chatId {} запросил календарь.", chatId);
                    handleCalendar(chatId);
                    break;
                case "🔄 Новый цикл":
                    log.info("Пользователь с chatId {} начал новый цикл.", chatId);
                    handleNewCycle(chatId);
                    break;
                case "Удалить базу":
                    log.warn("Пользователь с chatId {} запросил удаление всей базы данных.", chatId);
                    deleteAllData(chatId);
                    break;
                default:
                    log.warn("Пользователь с chatId {} ввел неизвестную команду: {}", chatId, messageText);
                    sendMessage(chatId, "Неизвестная команда. Попробуйте снова.");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке сообщения от пользователя с chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
    }


    private void deleteAllData(long chatId) {
        try {
            log.info("Удаление всех данных для пользователя с chatId {}", chatId);
            databaseService.deleteAllData();
            sendMessage(chatId, "База стерта");
            sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", createMenuKeyboard());
        } catch (Exception e) {
            log.error("Ошибка при удалении базы данных для пользователя с chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка при удалении базы данных. Попробуйте снова позже.");
        }
    }

    private void handleDataEntry(long chatId) {
        log.info("[handleDataEntry] Starting data entry for user with chatId: {}", chatId);
        userStates.put(chatId, UserState.AWAITING_CYCLE_LENGTH);
        partialCycleData.put(chatId, new Cycle());
        sendMessage(chatId, "Please enter your cycle length (in days):");
    }

    @Transactional
    private void handleDataEntrySteps(Update update, String messageText) {
        long chatId = update.getMessage().getChatId();
        UserState currentState = userStates.get(chatId);
        Cycle cycle = partialCycleData.get(chatId);

        log.info("[handleDataEntrySteps] Processing data entry step for user with chatId: {}. Current state: {}", chatId, currentState);

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
            log.error("[handleDataEntrySteps] Invalid number format entered by user with chatId: {}. Input: {}", chatId, messageText, e);
            sendMessage(chatId, "Please enter a valid number.");
        } catch (DateTimeParseException e) {
            log.error("[handleDataEntrySteps] Invalid date format entered by user with chatId: {}. Input: {}", chatId, messageText, e);
            sendMessage(chatId, "Please enter the date in YYYY-MM-DD format.");
        } catch (Exception e) {
            log.error("[handleDataEntrySteps] Unexpected error for user with chatId: {}. Error: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "An error occurred. Please try again later.");
            clearUserData(chatId);
        }
    }

    private void processCycleLength(long chatId, String messageText, Cycle cycle) {
        int cycleLength = Integer.parseInt(messageText);
        if (cycleLength <= 0) {
            log.warn("[processCycleLength] User with chatId: {} entered non-positive cycle length: {}", chatId, cycleLength);
            sendMessage(chatId, "Cycle length must be a positive number. Please try again:");
            return;
        }
        cycle.setCycleLength(cycleLength);
        userStates.put(chatId, UserState.AWAITING_PERIOD_LENGTH);
        sendMessage(chatId, "Enter your period length (in days):");
    }

    private void processPeriodLength(long chatId, String messageText, Cycle cycle) {
        int periodLength = Integer.parseInt(messageText);
        if (periodLength <= 0) {
            log.warn("[processPeriodLength] User with chatId: {} entered non-positive period length: {}", chatId, periodLength);
            sendMessage(chatId, "Period length must be a positive number. Please try again:");
            return;
        }
        cycle.setPeriodLength(periodLength);
        userStates.put(chatId, UserState.AWAITING_START_DATE);
        sendMessage(chatId, "Enter the start date of your last cycle (in YYYY-MM-DD format):");
    }

    private void processStartDate(long chatId, String messageText, Cycle cycle) {
        LocalDate startDate = LocalDate.parse(messageText);
        if (startDate.isAfter(LocalDate.now())) {
            log.warn("[processStartDate] User with chatId: {} entered a future date: {}", chatId, startDate);
            sendMessage(chatId, "Date cannot be in the future. Please enter a valid date:");
            return;
        }
        cycle.setStartDate(startDate);
        completeDataEntry(chatId, cycle);
    }

    private void completeDataEntry(long chatId, Cycle cycle) {
        try {
            userStates.put(chatId, UserState.NONE);

            log.info("[completeDataEntry] Saving cycle data for user with chatId: {}", chatId);
            Optional<User> optionalUser = userService.findById(chatId);
            if (optionalUser.isPresent()) {
                cycle.setUser(optionalUser.get());
            } else {
                throw new IllegalStateException("User not found for chatId: " + chatId);
            }            cycleCalculator.calculateCycleFields(cycle);
            cycleService.save(cycle);

            sendMessage(chatId, "Thank you! Your data has been saved.");
            sendMessageWithKeyboard(chatId, "Please select an option from the main menu:", createMenuKeyboard());

            partialCycleData.remove(chatId);
            log.info("[completeDataEntry] Temporary cycle data cleared for user with chatId: {}", chatId);
        } catch (Exception e) {
            log.error("[completeDataEntry] Error while saving data for user with chatId: {}. Error: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "An error occurred while saving your data. Please try again later.");
        }
    }
    private void handleUnexpectedState(long chatId, UserState currentState) {
        log.warn("[handleUnexpectedState] User with chatId: {} is in an unexpected state: {}", chatId, currentState);
        sendMessage(chatId, "Произошла ошибка в процессе. Пожалуйста, начните сначала или выберите команду из меню.");
        userStates.put(chatId, UserState.NONE);
    }

    private void clearUserData(long chatId) {
        log.info("[clearUserData] Clearing data for user with chatId: {}", chatId);
        userStates.remove(chatId);
        partialCycleData.remove(chatId);
    }



    private void handleRecommendation(long chatId) {
        log.info("Функционал рекомендаций запрошен пользователем с chatId {}", chatId);
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Получение рекомендации.", createMenuKeyboard());
    }

    private void handleStatistics(long chatId) {
        log.info("Функционал статистики запрошен пользователем с chatId {}", chatId);
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Показать статистику.", createMenuKeyboard());
    }

    private void handleCurrentDay(long chatId) {
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


    private void handleProfileSettings(long chatId) {
        log.info("Функционал настройки профиля запрошен пользователем с chatId {}", chatId);
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Настройка профиля.", createMenuKeyboard());
    }

    private void handleNotificationSettings(long chatId) {
        log.info("Функционал настройки уведомлений запрошен пользователем с chatId {}", chatId);
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Настройка уведомлений.", createMenuKeyboard());
    }

    @Transactional
    private void handleCalendar(long chatId) {
        try {
            log.info("Запрошен календарь для пользователя с chatId {}", chatId);
            sendMessage(chatId, "Ваш календарь на октябрь:");
            sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", calendarService.getCalendar(2024, 10, chatId));
        } catch (Exception e) {
            log.error("Ошибка при генерации календаря для пользователя с chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка при генерации календаря. Попробуйте снова позже.");
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        try {
            if (callbackData.startsWith("navigate:")) {
                String[] data = callbackData.split(":");
                int year = Integer.parseInt(data[1]);
                int month = Integer.parseInt(data[2]);

                log.info("Обработка навигации по календарю для пользователя с chatId {}: год {}, месяц {}", chatId, year, month);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(String.valueOf(chatId));
                editMessage.setMessageId(callbackQuery.getMessage().getMessageId());
                editMessage.setText("Календарь:");
                editMessage.setReplyMarkup(calendarService.getCalendar(year, month, chatId));

                execute(editMessage);
            }
        } catch (NumberFormatException e) {
            log.error("Ошибка формата числа при обработке навигации по календарю для пользователя с chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка при обработке команды. Попробуйте снова.");
        } catch (Exception e) {
            log.error("Ошибка при обработке callback для пользователя с chatId {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
    }

    private void handleNewCycle(long chatId) {
        log.info("Начат новый цикл для пользователя с chatId {}", chatId);
        sendMessageWithKeyboard(chatId, "Функционал разрабатывается: Начать новый цикл.", createMenuKeyboard());
    }

    private void sendMessage(long chatId, String text) {
        sendMessageWithKeyboard(chatId, text, null);
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboard keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        if (keyboardMarkup != null) {
            message.setReplyMarkup(keyboardMarkup);
        }
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке сообщения пользователю с chatId {}: {}", chatId, e.getMessage(), e);
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
