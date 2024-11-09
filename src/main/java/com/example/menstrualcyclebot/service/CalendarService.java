package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.format.TextStyle;
import java.util.*;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final UserService userService;



    public InlineKeyboardMarkup getCalendar(int year, int month, long userChatId, Update update) {
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
        User user;

        // Если пользователь не найден, создаем нового пользователя
        if (!optionalUser.isPresent()) {
            user = UserUtils.createNewUser(update);  // Используем Update для создания пользователя
            userService.save(user);
        } else {
            user = optionalUser.get();
        }

        // Сортируем циклы по дате начала в порядке убывания (более новые в начале)
        List<Cycle> sortedCycles = new ArrayList<>(user.getCycles());
        sortedCycles.sort(Comparator.comparing(Cycle::getStartDate).reversed());

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

                    // Если у пользователя есть циклы, добавляем эмодзи только из первого подходящего цикла
                    for (Cycle cycle : sortedCycles) {
                        String emoji = getEmojiForDay(cycle, year, month, currentDay);
                        if (!emoji.isEmpty()) {
                            dayText += emoji;
                            break; // Выходим из цикла, как только нашли эмодзи для самого нового цикла
                        }
                    }

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

        // Текущая дата
        LocalDate today = LocalDate.now();
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"));
        String todayText = String.format("Сегодня: %d, %s", today.getDayOfMonth(), monthName);

        // Добавляем строку с текущей датой
        List<InlineKeyboardButton> todayRow = new ArrayList<>();
        todayRow.add(createButton(todayText));
        keyboard.add(todayRow);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private String getEmojiForDay(Cycle cycle, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day); // Дата текущего дня

        // Проверяем, попадает ли текущий день в рамки цикла
        if (cycle.getStartDate() != null && !date.isBefore(cycle.getStartDate())) {

            // Проверка на менструацию
            LocalDate periodEndDate = cycle.getStartDate().plusDays(cycle.getPeriodLength() - 1);
            if (!date.isAfter(periodEndDate)) {
                return " 💧"; // Менструация
            }

            // Проверка на фолликулярную фазу
            if (cycle.getFollicularPhaseStart() != null && cycle.getOvulationDate() != null &&
                    !date.isBefore(cycle.getFollicularPhaseStart()) && date.isBefore(cycle.getOvulationDate())) {
                return " 👄"; // Фолликулярная фаза
            }

            // Проверка на фертильное окно
            // Проверка на фертильное окно
            if (cycle.getFertileWindowStartDay() != null && cycle.getFertileWindowEndDay() != null) {
                LocalDate fertileWindowStartDate = cycle.getStartDate().plusDays(cycle.getFertileWindowStartDay() - 1);
                LocalDate fertileWindowEndDate = cycle.getStartDate().plusDays(cycle.getFertileWindowEndDay() - 1);
                if (!date.isBefore(fertileWindowStartDate) && !date.isAfter(fertileWindowEndDate)) {
                    return " 🌕"; // Фертильное окно
                }
            }

            // Проверка на день овуляции
            if (cycle.getOvulationDate() != null && date.isEqual(cycle.getOvulationDate())) {
                return " 🌷"; // День овуляции
            }

            // Проверка на лютеиновую фазу
            if (cycle.getLutealPhaseStart() != null && cycle.getLutealPhaseEnd() != null &&
                    !date.isBefore(cycle.getLutealPhaseStart()) && !date.isAfter(cycle.getLutealPhaseEnd())) {
                return " 🫂"; // Лютеиновая фаза
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
