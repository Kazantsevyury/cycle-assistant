package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor // Lombok автоматически создаёт конструктор с final полями
public class CalendarService {

    private final UserService userService;

    public InlineKeyboardMarkup getCalendar(int year, int month, long userChatId) {
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

        // Получаем пользователя через UserService
        Optional<User> optionalUser = userService.findById(userChatId);

        // Проверяем, найден ли пользователь
        if (!optionalUser.isPresent()) {
            throw new IllegalArgumentException("Пользователь с chatId " + userChatId + " не найден");
        }

        User user = optionalUser.get();

        // Проверяем, есть ли у пользователя зарегистрированные циклы
        if (user.getCycles() == null || user.getCycles().isEmpty()) {
            throw new IllegalArgumentException("У вас нет зарегистрированных циклов.");
        }

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
                    String dayText = String.valueOf(currentDay);
                    // Если у пользователя есть циклы, находим текущий цикл
                    Cycle cycle = user.getCycles().get(0);  // Предполагаем, что хотя бы один цикл есть
                    dayText += getEmojiForDay(cycle, year, month, currentDay);
                    row.add(createButton(dayText));
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
    private String getEmojiForDay(Cycle cycle, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day); // Дата текущего дня

        // Проверяем, попадает ли текущий день в рамки цикла
        if (!date.isBefore(cycle.getStartDate())) {

            // Проверка на менструацию
            LocalDate periodEndDate = cycle.getStartDate().plusDays(cycle.getPeriodLength() - 1); // Последний день менструации
            if (!date.isAfter(periodEndDate)) {
                return " 💧"; // Менструация
            }

            // Проверка на овуляцию
            LocalDate ovulationStartDate = cycle.getStartDate().plusDays(cycle.getOvulationStartDay() - 1); // Дата начала овуляции
            LocalDate ovulationEndDate = cycle.getStartDate().plusDays(cycle.getOvulationEndDay() - 1); // Дата окончания овуляции
            if (!date.isBefore(ovulationStartDate) && !date.isAfter(ovulationEndDate)) {
                return " 💋"; // Овуляция
            }

            // Проверка на лютеиновую фазу
            LocalDate lutealPhaseStartDate = cycle.getLutealPhaseStart(); // Дата начала лютеиновой фазы
            LocalDate lutealPhaseEndDate = cycle.getLutealPhaseEnd(); // Дата окончания лютеиновой фазы
            if (!date.isBefore(lutealPhaseStartDate) && !date.isAfter(lutealPhaseEndDate)) {
                return " 🌞"; // Лютеиновая фаза
            }
        }

        // День без фазы
        return "";
    }



    private InlineKeyboardButton createButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(text); // Используем для обработки кликов
        return button;
    }

    private InlineKeyboardButton createNavigationButton(String text, int year, int month) {
        if (month < 1) {
            month = 12;
            year -= 1;
        } else if (month > 12) {
            month = 1;
            year += 1;
        }
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData("navigate:" + year + ":" + month);
        return button;
    }
}
