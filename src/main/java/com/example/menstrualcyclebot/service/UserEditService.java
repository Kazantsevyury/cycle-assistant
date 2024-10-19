package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.sbservices.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserEditService {
    private final UserService userService;

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
        salutationRow.add(createButton(salutationText));

        keyboard.add(salutationRow);

        // День рождения
        List<InlineKeyboardButton> birthDateRow = new ArrayList<>();
        // "Неактивная" кнопка, которая фактически ничего не делает
        birthDateRow.add(createInactiveButton("День рождения"));

        // Если у пользователя есть дата рождения, выводим её, иначе предлагаем ввести
        String birthDateText = (user.getBirthDate() != null)
                ? user.getBirthDate().toString()
                : "Ввести дату рождения";
        birthDateRow.add(createButton(birthDateText));

        keyboard.add(birthDateRow);

        // Кнопка назад
        List<InlineKeyboardButton> backRow = new ArrayList<>();
        backRow.add(createButton("Назад"));

        keyboard.add(backRow);
        inlineKeyboardMarkup.setKeyboard(keyboard); // Устанавливаем клавиатуру
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(text); // Используем для обработки кликов
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
