package com.example.menstrualcyclebot.presentation;
import static com.example.menstrualcyclebot.utils.BotTextConstants.*;

import com.example.menstrualcyclebot.domain.CallbackType;
import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.CalendarService;
import com.example.menstrualcyclebot.service.NotificationService;
import com.example.menstrualcyclebot.service.UserEditService;
import com.example.menstrualcyclebot.service.dbservices.*;
import com.example.menstrualcyclebot.state.*;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.UserUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.menstrualcyclebot.utils.UIUtils.*;

@Slf4j
@Component
@Data
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
    private final CycleRecalculationService cycleRecalculationService;
    private final Map<Long, UserStateHandler> userStates = new HashMap<>();
    private final Map<Long, Cycle> partialCycleData = new HashMap<>();
    private final NotificationService notificationService;

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
             CycleRecalculationService cycleRecalculationService, NotificationService notificationService) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.userService = userService;
        this.cycleService = cycleService;
        this.userCycleManagementService = userCycleManagementService;
        this.calendarService = calendarService;
        this.databaseService = databaseService;
        this.cycleCalculator = cycleCalculator;
        this.userEditService = userEditService;
        this.cycleRecalculationService = cycleRecalculationService;
        this.notificationService = notificationService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery(), update);  // Передаем update вместе с callbackQuery
        }
    }
    public void handleIncomingMessage(Update update) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            if (isMainCommand(messageText)) {
                partialCycleData.remove(chatId);
                userStates.put(chatId, new NoneState());

                Map<String, Runnable> commandHandlers = new HashMap<>();

                commandHandlers.put(COMMAND_START, () -> handleStartCommand(chatId, update));
                commandHandlers.put(COMMAND_ENTER_DATA, () -> sendMessageWithKeyboard(chatId, RECOMMENDATION_REQUEST, createDataEntryChoiceKeyboard()));
                commandHandlers.put(ENTER_HISTORICAL_DATA, () -> handleHistoricalCycleData(chatId));
                commandHandlers.put(FINISH_DATA_ENTRY, () -> sendMessageWithKeyboard(chatId, HISTORICAL_CYCLE_DATA_SAVED, createMenuKeyboard()));
                commandHandlers.put(COMMAND_PROFILE_SETTINGS, () -> handleProfileSettings(chatId));
                commandHandlers.put(CURRENT_CYCLE_DATA, () -> handleActiveCycleDataEntry(chatId));
                commandHandlers.put(COMMAND_NEW_CYCLE, () -> handleNewCycle(chatId));
                commandHandlers.put(NOTIFICATION_TYPE_SEX, () -> handleRecalculationCommand(chatId));
                commandHandlers.put("r", () -> handleRecalculationCommand(chatId));
                commandHandlers.put(COMMAND_CALENDAR, () -> handleCalendar(chatId, update));
                commandHandlers.put("i", () -> handleCompletedCycles(chatId));
                commandHandlers.put(COMMAND_CURRENT_CYCLE_DAY, () -> handleCurrentDay(chatId));
                commandHandlers.put(ENTER_ANOTHER_CYCLE, () -> handleHistoricalCycleData(chatId));
                commandHandlers.put(DELETE_CYCLE, () -> promptCycleDeletion(chatId));
                commandHandlers.put("d", () -> deleteAllData(chatId));
                commandHandlers.put(BACK, () -> sendMessageWithKeyboard(chatId, SELECT_COMMAND, createMenuKeyboard()));
                commandHandlers.put(CONFIRM_DELETE_CYCLE, () -> handleDeleteCycle(chatId));

                Runnable commandHandler = commandHandlers.getOrDefault(messageText, () -> sendMessageWithKeyboard(chatId, UNKNOWN_COMMAND, createMenuKeyboard()));
                commandHandler.run();
                return;
            }

            UserStateHandler currentState = userStates.get(chatId);

            if (currentState instanceof AwaitingHistoricalCycleDataState) {
                handleHistoricalCycleState(chatId, update);
                return;
            }

            if (currentState != null && !(currentState instanceof NoneState)) {
                handleActiveCycleState(chatId, update);
                return;
            } else {
                sendMessageWithKeyboard(chatId, UNKNOWN_COMMAND, createMenuKeyboard());
            }
        } catch (Exception e) {
            sendMessageWithKeyboard(chatId, "", createMenuKeyboard());
        }
    }

    private void handleCompletedCycles(long chatId) {
        List<Cycle> cycles = userService.findUserCyclesByChatId(chatId);
        String cyclesListMessage = cycles.isEmpty() ? NO_COMPLETED_CYCLES : "Ваши завершенные циклы:\n" +
                cycles.stream()
                        .map(cycle -> String.format("• Цикл с %s по %s (%d дней)",
                                cycle.getStartDate(),
                                cycle.getEndDate(),
                                cycle.getCycleLength()))
                        .collect(Collectors.joining("\n"));
        sendMessageWithKeyboard(chatId, cyclesListMessage, createMenuKeyboard());
    }

    private void handleDeleteCycle(long chatId) {
        Optional<Cycle> activeCycle = cycleService.findActiveOrDelayedCycleByChatId(chatId);
        if (activeCycle.isPresent()) {
            cycleService.deleteCycleById(activeCycle.get().getCycleId());
            userStates.put(chatId, new NoneState());
            partialCycleData.remove(chatId);
            sendMessageWithKeyboard(chatId, CYCLE_DELETED, createMenuKeyboard());
        } else {
            sendMessage(chatId, NO_ACTIVE_CYCLE);
        }
    }

    private void handleHistoricalCycleState(long chatId, Update update) {
        Cycle historicalCycle = partialCycleData.getOrDefault(chatId, new Cycle());
        userStates.get(chatId).handleState(this, update, historicalCycle);

        if (historicalCycle.getStartDate() != null && historicalCycle.getCycleLength() != 0 && historicalCycle.getPeriodLength() != 0) {
            historicalCycle.setStatus(CycleStatus.COMPLETED);
            cycleService.saveHistoricalCycle(historicalCycle);
            partialCycleData.remove(chatId);
        }

        partialCycleData.put(chatId, historicalCycle);
    }

    private void handleActiveCycleState(long chatId, Update update) {
        Cycle cycle = partialCycleData.getOrDefault(chatId, new Cycle());
        userStates.get(chatId).handleState(this, update, cycle);

        if (cycle.getStartDate() != null && cycle.getCycleLength() != 0 && cycle.getPeriodLength() != 0) {
            User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));
            cycle.setUser(user);
            cycleCalculator.calculateCycleFields(cycle);
            cycle.setStatus(CycleStatus.ACTIVE);
            cycleService.save(cycle);
            partialCycleData.remove(chatId);
            sendMessageWithKeyboard(chatId, CYCLE_SAVED_SUCCESSFULLY, createMenuKeyboard());
        }

        partialCycleData.put(chatId, cycle);
    }


    public void promptCycleDeletion(long chatId) {
        // Получаем список завершённых циклов
        List<LocalDate> completedCycleEndDates = userCycleManagementService.findLastCompletedCycleEndDatesByChatId(chatId, 6);

        if (completedCycleEndDates.isEmpty()) {
            sendMessage(chatId, NO_COMPLETED_CYCLES_FOR_DELETION);
            return;
        }

        // Формируем сообщение с пронумерованным списком циклов
        StringBuilder message = new StringBuilder(PROMPT_CYCLE_DELETION + "\n");
        for (int i = 0; i < completedCycleEndDates.size(); i++) {
            message.append(i + 1).append(". ").append(completedCycleEndDates.get(i)).append("\n");
        }

        sendMessage(chatId, message.toString());
        changeUserState(chatId, new
                AwaitingCycleDeletionState(cycleService, completedCycleEndDates,userCycleManagementService));  // Переход в состояние ожидания номера для удаления
    }

    public void handleHistoricalCycleData(long chatId) {
        // Получаем завершённые циклы пользователя
        List<LocalDate> completedCycleEndDates = userCycleManagementService.findLastCompletedCycleEndDatesByChatId(chatId, 6);
        int completedCyclesCount = completedCycleEndDates.size();

        if (completedCyclesCount >= 6) {
            // Если уже есть 6 завершённых циклов, выводим сообщение и клавиатуру
            sendMessageWithKeyboard(chatId,
                    HISTORICAL_CYCLES_LIMIT,
                    createCycleDatesKeyboard());
        } else {
            // Если завершённых циклов меньше 6, отправляем список завершённых циклов
            StringBuilder message = new StringBuilder(YOUR_COMPLETED_CYCLES_LIST + "\n");
            for (int i = 0; i < completedCyclesCount; i++) {
                message.append(i + 1).append(". ").append(completedCycleEndDates.get(i)).append("\n");
            }

            // Отправляем пользователю сообщение с завершёнными циклами
            sendMessage(chatId, message.toString());

            // Переходим в состояние AwaitingHistoricalCycleDataState для ввода нового цикла
            changeUserState(chatId, new AwaitingHistoricalCycleDataState(cycleService, userCycleManagementService, chatId));
            sendMessageWithKeyboard(chatId, ENTER_HISTORICAL_DATA_PROMPT,createDataEntryChoiceKeyboard());
        }
    }

    private void handleStartCommand(long chatId, Update update) {
        if (!userService.existsById(chatId)) {
            User newUser = UserUtils.createNewUser(update);
            userService.save(newUser);
        }

        Optional<Cycle> activeCycle = cycleService.findActiveOrDelayedCycleByChatId(chatId);

        if (activeCycle.isEmpty()) {
            sendMessage(chatId, START_MESSAGE);
            handleActiveCycleDataEntry(chatId); // Запускаем ввод данных актуального цикла
        } else {
            sendMessageWithKeyboard(chatId, HELP_MESSAGE, createMenuKeyboard());
        }
    }


    private void handleActiveCycleDataEntry(long chatId) {
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
            sendMessageWithKeyboard(chatId,PROMPT_DELETE_CYCLE,createDeleteCycleConfirmationKeyboard());
        } else {
            changeUserState(chatId, new AwaitingStartDateState());
            sendMessage(chatId, ENTER_LAST_CYCLE_START_DATE);
        }
    }

    private boolean isMainCommand(String messageText) {
        return messageText.equals(COMMAND_START) ||
                messageText.equals(COMMAND_ENTER_DATA) ||
                messageText.equals(COMMAND_PROFILE_SETTINGS) ||
                messageText.equals(COMMAND_NEW_CYCLE) ||
                messageText.equals("r") ||
                messageText.equals(COMMAND_CALENDAR) ||
                messageText.equals(COMMAND_CURRENT_CYCLE_DAY) ||
                messageText.equals("d") ||
                messageText.equals(BACK) ||
                messageText.equals(ENTER_HISTORICAL_DATA) ||
                messageText.equals(CURRENT_CYCLE_DATA) ||
                messageText.equals("r") ||
                messageText.equals(FINISH_DATA_ENTRY) ||
                messageText.equals(DELETE_CYCLE) ||
                messageText.equals("i") ||
                messageText.equals(ENTER_ANOTHER_CYCLE) ||
                messageText.equals(CONFIRM_DELETE_CYCLE);


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

            cycleCalculator.recalculateCycleFieldsBasedOnEndDate(currentCycle);

            cycleService.save(currentCycle);

            Cycle newCycle = new Cycle();
            newCycle.setUser(currentCycle.getUser());
            newCycle.setStartDate(LocalDate.now());
            newCycle.setCycleLength(originalCycleLength);
            newCycle.setPeriodLength(currentCycle.getPeriodLength());
            newCycle.setStatus(CycleStatus.ACTIVE);

            cycleCalculator.calculateCycleFields(newCycle);
            cycleService.save(newCycle);

            sendMessageWithKeyboard(chatId, NEW_CYCLE_CREATED, createMenuKeyboard());
            sendUndoMessage(chatId, newCycle.getCycleId());
        } else {
            sendMessage(chatId, NO_ACTIVE_OR_DELAYED_CYCLE_FOUND);
        }
    }


    public void sendUndoMessage(long chatId, Long cycleId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(CYCLE_UPDATED);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton undoButton = new InlineKeyboardButton();
        undoButton.setText("Если вы ошиблись, то нажмите тут");
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
            sendMessageWithKeyboard(chatId, TODAY + currentDay + CYCLE_DAY_SUFFIX, createMenuKeyboard());

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
        sendMessageWithKeyboard(chatId, PROFILE_SETTINGS_TEXT, keyboard);
    }


    public void changeUserState(long chatId, UserStateHandler newState) {
        synchronized (userStates) {  // Блокируем доступ к userStates
            userStates.put(chatId, newState);
        }
    }
    /**
     * Обрабатывает запрос пользователя на получение календаря.
     *
     */
    public void handleCalendar(long chatId, Update update) {
        try {
            LocalDate currentDate = LocalDate.now(); // Получаем текущую дату
            int currentYear = currentDate.getYear();
            int currentMonth = currentDate.getMonthValue();

            // Отправляем календарь с текущим месяцем и годом
            sendMessageWithKeyboard(chatId, MESSAGE_BEFORE_CALENDAR, calendarService.getCalendar(currentYear, currentMonth, chatId, update));
        } catch (Exception e) {
            sendMessage(chatId, ERROR_GENERATING_CALENDAR);
        }
    }




    public void handleCallback(CallbackQuery callbackQuery, Update update) {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        log.info("Received callback: {} for chatId: {}, messageId: {}", callbackData, chatId, messageId);

        try {
            // 1) Определяем «тип» коллбэка
            CallbackType callbackType = getCallbackType(callbackData);

            // 2) В зависимости от «типа» коллбэка
            switch (callbackType) {
                case INFO:
                    handleInfoCallback(callbackQuery, update);
                    break;
                case NOTIFICATION:
                    handleNotificationCallback(callbackQuery, update);
                    break;
                case CALENDAR:
                    handleCalendarCallback(callbackQuery, update);
                    break;
                case USER_DATA:
                    handleUserDataCallback(callbackQuery, update);
                    break;
                case USER_SETTINGS:
                    handleUserSettingsCallback(callbackQuery, update);
                    break;
                default:
                    handleUnknownCallback(callbackQuery);
                    break;
            }

        } catch (Exception e) {
            log.error("Error processing callback for chatId: {}, messageId: {}, callbackData: {}",
                    chatId, messageId, callbackData, e);
        }
    }

    /**
     * Определяем к какой «категории» относится callbackData.
     */
    private CallbackType getCallbackType(String callbackData) {
        // 1) INFO
        if ("info_question_mark".equals(callbackData) || "info_question_mark_monday".equals(callbackData)) {
            return CallbackType.INFO;
        }

        // 2) NOTIFICATION
        // - Конкретная константа: SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS
        // - Вызов вашего метода isNotificationCallback(callbackData)
        // - Префикс "fertility"
        if (SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS.equals(callbackData)
                || isNotificationCallback(callbackData)
                || callbackData.startsWith("fertility")) {
            return CallbackType.NOTIFICATION;
        }

        // 3) CALENDAR
        // - "navigate:..." => переход по календарю
        // - "undo_cycle:..." => отмена цикла
        // - "new_cycle" => создание цикла
        //   В исходном коде у вас "navigate" и "undo_cycle"/"new_cycle" без двоеточий, но давайте придержимся ключа:
        String firstPart = callbackData.split(":")[0]; // например "navigate" или "undo_cycle"
        if ("navigate".equals(firstPart) || "undo_cycle".equals(firstPart) || "new_cycle".equals(firstPart)) {
            return CallbackType.CALENDAR;
        }

        // 4) USER_DATA
        // - "edit_salutation"
        // - "edit_birth_date"
        // - "edit_time_zone"
        if ("edit_salutation".equals(firstPart)
                || "edit_birth_date".equals(firstPart)
                || "edit_time_zone".equals(firstPart)) {
            return CallbackType.USER_DATA;
        }

        // 5) USER_SETTINGS
        // - COMMAND_NOTIFICATIONS_SETTINGS
        // - BACK_TO_NOTIFICATION_SETTING
        // - SETTING_UP_GENERAL_RECOMMENDATIONS
        // - BACK_TO_USER_SETTINGS_MENU
        // - "back_to_main_menu"
        // (Учитывая, что некоторые из них — целые строки, некоторые — часть префикса)
        if (COMMAND_NOTIFICATIONS_SETTINGS.equals(firstPart)
                || BACK_TO_NOTIFICATION_SETTING.equals(firstPart)
                || SETTING_UP_GENERAL_RECOMMENDATIONS.equals(callbackData)
                || BACK_TO_USER_SETTINGS_MENU.equals(callbackData)
                || "back_to_main_menu".equals(firstPart)
                || "edit_timing_general".equals(callbackData)) { //{
            return CallbackType.USER_SETTINGS;
        }

        // Если ничего не подошло — UNKNOWN
        return CallbackType.UNKNOWN;
    }

