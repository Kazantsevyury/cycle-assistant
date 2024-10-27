package com.example.menstrualcyclebot.presentation;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.StatisticsService;
import com.example.menstrualcyclebot.service.UserEditService;
import com.example.menstrualcyclebot.service.dbservices.*;
import com.example.menstrualcyclebot.state.*;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.UserUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.menstrualcyclebot.utils.UIUtils.createMenuKeyboard;
@Slf4j
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
    public void processUpdate(Update update) {
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

    public void handleIncomingMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        log.debug("Handling incoming message '{}' from chatId {}", messageText, chatId);

        try {
            // Проверка всех команд основного меню и сброс временных данных и состояния
            if (messageText.equals("/start") || messageText.equals("✍️ Ввести данные") || messageText.equals("👤 Настройка профиля") ||
                    messageText.equals("🔄 Новый цикл") || messageText.equals("r") || messageText.equals("📆 Календарь") ||
                    messageText.equals("📅 Текущий день цикла") || messageText.equals("Удалить базу")) {

                // Сбрасываем временные данные
                partialCycleData.remove(chatId);
                log.info("Cleared temporary data for chatId {}", chatId);

                // Сбрасываем состояние
                changeUserState(chatId, new NoneState());
                log.info("Reset user state to NoneState for chatId {}", chatId);

                // Обрабатываем команды
                switch (messageText) {
                    case "/start":
                        if (!userService.existsById(chatId)) {
                            User newUser = UserUtils.createNewUser(update);
                            userService.save(newUser);
                            log.info("New user created and saved for chatId {}", chatId);
                        }
                        sendMessageWithKeyboard(chatId, "Добро пожаловать! Выберите команду для начала.", createMenuKeyboard());
                        break;

                    case "✍️ Ввести данные":
                        Optional<Cycle> activeCycle = cycleService.findActiveOrDelayedCycleByChatId(chatId);
                        if (activeCycle.isPresent()) {
                            Cycle existingCycle = activeCycle.get();
                            String cycleInfo = String.format(
                                    "У вас уже есть активный или задержанный цикл:\nНачало цикла: %s\nДлительность цикла: %d дней\nДлительность менструации: %d дней",
                                    existingCycle.getStartDate(),
                                    existingCycle.getCycleLength(),
                                    existingCycle.getPeriodLength()
                            );
                            sendMessage(chatId, cycleInfo);
                            log.info("Existing cycle information sent to chatId {}", chatId);
                        } else {
                            changeUserState(chatId, new AwaitingStartDateState());
                            sendMessage(chatId, "Пожалуйста, введите дату начала вашего последнего цикла:");
                            log.info("AwaitingStartDateState set for chatId {}", chatId);
                        }
                        break;

                    case "👤 Настройка профиля":
                        handleProfileSettings(chatId);
                        log.info("Handled profile settings for chatId {}", chatId);
                        break;

                    case "🔄 Новый цикл":
                        handleNewCycle(chatId);
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
                        log.info("Deleted all data for chatId {}", chatId);
                        break;

                    default:
                        sendMessageWithKeyboard(chatId, "Неизвестная команда. Пожалуйста, выберите опцию из меню.", createMenuKeyboard());
                        log.warn("Unknown command '{}' received from chatId {}", messageText, chatId);
                        break;
                }
                return;
            }

            // Обработка логики состояния пользователя
            UserStateHandler currentState = userStates.get(chatId);
            if (currentState != null && !(currentState instanceof NoneState)) {
                Cycle cycle = partialCycleData.getOrDefault(chatId, new Cycle());
                currentState.handleState(this, update, cycle);
                log.debug("State '{}' handled for chatId {}", currentState.getClass().getSimpleName(), chatId);

                if (cycle.getStartDate() != null && cycle.getCycleLength() != 0 && cycle.getPeriodLength() != 0) {
                    User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));
                    cycle.setUser(user);
                    CycleCalculator.calculateCycleFields(cycle);
                    cycle.setStatus(CycleStatus.ACTIVE);

                    cycleService.save(cycle);
                    partialCycleData.remove(chatId);
                    sendMessage(chatId, "Ваш цикл успешно сохранен!");
                    log.info("Cycle saved successfully for chatId {}", chatId);
                }

                partialCycleData.put(chatId, cycle);
                return;
            }

        } catch (Exception e) {
            log.error("Exception while handling incoming message '{}' from chatId {}", messageText, chatId, e);
            sendMessageWithKeyboard(chatId, "Произошла ошибка. Попробуйте снова позже.", createMenuKeyboard());
        }
    }

    public void handleNewCycle(long chatId) {
        log.debug("Handling new cycle creation for chatId {}", chatId);

        Optional<Cycle> optionalCurrentCycle = cycleService.findActiveOrDelayedCycleByChatId(chatId);

        Cycle currentCycle = null;

        if (optionalCurrentCycle.isPresent()) {
            currentCycle = optionalCurrentCycle.get();
            int originalCycleLength = currentCycle.getCycleLength();

            currentCycle.setStatus(CycleStatus.COMPLETED);
            currentCycle.setEndDate(LocalDate.now());
            log.info("Completed current cycle with id {} for chatId {}", currentCycle.getCycleId(), chatId);

            cycleCalculator.recalculateCycleFieldsBasedOnEndDate(currentCycle);
            cycleService.save(currentCycle);
            log.info("Recalculated and saved completed cycle for chatId {}", chatId);

            Cycle newCycle = new Cycle();
            newCycle.setUser(currentCycle.getUser());
            newCycle.setStartDate(LocalDate.now());
            newCycle.setCycleLength(originalCycleLength);
            newCycle.setPeriodLength(currentCycle.getPeriodLength());
            newCycle.setStatus(CycleStatus.ACTIVE);

            cycleCalculator.calculateCycleFields(newCycle);
            cycleService.save(newCycle);
            log.info("New cycle created and saved with id {} for chatId {}", newCycle.getCycleId(), chatId);

            sendMessageWithKeyboard(chatId, "Новый цикл успешно создан.", createMenuKeyboard());
            sendUndoMessage(chatId, newCycle.getCycleId());
            log.info("Undo message sent for new cycle with id {} for chatId {}", newCycle.getCycleId(), chatId);
        } else {
            sendMessage(chatId, "Не найден активный или задержанный цикл для создания нового.");
            log.warn("No active or delayed cycle found for new cycle creation in chatId {}", chatId);
        }
    }


    // Новый метод для отправки сообщения с кнопкой "Тут"
    public void sendUndoMessage(long chatId, Long cycleId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Ваш цикл успешно обновлен, если вы ошиблись, то нажмите [Тут]");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton undoButton = new InlineKeyboardButton();
        undoButton.setText("Тут");
        undoButton.setCallbackData("undo_cycle_" + cycleId); // callbackData включает ID цикла для удаления

        row.add(undoButton);
        buttons.add(row);
        markup.setKeyboard(buttons);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }





    public void handleCurrentDay(long chatId) {
        try {
            int currentDay = userCycleManagementService.getCurrentDay(chatId);
            sendMessageWithKeyboard(chatId, "Сегодня: " + currentDay + " день цикла.", createMenuKeyboard());

        } catch (IllegalArgumentException e) {
            sendMessageWithKeyboard(chatId, e.getMessage(), createMenuKeyboard());
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

        log.debug("Handling callback with data '{}' from chatId {}", callbackData, chatId);

        try {
            // Обработка отмены цикла
            if (callbackData.startsWith("undo_cycle_")) {
                Long cycleId = Long.parseLong(callbackData.split("_")[2]);
                Optional<Cycle> cycleOptional = cycleService.findById(cycleId);

                if (cycleOptional.isPresent()) {
                    cycleService.deleteCycleById(cycleId);
                    sendMessageWithKeyboard(chatId, "Цикл успешно отменен.", createMenuKeyboard());
                    log.info("Cycle with id {} successfully canceled for chatId {}", cycleId, chatId);
                } else {
                    sendMessage(chatId, "Ошибка: Цикл не найден.");
                    log.warn("Cycle with id {} not found for cancellation in chatId {}", cycleId, chatId);
                }
                return;
            }

            // Обработка других команд
            switch (callbackData) {
                case "new_cycle":
                    handleNewCycle(chatId);
                    log.info("New cycle creation initiated for chatId {}", chatId);
                    break;

                case "edit_salutation":
                    userStates.put(chatId, new AwaitingSalutationState());
                    sendMessage(chatId, "Введите новое обращение:");
                    log.info("Set state to AwaitingSalutationState for chatId {}", chatId);
                    break;

                case "edit_birth_date":
                    userStates.put(chatId, new AwaitingBirthdateState());
                    sendMessage(chatId, "Введите дату рождения (в формате ГГГГ-ММ-ДД):");
                    log.info("Set state to AwaitingBirthdateState for chatId {}", chatId);
                    break;

                case "edit_time_zone":
                    userStates.put(chatId, new AwaitingTimezoneState());
                    sendMessage(chatId, "Введите смещение часового пояса (например, +3 для Москвы):");
                    log.info("Set state to AwaitingTimezoneState for chatId {}", chatId);
                    break;

                case "back_to_main_menu":
                    sendMessageWithKeyboard(chatId, "Главное меню:", createMenuKeyboard());
                    userStates.put(chatId, new NoneState());
                    log.info("Returned to main menu and reset state for chatId {}", chatId);
                    break;

                default:
                    sendMessage(chatId, "Произошла ошибка. Попробуйте снова.");
                    log.warn("Received unknown callback data '{}' from chatId {}", callbackData, chatId);
                    break;
            }
        } catch (Exception e) {
            log.error("Exception while handling callback '{}' from chatId {}", callbackData, chatId, e);
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
    public void handleRecalculationCommand(long chatId) {
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
