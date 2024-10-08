package com.example.menstrualcyclebot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class UIUtils {

    public static ReplyKeyboardMarkup createMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Получить совет на день"));
        row1.add(new KeyboardButton("Статистика"));
        row1.add(new KeyboardButton("Текущий день цикла"));


        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Настройка профиля"));
        row2.add(new KeyboardButton("Настроить уведомления"));
        row2.add(new KeyboardButton("Календарь"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Новый цикл"));


        keyboard.add(row1);

        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true); // Автоматически подгоняет размер кнопок под экран пользователя
        keyboardMarkup.setOneTimeKeyboard(false); // Меню будет оставаться после использования
        return keyboardMarkup;
    }
}
