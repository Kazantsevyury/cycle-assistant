package com.example.menstrualcyclebot.utils;

import com.example.menstrualcyclebot.domain.User;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.ZoneId;
import java.time.ZoneOffset;

public class UserUtils {

    // Стандартный московский часовой пояс
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Europe/Moscow");

    public static User createNewUser(Update update) {
        long chatId = update.getMessage().getChatId();

        User newUser = new User();
        newUser.setChatId(chatId);
        newUser.setUsername(update.getMessage().getFrom().getUserName());


        // Установка имени/обращения пользователя
        if (update.getMessage().getFrom().getFirstName() != null) {
            newUser.setSalutation(update.getMessage().getFrom().getFirstName());
            newUser.setName(update.getMessage().getFrom().getFirstName());
        } else {
            newUser.setSalutation(newUser.getUsername());
        }

        // Сохраняем московский часовой пояс по умолчанию
        newUser.setTimeZone(DEFAULT_ZONE);
        return newUser;
    }
}
