package com.example.menstrualcyclebot.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UIUtils {

    public static ReplyKeyboardMarkup createMenuKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("💡 Получить рекомендацию"));
        row1.add(new KeyboardButton("📊 Статистика"));
        row1.add(new KeyboardButton("📅 Текущий день цикла"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("👤 Настройка профиля"));
        row2.add(new KeyboardButton("🔔 Настроить уведомления"));
        row2.add(new KeyboardButton("📆 Календарь"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("🔄 Новый цикл"));
        row3.add(new KeyboardButton("✍️ Ввести данные"));



        keyboard.add(row1);

        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true); // Автоматически подгоняет размер кнопок под экран пользователя
        keyboardMarkup.setOneTimeKeyboard(false); // Меню будет оставаться после использования
        return keyboardMarkup;
    }
    // Меню для исторического ввода данных
    public static ReplyKeyboardMarkup createHistoricalDataKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("✍️ Ввести исторические данные"));
        row.add(new KeyboardButton("Завершить ввод исторических данных"));
        keyboard.add(row);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        return keyboardMarkup;
    }
    public static ReplyKeyboardMarkup createDataEntryChoiceKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Ввести данные актуального цикла"));
        row.add(new KeyboardButton("✍️ Ввести исторические данные"));
        row.add(new KeyboardButton("Назад"));

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createCycleDatesKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Ввести еще один цикл"));
        row.add(new KeyboardButton("Закончить ввод данных"));
        row.add(new KeyboardButton("Удалить один из введенных циклов"));

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup createCycleDatesDel(List<LocalDate> endDates) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Удалить цикл"));
        row.add(new KeyboardButton("В главное меню"));

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }
    public static ReplyKeyboardMarkup createDeleteCycleConfirmationKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Да, удалить текущий цикл"));
        row.add(new KeyboardButton("Назад"));

        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        return keyboardMarkup;
    }


}