//------------------------------------------//
//      ОБРАБОТЧИКИ РАЗНЫХ ТИПОВ КОЛЛБЭКОВ  //
//------------------------------------------//

    /** INFO */
    private void handleInfoCallback(CallbackQuery callbackQuery, Update update) throws TelegramApiException {
        long chatId = callbackQuery.getMessage().getChatId();
        log.info("Handling info_question_mark callback for chatId: {}", chatId);

        // Отправляем список эмоджи с описаниями
        String emojiList = calendarService.generateEmojiList();
        sendMessage(chatId, emojiList);
    }

    /** NOTIFICATION */
    private void handleNotificationCallback(CallbackQuery callbackQuery, Update update) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        if ("edit_timing_general".equals(callbackData)) {
            log.info("Entering general timing edit mode for chatId: {}, messageId: {}", chatId, messageId);

            // ✅ Передаем messageId в AwaitingGeneralTimingState
            changeUserState(chatId, new AwaitingGeneralTimingState(chatId, messageId));

            sendMessage(chatId, "Введите время отправки уведомлений в формате HH:mm (например, 08:30):");
            return;
        }
        if ("edit_timing_fertility".equals(callbackData)) {
            log.info("Entering general timing edit mode for chatId: {}, messageId: {}", chatId, messageId);

            changeUserState(chatId, new AwaitingFertilityTimingState(chatId, messageId));

            sendMessage(chatId, "Введите время отправки уведомлений в формате HH:mm (например, 08:30):");
            return;
        }


        if (SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS.equals(callbackData)) {
            log.info("Handling SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS for chatId: {}", chatId);
            EditMessageReplyMarkup replyMarkup = notificationService.createFertilityWindowMenu(chatId, messageId);
            execute(replyMarkup);
        }
        else if (SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS.equals(callbackData)) {
            log.info("Handling SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS for chatId: {}", chatId);
            EditMessageReplyMarkup replyMarkup = notificationService.createMenstruationWindowMenu(chatId, messageId);
            execute(replyMarkup);
        }
        else if (isNotificationCallback(callbackData)) {
            log.info("Handling notification callback: {} for chatId: {}", callbackData, chatId);
            notificationService.toggleNotificationSetting(chatId, callbackData);

            EditMessageReplyMarkup editMarkup =
                    notificationService.createGeneralNotificationSettingsMenu(chatId, messageId);
            execute(editMarkup);
        }
        else if (callbackData.startsWith("fertility")) {
            log.info("Handling fertility callback: {} for chatId: {}", callbackData, chatId);
            handleFertilityCallback(callbackData, chatId);
        }
    }

    /** CALENDAR */
    private void handleCalendarCallback(CallbackQuery callbackQuery, Update update) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        String firstPart = callbackData.split(":")[0];
        switch (firstPart) {
            case "navigate": {
                String[] data = callbackData.split(":");
                int year = Integer.parseInt(data[1]);
                int month = Integer.parseInt(data[2]);

                log.info("Navigating calendar to year: {}, month: {} for chatId: {}", year, month, chatId);

                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(String.valueOf(chatId));
                editMessage.setMessageId(messageId);
                editMessage.setText(MESSAGE_BEFORE_CALENDAR);
                editMessage.setReplyMarkup(calendarService.getCalendar(year, month, chatId, update));
                execute(editMessage);
                break;
            }
            case "undo_cycle": {
                Long cycleId = Long.parseLong(callbackData.split("_")[2]);
                log.info("Undoing cycle with id: {} for chatId: {}", cycleId, chatId);

                Optional<Cycle> cycleOptional = cycleService.findById(cycleId);
                if (cycleOptional.isPresent()) {
                    cycleService.deleteCycleById(cycleId);
                    sendMessageWithKeyboard(chatId, CYCLE_CANCELLED_SUCCESSFULLY, createMenuKeyboard());
                    log.info("Cycle with id: {} successfully canceled for chatId: {}", cycleId, chatId);
                } else {
                    sendMessage(chatId, ERROR_CYCLE_NOT_FOUND);
                    log.warn("Cycle with id: {} not found for cancellation in chatId: {}", cycleId, chatId);
                }
                break;
            }
            case "new_cycle": {
                log.info("Handling new cycle creation for chatId: {}", chatId);
                handleNewCycle(chatId);
                break;
            }
            default:
                log.warn("Unknown calendar callback: {} for chatId: {}", callbackData, chatId);
                break;
        }
    }

    /** USER_DATA */
    private void handleUserDataCallback(CallbackQuery callbackQuery, Update update) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        String firstPart = callbackData.split(":")[0];

        switch (firstPart) {
            case "edit_salutation": {
                log.info("Entering salutation edit mode for chatId: {}", chatId);
                userStates.put(chatId, new AwaitingSalutationState());
                sendMessage(chatId, "Введите новое обращение:");
                break;
            }
            case "edit_birth_date": {
                log.info("Entering birth date edit mode for chatId: {}", chatId);
                userStates.put(chatId, new AwaitingBirthdateState());
                sendMessage(chatId, ENTER_DATE_FORMAT_DD_MM_YYYY);
                break;
            }
            case "edit_time_zone": {
                log.info("Entering time zone edit mode for chatId: {}", chatId);
                userStates.put(chatId, new AwaitingTimezoneState());
                sendMessage(chatId, ENTER_TIMEZONE_OFFSET);
                break;
            }
            default:
                log.warn("Unknown user data callback: {} for chatId: {}", callbackData, chatId);
                break;
        }
    }

    /** USER_SETTINGS */
    private void handleUserSettingsCallback(CallbackQuery callbackQuery, Update update) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        // Можно также сделать switch или if-else по callbackData
        if (SETTING_UP_GENERAL_RECOMMENDATIONS.equals(callbackData)) {
            log.info("Handling general recommendations settings for chatId: {}", chatId);
            EditMessageReplyMarkup editMessageReplyMarkup =
                    notificationService.createGeneralNotificationSettingsMenu(chatId, messageId);
            execute(editMessageReplyMarkup);
        }
        else if (BACK_TO_USER_SETTINGS_MENU.equals(callbackData)) {
            log.info("Navigating back to user settings menu for chatId: {}", chatId);
            handleProfileSettings(chatId);
        }
        else if ("edit_timing_general".equals(callbackData)) {
            log.info("Entering general timing edit mode for chatId: {}", chatId);
            changeUserState(chatId, new AwaitingGeneralTimingState(chatId, messageId));
            sendMessage(chatId, "Введите время отправки уведомлений в формате HH:mm (например, 08:30):");
        }
        else if ("edit_timing_fertility".equals(callbackData)) {
            log.info("Entering general timing edit mode for chatId: {}", chatId);
            changeUserState(chatId, new AwaitingFertilityTimingState(chatId, messageId));
            sendMessage(chatId, "Введите время отправки уведомлений в формате HH:mm (например, 08:30):");
        }


        else if ("back_to_main_menu".equals(callbackData.split(":")[0])) {
            log.info("Navigating back to main menu for chatId: {}", chatId);
            sendMessageWithKeyboard(chatId, MAIN_MENU, createMenuKeyboard());
            userStates.put(chatId, new NoneState());
        }
        else {
            // В исходном коде командой для открытия настроек уведомлений
            // были COMMAND_NOTIFICATIONS_SETTINGS и BACK_TO_NOTIFICATION_SETTING
            String firstPart = callbackData.split(":")[0];
            if (COMMAND_NOTIFICATIONS_SETTINGS.equals(firstPart)
                    || BACK_TO_NOTIFICATION_SETTING.equals(firstPart)) {
                log.info("Navigating to notification settings for chatId: {}", chatId);
                SendMessage notificationMenu = notificationService.createMainNotificationSettingsMenu(chatId);
                execute(notificationMenu);
            } else {
                log.warn("Unknown user settings callback: {} for chatId: {}", callbackData, chatId);
            }
        }
    }

    /** UNKNOWN */
    private void handleUnknownCallback(CallbackQuery callbackQuery) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        long chatId = callbackQuery.getMessage().getChatId();

        log.warn("Unknown callback received: {} for chatId: {}", callbackData, chatId);
        // При желании можно отправить пользователю сообщение об ошибке
        // sendMessage(chatId, "Неизвестная команда. Попробуйте снова.");
    }


    private void handleFertilityCallback(String callbackData, Long chatId) {
        log.info("Processing fertility callback: {} for chatId: {}", callbackData, chatId);

        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        switch (callbackData) {
            case "toggle_fertility":
                boolean newStatus = !user.isFertilityWindowNotificationEnabled();
                user.setFertilityWindowNotificationEnabled(newStatus);
                userService.save(user);
                log.info("Fertility notification toggled to: {} for chatId: {}", newStatus, chatId);
                sendMessage(chatId, "Настройки уведомлений обновлены!");
                break;

            case "edit_timing_fertility":
                log.info("Entering timing edit mode for fertility notifications for chatId: {}", chatId);
                sendMessage(chatId, "Введите время уведомлений (например, 08:00):");
                break;

            case "edit_days_before_fertility":
                log.info("Entering days before fertility window edit mode for chatId: {}", chatId);
                sendMessage(chatId, "Введите количество дней до окна фертильности:");
                break;

            default:
                log.warn("Unknown fertility callback received: {} for chatId: {}", callbackData, chatId);
                sendMessage(chatId, "Неизвестное действие.");
                break;
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

    private boolean isNotificationCallback(String callbackData) {
        return callbackData.equals("PHYSICAL_ACTIVITY")
                || callbackData.equals("NUTRITION")
                || callbackData.equals("WORK_PRODUCTIVITY")
                || callbackData.equals("RELATIONSHIPS_COMMUNICATION")
                || callbackData.equals("CARE")
                || callbackData.equals("EMOTIONAL_WELLBEING")
                || callbackData.equals("SEX")
                || callbackData.equals("toggle_fertility")
                || callbackData.equals("edit_days_before_fertility")
                || callbackData.equals(SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS)
                || callbackData.equals("toggle_fertility")
                || callbackData.equals("edit_timing_fertility")
                || callbackData.equals("edit_days_before_fertility");

    }
}
