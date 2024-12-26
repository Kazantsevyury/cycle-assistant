// UserEditService.java
package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.menstrualcyclebot.utils.BotTextConstants.BACK_TO_NOTIFICATION_SETTING;
import static com.example.menstrualcyclebot.utils.BotTextConstants.COMMAND_NOTIFICATIONS_SETTINGS;

@Service
@AllArgsConstructor
public class UserEditService {

    private final UserService userService;

    // Метод для отображения меню редактирования профиля
    public InlineKeyboardMarkup getUserEditor(long chatId) {
        Optional<User> userOptional = userService.findById(chatId);
        User user = userOptional.orElse(new User()); // Если пользователь не найден, создаем пустого

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Обращение
        List<InlineKeyboardButton> salutationRow = new ArrayList<>();
        // "Неактивная" кнопка, которая фактически ничего не делает
        salutationRow.add(createInactiveButton("Обращение:"));

        // Если у пользователя есть обращение, то выводим его, иначе предлагаем ввести
        String salutationText = (user.getSalutation() != null && !user.getSalutation().isEmpty())
                ? user.getSalutation()
                : "Ввести обращение";
        salutationRow.add(createButton(salutationText, "edit_salutation"));

        keyboard.add(salutationRow);

        // День рождения
        List<InlineKeyboardButton> birthDateRow = new ArrayList<>();
        // "Неактивная" кнопка, которая фактически ничего не делает
        birthDateRow.add(createInactiveButton("День рождения"));

        // Если у пользователя есть дата рождения, выводим её, иначе предлагаем ввести
        String birthDateText = (user.getBirthDate() != null)
                ? user.getBirthDate().toString()
                : "Ввести дату рождения";
        birthDateRow.add(createButton(birthDateText, "edit_birth_date"));

        keyboard.add(birthDateRow);

        // Часовой пояс
        List<InlineKeyboardButton> timeZoneRow = new ArrayList<>();
        // "Неактивная" кнопка, которая фактически ничего не делает
        timeZoneRow.add(createInactiveButton("Временная зона"));

        // Если у пользователя есть часовой пояс, выводим его, иначе предлагаем ввести
        String timeZoneText = String.valueOf((user.getTimeZone()));
        timeZoneRow.add(createButton(timeZoneText, "edit_time_zone"));

        keyboard.add(timeZoneRow);

        List<InlineKeyboardButton> notificationRow = new ArrayList<>();
        notificationRow.add(createButton(COMMAND_NOTIFICATIONS_SETTINGS, COMMAND_NOTIFICATIONS_SETTINGS));
        keyboard.add(notificationRow);
        // Кнопка назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("Назад", "back_to_main_menu"));

        keyboard.add(backRow);
        inlineKeyboardMarkup.setKeyboard(keyboard); // Устанавливаем клавиатуру
        return inlineKeyboardMarkup;
    }

    // Метод для обновления обращения
    public void updateSalutation(long chatId, String newSalutation) {
        Optional<User> userOptional = userService.findById(chatId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setSalutation(newSalutation);

            try {
                userService.save(user); // Сохраняем изменения
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сохранении обращения", e);
            }
        } else {
            throw new RuntimeException("Пользователь не найден");
        }
    }

    // Метод для обновления даты рождения
    public void updateBirthdate(long chatId, LocalDate birthDate) {
        Optional<User> userOptional = userService.findById(chatId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            user.setBirthDate(birthDate);

            try {
                userService.save(user); // Сохраняем изменения
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сохранении даты рождения", e);
            }
        } else {
            throw new RuntimeException("Пользователь не найден");
        }
    }

    // Метод для обновления часового пояса
    public void updateTimezone(long chatId, int timezoneOffset) {
        Optional<User> userOptional = userService.findById(chatId);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            ZoneId zoneId = ZoneId.ofOffset("UTC", ZoneOffset.ofHours(timezoneOffset));
            user.setTimeZone(zoneId);

            try {
                userService.save(user); // Сохраняем изменения
            } catch (Exception e) {
                throw new RuntimeException("Ошибка при сохранении часового пояса", e);
            }
        } else {
            throw new RuntimeException("Пользователь не найден");
        }
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData); // Используем для обработки кликов
        return button;
    }

    // Метод для создания "неактивной" кнопки с заглушкой
    private InlineKeyboardButton createInactiveButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("noop"); // Присваиваем callbackData, которая ничего не делает
        return button;
    }
}
