package com.example.menstrualcyclebot.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CalendarService {

    public InlineKeyboardMarkup getCalendar(int year, int month) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // Первая строка - дни недели
        List<InlineKeyboardButton> weekDaysRow = new ArrayList<>();
        weekDaysRow.add(createButton("Пн"));
        weekDaysRow.add(createButton("Вт"));
        weekDaysRow.add(createButton("Ср"));
        weekDaysRow.add(createButton("Чт"));
        weekDaysRow.add(createButton("Пт"));
        weekDaysRow.add(createButton("Сб"));
        weekDaysRow.add(createButton("Вс"));
        keyboard.add(weekDaysRow);

        // Определяем количество дней в месяце и первый день недели
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // 1 = Пн, 7 = Вс

        // Переменная для отслеживания дня
        int currentDay = 1;

        // Заполнение дней в календаре
        for (int i = 0; i < 6; i++) {  // 6 строк - максимум для месяца
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int j = 1; j <= 7; j++) {
                if (i == 0 && j < firstDayOfWeek) {
                    // Добавляем пустые ячейки в начале месяца
                    row.add(createButton(" "));
                } else if (currentDay <= daysInMonth) {
                    // Добавляем кнопку с днем месяца и смайликом (меняется по примеру)
                    if (currentDay <= 5) {
                        row.add(createButton(currentDay + " 💧"));
                    } else if (currentDay <= 10) {
                        row.add(createButton(currentDay + " 💋"));
                    } else if (currentDay <= 20) {
                        row.add(createButton(currentDay + " 🌞"));
                    } else {
                        row.add(createButton(currentDay + " 📅"));
                    }
                    currentDay++;
                } else {
                    // Добавляем пустые ячейки в конце месяца
                    row.add(createButton(" "));
                }
            }

            keyboard.add(row);
            if (currentDay > daysInMonth) {
                break;  // Останавливаем, если все дни месяца добавлены
            }
        }

        // Добавляем кнопки "назад" и "вперед" для переключения месяца
        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        navigationRow.add(createNavigationButton("⬅️ Назад", year, month - 1));
        navigationRow.add(createNavigationButton("Вперед ➡️", year, month + 1));
        keyboard.add(navigationRow);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private InlineKeyboardButton createButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(text); // Используем для обработки кликов
        return button;
    }

    private InlineKeyboardButton createNavigationButton(String text, int year, int month) {
        // Учитываем переходы между годами
        if (month < 1) {
            month = 12;
            year -= 1;
        } else if (month > 12) {
            month = 1;
            year += 1;
        }

        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        // Передаём в callbackData текущий год и месяц, чтобы при нажатии бот знал, что обновлять
        button.setCallbackData("navigate:" + year + ":" + month);
        return button;
    }

    public InlineKeyboardMarkup handleNavigation(String callbackData) {
        // Пример обработки callbackData, которая будет приходить в формате "navigate:2024:10"
        String[] data = callbackData.split(":");
        int year = Integer.parseInt(data[1]);
        int month = Integer.parseInt(data[2]);

        // Обновляем календарь на основе переданного месяца и года
        return getCalendar(year, month);
    }
}