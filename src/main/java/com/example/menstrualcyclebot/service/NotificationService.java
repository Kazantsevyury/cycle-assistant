package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.recommendations.FertilityWindowRecommendations;
import com.example.menstrualcyclebot.utils.recommendations.MenstruationStartRecommendations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.menstrualcyclebot.utils.BotTextConstants.*;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserService userService;

    @Value("${telegram.bot.token}")
    private String botToken;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public NotificationService(UserService userService) {
        this.userService = userService;
    }

    public void handleNotificationMenu() {
        log.info("Handling notification menu");
    }

    public void notifyUser(Long chatId, String message) {
        String url = TELEGRAM_API_URL + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        log.info("Sending notification to chatId: {} with message: {}", chatId, message);

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notification sent successfully to chatId: {}", chatId);
            } else {
                log.error("Failed to send notification to chatId: {}. Response status: {}", chatId, response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error while sending notification to chatId: {}", chatId, e);
        }
    }

    private static String getNotificationButtonText(String notificationType, boolean isEnabled) {
        return notificationType + (isEnabled ? " ✅" : " ❌");
    }

    public EditMessageReplyMarkup createGeneralNotificationSettingsMenu(Long chatId, Integer messageId) {
        log.info("Creating notification settings menu for chatId: {}, messageId: {}", chatId, messageId);

        User user = userService.findById(chatId).orElseThrow(() -> {
            log.error("User not found for chatId: {}", chatId);
            return new IllegalArgumentException("User not found");
        });

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        buttons.add(List.of(createButton(NOTIFICATION_TYPE_GENERAL_RECOMMENDATIONS, "NO_ACTION")));

        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_PHYSICAL_ACTIVITY, user.isPhysicalActivityEnabled()), "PHYSICAL_ACTIVITY")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_NUTRITION, user.isNutritionEnabled()), "NUTRITION")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_WORK_PRODUCTIVITY, user.isWorkProductivityNotification()), "WORK_PRODUCTIVITY")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_RELATIONSHIPS_COMMUNICATION, user.isRelationshipsCommunicationNotification()), "RELATIONSHIPS_COMMUNICATION")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_CARE, user.isCareNotification()), "CARE")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_EMOTIONAL_WELLBEING, user.isEmotionalWellbeingNotification()), "EMOTIONAL_WELLBEING")));
        buttons.add(List.of(createButton(getNotificationButtonText(NOTIFICATION_TYPE_SEX, user.isSexNotification()), "SEX")));
        buttons.add(List.of(
                createButton("Время отправки", "NO_ACTION"),
                createButton(user.getTimingOfGeneralRecommendations(), "edit_timing_general")
        ));
        buttons.add(List.of(createButton(BACK_TO_NOTIFICATION_SETTING, BACK_TO_NOTIFICATION_SETTING)));

        markup.setKeyboard(buttons);
        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
    }

    public void toggleNotificationSetting(Long chatId, String settingKey) {
        log.info("Toggling notification setting: {} for chatId: {}", settingKey, chatId);

        User user = userService.findById(chatId).orElseThrow(() -> {
            log.error("User not found for chatId: {}", chatId);
            return new IllegalArgumentException("User not found");
        });

        switch (settingKey) {
            case "PHYSICAL_ACTIVITY":
                user.setPhysicalActivityEnabled(!user.isPhysicalActivityEnabled());
                break;
            case "NUTRITION":
                user.setNutritionEnabled(!user.isNutritionEnabled());
                break;
            case "WORK_PRODUCTIVITY":
                user.setWorkProductivityNotification(!user.isWorkProductivityNotification());
                break;
            case "RELATIONSHIPS_COMMUNICATION":
                user.setRelationshipsCommunicationNotification(!user.isRelationshipsCommunicationNotification());
                break;
            case "CARE":
                user.setCareNotification(!user.isCareNotification());
                break;
            case "EMOTIONAL_WELLBEING":
                user.setEmotionalWellbeingNotification(!user.isEmotionalWellbeingNotification());
                break;
            case "SEX":
                user.setSexNotification(!user.isSexNotification());
                break;
            default:
                log.error("Invalid notification setting key: {} for chatId: {}", settingKey, chatId);
                throw new IllegalArgumentException("Invalid notification type");
        }

        userService.save(user);
        log.info("Notification setting: {} updated for chatId: {}", settingKey, chatId);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        log.debug("Creating button with text: {} and callbackData: {}", text, callbackData);
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public SendMessage createMainNotificationSettingsMenu(Long chatId) {
        log.info("Creating main notification settings menu for chatId: {}", chatId);

        User user = userService.findById(chatId).orElseThrow(() -> {
            log.error("User not found for chatId: {}", chatId);
            return new IllegalArgumentException("User not found");
        });

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        buttons.add(List.of(createButton(SETTING_UP_GENERAL_RECOMMENDATIONS, SETTING_UP_GENERAL_RECOMMENDATIONS)));
        buttons.add(List.of(createButton(SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS, SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS)));
        buttons.add(List.of(createButton(SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS, SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS)));

        buttons.add(List.of(createButton(BACK_TO_USER_SETTINGS_MENU, BACK_TO_USER_SETTINGS_MENU)));

        markup.setKeyboard(buttons);
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Настройки уведомлений:")
                .replyMarkup(markup)
                .build();
    }

    public EditMessageReplyMarkup createFertilityWindowMenu(Long chatId, Integer messageId) {
        log.info("Creating fertility window menu for chatId: {}, messageId: {}", chatId, messageId);

        User user = userService.findById(chatId).orElseThrow(() -> {
            log.error("User not found for chatId: {}", chatId);
            return new IllegalArgumentException("User not found");
        });

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        String statusText = user.isMenstruationStartNotificationEnabled() ? "✅" : "❌";
        buttons.add(List.of(
                createButton("Вкл/Выкл", "NO_ACTION"),
                createButton(statusText, "toggle_fertility")
        ));

        buttons.add(List.of(
                createButton("Время отправки", "NO_ACTION"),
                createButton(user.getTimingOfMenstruationStartNotifications(), "edit_timing_fertility")
        ));

        buttons.add(List.of(
                createButton("Дней до", "NO_ACTION"),
                createButton(String.valueOf(user.getDaysBeforeMenstruationStartNotifications()), "edit_days_before_fertility")
        ));

        buttons.add(List.of(createButton(BACK_BUTTON, BACK_TO_NOTIFICATION_SETTING)));

        markup.setKeyboard(buttons);

        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
    }

    public EditMessageReplyMarkup createMenstruationWindowMenu(Long chatId, Integer messageId) {
        log.info("Creating fertility window menu for chatId: {}, messageId: {}", chatId, messageId);

        User user = userService.findById(chatId).orElseThrow(() -> {
            log.error("User not found for chatId: {}", chatId);
            return new IllegalArgumentException("User not found");
        });

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        String statusText = user.isFertilityWindowNotificationEnabled() ? "✅" : "❌";
        buttons.add(List.of(
                createButton("Вкл/Выкл", "NO_ACTION"),
                createButton(statusText, "toggle_menstruation")
        ));

        buttons.add(List.of(
                createButton("Время отправки", "NO_ACTION"),
                createButton(user.getTimingOfFertilityWindowNotifications(), "edit_timing_menstruation")
        ));

        buttons.add(List.of(
                createButton("Дней до", "NO_ACTION"),
                createButton(String.valueOf(user.getDaysBeforeFertilityWindowNotifications()), "edit_days_before_menstruation")
        ));

        buttons.add(List.of(createButton(BACK_BUTTON, BACK_TO_NOTIFICATION_SETTING)));

        markup.setKeyboard(buttons);

        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
    }
    @Scheduled(fixedRate = 600000) // Проверка каждые 10 минут
    public void scheduleNotifications() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<User> users = userService.getAllUsers(); // Получить всех пользователей
        for (User user : users) {
            // Уведомления о фертильном окне
            if (user.isFertilityWindowNotificationEnabled() &&
                    user.getTimingOfFertilityWindowNotifications() != null &&
                    now.equals(LocalTime.parse(user.getTimingOfFertilityWindowNotifications()))) {

                String fertilityMessage = generateFertilityWindowNotification(user, today);
                notifyUser(user.getChatId(), fertilityMessage);
            }

            // Уведомления о начале менструации
            if (user.isMenstruationStartNotificationEnabled() &&
                    user.getTimingOfMenstruationStartNotifications() != null &&
                    now.equals(LocalTime.parse(user.getTimingOfMenstruationStartNotifications()))) {

                String menstruationMessage = generateMenstruationStartNotification(user, today);
                notifyUser(user.getChatId(), menstruationMessage);
            }
        }
    }

    /**
     * Генерация уведомления о фертильном окне.
     */
    private String generateFertilityWindowNotification(User user, LocalDate today) {
        Cycle currentCycle = getCurrentCycle(user, today);
        if (currentCycle == null) {
            return "Нет данных о текущем цикле.";
        }

        long dayOffset = calculateDayOffset(currentCycle, today);
        return FertilityWindowRecommendations.DAYS_BEFORE_FERTILITY_WINDOW.getOrDefault((int) dayOffset,
                FertilityWindowRecommendations.FERTILITY_WINDOW_DAYS.getOrDefault((int) dayOffset,
                        "Нет рекомендаций для фертильного окна на этот день."));
    }

    /**
     * Генерация уведомления о начале менструации.
     */
    private String generateMenstruationStartNotification(User user, LocalDate today) {
        Cycle currentCycle = getCurrentCycle(user, today);
        if (currentCycle == null) {
            return "Нет данных о текущем цикле.";
        }

        long dayOffset = calculateDayOffset(currentCycle, today);
        return MenstruationStartRecommendations.DAYS_BEFORE_MENSTRUATION.getOrDefault((int) dayOffset,
                MenstruationStartRecommendations.MENSTRUATION_START_DAYS.getOrDefault((int) dayOffset,
                        "Нет рекомендаций для начала менструации на этот день."));
    }

    /**
     * Получение текущего цикла пользователя.
     */
    private Cycle getCurrentCycle(User user, LocalDate today) {
        return user.getCycles().stream()
                .filter(cycle -> !today.isBefore(cycle.getStartDate()) &&
                        (cycle.getEndDate() == null || !today.isAfter(cycle.getEndDate())))
                .findFirst()
                .orElse(null);
    }

    /**
     * Вычисление дня относительно начала цикла.
     */
    private long calculateDayOffset(Cycle cycle, LocalDate today) {
        return today.toEpochDay() - cycle.getStartDate().toEpochDay();
    }
}
