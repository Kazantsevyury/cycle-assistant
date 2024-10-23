package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.StatisticsService;
import com.example.menstrualcyclebot.service.UserEditService;
import com.example.menstrualcyclebot.service.dbservices.*;
import com.example.menstrualcyclebot.state.*;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.UserUtils;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
    public final UserEditService userEditService;
    private final StatisticsService statisticsService;
    private final CycleRecalculationService cycleRecalculationService; // Добавляем пересчёт циклов


    private final ExecutorService executorService = Executors.newFixedThreadPool(10); // Настрой количество потоков

    private final Map<Long, UserStateHandler> userStates = new HashMap<>();
    private final Map<Long, Cycle> partialCycleData = new HashMap<>();

    /**
     * Конструктор для создания бота с необходимыми зависимостями.
     *
     * @param botToken Токен бота для Telegram API.
     * @param botUsername Имя пользователя бота в Telegram.
     * @param userService Сервис для управления пользователями.
     * @param cycleService Сервис для управления циклами.
     * @param userCycleManagementService Сервис для управления циклами пользователей.
     * @param calendarService Сервис для работы с календарем.
     * @param databaseService Сервис для работы с базой данных.
     * @param cycleCalculator Утилита для вычисления циклов.
     * @param userEditService Сервис для редактирования данных пользователей.
     * @param statisticsService Сервис для генерации статистики.
     */
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
            StatisticsService statisticsService, CycleRecalculationService cycleRecalculationService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;
        this.cycleService = cycleService;
        this.userCycleManagementService = userCycleManagementService;
        this.calendarService = calendarService;
        this.databaseService = databaseService;
        this.cycleCalculator = cycleCalculator;
        this.userEditService = userEditService;
        this.statisticsService = statisticsService;
        this.cycleRecalculationService = cycleRecalculationService;
    }

    /**
     * Обрабатывает входящее обновление от пользователя.
     *
     * @param update Объект обновления, содержащий информацию от пользователя.
     */
    private void processUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
        }
    }

    /**
     * Метод, вызываемый при получении обновления от Telegram API.
     * Запускает обработку каждого обновления в отдельном потоке.
     *
     * @param update Объект обновления от пользователя.
     */
    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> processUpdate(update)); // Обработка каждого апдейта в отдельном потоке
    }

    /**
     * Обрабатывает входящее текстовое сообщение от пользователя.
     *
     * @param update Объект обновления, содержащий сообщение от пользователя.
     */
    public void handleIncomingMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            UserStateHandler currentState = userStates.get(chatId);

            if (currentState != null && !(currentState instanceof NoneState)) {
                Cycle cycle = partialCycleData.getOrDefault(chatId, new Cycle());
                currentState.handleState(this, update, cycle);

                // Проверка, если все данные для цикла введены
                if (cycle.getStartDate() != null && cycle.getCycleLength() != 0 && cycle.getPeriodLength() != 0) {
                    User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));
                    cycle.setUser(user); // Привязываем цикл к пользователю
                    CycleCalculator.calculateCycleFields(cycle);

                    cycleService.save(cycle); // Сохранение в базу данных
                    partialCycleData.remove(chatId); // Удаление временных данных о цикле

                    sendMessage(chatId, "Ваш цикл успешно сохранен!");
                }

                partialCycleData.put(chatId, cycle);
                return;
            }

            String salutation = getUserSalutation(chatId);

                // Обработка команд
                switch (messageText) {
                    case "/start":
                        if (!userService.existsById(chatId)) {
                            User newUser = UserUtils.createNewUser(update);
                            userService.save(newUser);
                        }
                        sendMessageWithKeyboard(chatId, "Добро пожаловать, " + salutation + "! Выберите команду для начала.", createMenuKeyboard());
                        break;

                    case "✍️ Ввести данные":
                        changeUserState(chatId, new AwaitingStartDateState());
                        sendMessage(chatId, "Пожалуйста, введите дату начала вашего последнего цикла (в формате ГГГГ-ММ-ДД):");
                        break;

                    case "👤 Настройка профиля":
                        handleProfileSettings(chatId);
                        break;

                    case "r":
                        handleRecalculationCommand(chatId);
                        break;
                    case "📆 Календарь":
                        handleCalendar(chatId);
                        break;
                    case "📅 Текущий день цикла":
                        handleCurrentDay(chatId);
                        break;
                    case "Удалить базу":
                        deleteAllData(chatId);
                        break;
                    default:
                        sendMessage(chatId, "Неизвестная команда, " + salutation + ". Пожалуйста, выберите опцию из меню.");
                }
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
    }

    public String getUserSalutation(long chatId) {
        Optional<User> optionalUser = userService.findById(chatId);
        if (optionalUser.isEmpty()) {
            return "";
        }
        User user = optionalUser.get();
        return user.getSalutation() != null ? user.getSalutation() : user.getName();
    }

    public void handleCurrentDay(long chatId) {
        try {
            int currentDay = userCycleManagementService.getCurrentDay(chatId);
            sendMessageWithKeyboard(chatId, "Сегодня: " + currentDay + " день цикла.", createMenuKeyboard());

        } catch (IllegalArgumentException e) {
            sendMessageWithKeyboard(chatId, e.getMessage(), createMenuKeyboard());
        }
    }


    /**
     * Обрабатывает запрос пользователя на настройку профиля.
     *
     * @param chatId Идентификатор чата, в котором нужно отправить клавиатуру настроек профиля.
     */
    public void handleProfileSettings(long chatId) {
        // Получаем клавиатуру для настройки профиля
        InlineKeyboardMarkup keyboard = userEditService.getUserEditor(chatId);
        // Отправляем клавиатуру пользователю
        sendMessageWithKeyboard(chatId, "Настройки профиля: ", keyboard);
    }


    public void changeUserState(long chatId, UserStateHandler newState) {
        userStates.put(chatId, newState);
    }


    /**
     * Обрабатывает запрос пользователя на получение календаря.
     *
     * @param chatId Идентификатор чата, в котором нужно отправить календарь.
     */
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
            switch (callbackData) {
                case "edit_salutation":
                    // Устанавливаем состояние ожидания нового обращения
                    userStates.put(chatId, new AwaitingSalutationState());
                    sendMessage(chatId, "Введите новое обращение:");
                    break;

                case "edit_birth_date":
                    // Устанавливаем состояние ожидания новой даты рождения
                    userStates.put(chatId, new AwaitingBirthdateState());
                    sendMessage(chatId, "Введите дату рождения (в формате ГГГГ-ММ-ДД):");
                    break;

                case "edit_time_zone":
                    // Устанавливаем состояние ожидания часового пояса
                    userStates.put(chatId, new AwaitingTimezoneState());
                    sendMessage(chatId, "Введите смещение часового пояса (например, +3 для Москвы):");
                    break;

                case "back_to_main_menu":
                    sendMessageWithKeyboard(chatId, "Главное меню:", createMenuKeyboard());
                    userStates.put(chatId, new NoneState()); // Возвращаемся в состояние "None"
                    break;

                default:
                    sendMessage(chatId, "Произошла ошибка. Попробуйте снова.");
            }
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка. Попробуйте снова позже.");
        }
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

    /**
     * Удаляет все данные из базы данных.
     *
     * @param chatId Идентификатор чата, в котором нужно отправить сообщение после удаления.
     */
    public void deleteAllData(long chatId) {
        try {
            databaseService.deleteAllData();
            sendMessage(chatId, "База стерта");
            sendMessageWithKeyboard(chatId, "Выберите опцию ниже:", createMenuKeyboard());
        } catch (Exception e) {
            sendMessage(chatId, "Произошла ошибка при удалении базы данных. Попробуйте снова позже.");
        }
    }

    /**
     * Обрабатывает команду пересчёта циклов
     */
    private void handleRecalculationCommand(long chatId) {
        try {
            cycleRecalculationService.recalculateAndUpdateCycles(); // Запуск пересчёта циклов
            sendMessage(chatId, "Пересчёт циклов успешно выполнен.");
        } catch (Exception e) {
            sendMessage(chatId, "Ошибка при пересчёте циклов: " + e.getMessage());
        }
    }

    /**
     * Возвращает имя пользователя бота.
     *
     * @return Имя пользователя бота.
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Возвращает токен бота.
     *
     * @return Токен бота.
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Выполняет завершающие операции перед завершением работы приложения, такие как завершение потоков.
     */
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
