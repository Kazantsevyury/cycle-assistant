package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

import static com.example.menstrualcyclebot.utils.BotTextConstants.*;

@Service
public class NotificationService {
    private final UserService userService;
    @Value("${telegram.bot.token}")
    private String botToken;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public NotificationService(UserService userService) {
        this.userService = userService;
    }

    public void handleNotificationMenu() {

    }

    public void notifyUser(Long chatId, String message) {
        String url = TELEGRAM_API_URL + botToken + "/sendMessage?chat_id=" + chatId + "&text=" + message;

        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Notification sent successfully to chat ID: " + chatId);
            } else {
                System.err.println("Failed to send notification to chat ID: " + chatId);
            }
        } catch (Exception e) {
            System.err.println("Error while sending notification: " + e.getMessage());
        }
    }

    private static String getNotificationButtonText(String notificationType, boolean isEnabled) {
        return notificationType + (isEnabled ? " ✅" : " ❌");
    }


    public EditMessageReplyMarkup createNotificationSettingsMenu(Long chatId, Integer messageId) {
        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

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

        buttons.add(List.of(createButton(BACK_TO_NOTIFICATION_SETTING, BACK_TO_NOTIFICATION_SETTING)));

        markup.setKeyboard(buttons);
        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
    }

    public void toggleNotificationSetting(Long chatId, String settingKey) {
        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

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
                throw new IllegalArgumentException("Invalid notification type");
        }

        userService.save(user);
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }

    public SendMessage createMainNotificationSettingsMenu(Long chatId) {
        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();


        buttons.add(List.of(createButton(SETTING_UP_GENERAL_RECOMMENDATIONS,SETTING_UP_GENERAL_RECOMMENDATIONS)));
        buttons.add(List.of(createButton(SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS ,SETTING_UP_FERTILE_WINDOW_RECOMMENDATIONS )));
        buttons.add(List.of(createButton(SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS ,SETTING_UP_CYCLE_DELAY_RECOMMENDATIONS )));

        buttons.add(List.of(createButton(BACK_TO_USER_SETTINGS_MENU, BACK_TO_USER_SETTINGS_MENU)));

        markup.setKeyboard(buttons);
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Настройки уведомлений:")
                .replyMarkup(markup)
                .build();
    }
    public SendMessage createNotificationSettingsMenu(Long chatId) {
        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

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

        buttons.add(List.of(createButton(BACK_BUTTON, "BACK")));

        markup.setKeyboard(buttons);
        return SendMessage.builder()
                .chatId(String.valueOf(chatId))
                .text("Настройки уведомлений:")
                .replyMarkup(markup)
                .build();
    }

    public EditMessageReplyMarkup createFertilityNotificationSettingsMenu(Long chatId, Integer messageId, String notificationType) {
        User user = userService.findById(chatId).orElseThrow(() -> new IllegalArgumentException("User not found"));

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();

        // 1st Row: Notification On/Off Button
        String notificationStatus = user.isFertilityWindowNotification() ? "✅" : "❌";
        buttons.add(List.of(createButton("Уведомления " + notificationStatus, notificationType + ":toggle")));

        // 2nd Row: Timing of Scheduled Notifications Button and Input Data Button
        buttons.add(List.of(
                createButton("Timing of scheduled notifications", notificationType + ":timing"),
                createButton(user.getTimingOfFertilityWindowNotifications() + ":" + user.getTimingOfFertilityWindowNotifications(), notificationType + ":set_timing")
        ));

        // 3rd Row: Fertility Window Notification Days Button and Default Value Button
        buttons.add(List.of(
                createButton("How many days before Fertility window", notificationType + ":fertility_days"),
                createButton(String.valueOf(user.getDaysBeforeFertilityWindowNotifications()), notificationType + ":set_fertility_days")
        ));

        // 4th Row: Back Button
        buttons.add(List.of(createButton(BACK_BUTTON, BACK_TO_NOTIFICATION_SETTING)));

        markup.setKeyboard(buttons);
        return EditMessageReplyMarkup.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .replyMarkup(markup)
                .build();
    }


}
