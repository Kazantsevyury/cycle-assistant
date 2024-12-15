package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.UserUtils;
import com.example.menstrualcyclebot.utils.BotTextConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import java.time.format.TextStyle;
import java.util.*;
import java.time.LocalDate;

import static com.example.menstrualcyclebot.domain.CycleStatus.DELAYED;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final UserService userService;
    private final List<String> russianMonths = Arrays.asList(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    );

    public InlineKeyboardMarkup getCalendar(int year, int month, long userChatId, Update update) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        // Текущая дата
        LocalDate today = LocalDate.now();
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"));
        String todayText = String.format("Сегодня: %d %s", today.getDayOfMonth(), monthName);

        // Создаем строку с кнопкой текущей даты и значком вопросительного знака
        List<InlineKeyboardButton> todayRow = new ArrayList<>(); // Создаем todayRow как список
        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText(todayText);
        infoButton.setCallbackData("today_info"); // Callback для отображения информации о фазах
        todayRow.add(infoButton); // Добавляем кнопку в todayRow
        keyboard.add(todayRow); // Добавляем todayRow в основную клавиатуру

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

        // Проверяем последнюю строку календаря и добавляем значок вопроса в соответствующую клетку
        List<InlineKeyboardButton> lastRow = keyboard.get(keyboard.size() - 1);
        boolean addedQuestionMark = false;
        for (int i = lastRow.size() - 1; i >= 0; i--) {
            InlineKeyboardButton button = lastRow.get(i);
            if (button.getText().trim().isEmpty()) {
                button.setText(BotTextConstants.QUESTION_MARK_EMOJI);
                button.setCallbackData("info_question_mark");
                addedQuestionMark = true;
                break;
            }
        }


// Если не удалось добавить в последнюю строку, добавляем в первый понедельник месяца
        if (!addedQuestionMark) {
            // Найти первую строку с днями месяца
            for (List<InlineKeyboardButton> row : keyboard) {
                if (row.size() == 7 && row.stream().anyMatch(btn -> btn.getText().matches("\\d+"))) {
                    // В строке с днями найти понедельник
                    InlineKeyboardButton firstMondayButton = row.get(0); // Первый столбец - "Пн"
                    firstMondayButton.setText(firstMondayButton.getText() + " " + BotTextConstants.QUESTION_MARK_EMOJI);
                    firstMondayButton.setCallbackData("info_question_mark_monday");
                    break;
                }
            }
        }

        // Добавляем кнопки "назад", название месяца и "вперед" для переключения месяца
        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        navigationRow.add(createNavigationButton(" ⬅️ ", year, month - 1));

        // Получаем название месяца из списка месяцев на русском языке
        String monthDisplayName = russianMonths.get(month - 1);

        // Добавляем название месяца как некликабельную кнопку
        InlineKeyboardButton monthButton = new InlineKeyboardButton();
        monthButton.setText(monthDisplayName);
        monthButton.setCallbackData("noop"); // Ничего не выполняет при нажатии
        navigationRow.add(monthButton);

        navigationRow.add(createNavigationButton(" ➡️ ", year, month + 1));
        keyboard.add(navigationRow);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private String getEmojiForDay(Cycle cycle, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day); // Дата текущего дня
        LocalDate today = LocalDate.now();

        // Проверяем, попадает ли текущий день в рамки цикла
        if (cycle.getStartDate() != null && !date.isBefore(cycle.getStartDate())) {
            LocalDate cycleEndDate = cycle.getStartDate().plusDays(cycle.getCycleLength() - 1);

            // Проверка на день овуляции (всегда отображается с приоритетом)
            if (cycle.getOvulationDate() != null && date.isEqual(cycle.getOvulationDate())) {
                return BotTextConstants.OVULATION_EMOJI; // День овуляции
            }

            // Проверка на фертильное окно
            if (cycle.getFertileWindowStartDay() != null && cycle.getFertileWindowEndDay() != null) {
                LocalDate fertileWindowStartDate = cycle.getStartDate().plusDays(cycle.getFertileWindowStartDay() - 1);
                LocalDate fertileWindowEndDate = cycle.getStartDate().plusDays(cycle.getFertileWindowEndDay() - 1);
                if (!date.isBefore(fertileWindowStartDate) && !date.isAfter(fertileWindowEndDate)) {
                    return BotTextConstants.FERTILE_WINDOW_EMOJI; // Фертильное окно (обновленный эмодзи)
                }
            }

            // Проверка на задержку цикла (цикл задерживается, если текущая дата больше предполагаемой даты окончания цикла, но не позднее сегодняшнего дня)
            if (cycleEndDate != null && date.isAfter(cycleEndDate) && !date.isAfter(today)) {
                return BotTextConstants.CYCLE_DELAY_EMOJI; // Задержка цикла (обновленный эмодзи)
            }

            // Проверка на менструацию
            LocalDate periodEndDate = cycle.getStartDate().plusDays(cycle.getPeriodLength() - 1);
            if (!date.isAfter(periodEndDate)) {
                return BotTextConstants.MENSTRUATION_EMOJI; // Менструация
            }

            // Проверка на фолликулярную фазу
            if (cycle.getFollicularPhaseStart() != null && cycle.getOvulationDate() != null &&
                    !date.isBefore(cycle.getFollicularPhaseStart()) && date.isBefore(cycle.getOvulationDate())) {
                return BotTextConstants.FOLLICULAR_PHASE_EMOJI; // Фолликулярная фаза (обновленный эмодзи)
            }

            // Проверка на лютеиновую фазу
            if (cycle.getLutealPhaseStart() != null && cycle.getLutealPhaseEnd() != null &&
                    !date.isBefore(cycle.getLutealPhaseStart()) && !date.isAfter(cycle.getLutealPhaseEnd())) {
                return BotTextConstants.LUTEAL_PHASE_EMOJI; // Лютеиновая фаза (обновленный эмодзи)
            }
        }

        // Если текущий день позже сегодняшнего, планируем повторение фаз цикла на основе текущего цикла без пропусков
        if (date.isAfter(today) && cycle.getEndDate() != null && cycle.getStatus().equals(DELAYED) ) {
            return planFutureCycles(cycle, date);
        }

        // День без фазы
        return "";
    }
    private String planFutureCycles(Cycle currentCycle, LocalDate date) {
        // 1. Начальная дата для следующего цикла - следующий день после окончания текущего цикла
        int cycleLength = currentCycle.getCycleLength();
        int periodLength = currentCycle.getPeriodLength();
        LocalDate nextCycleStartDate = currentCycle.getEndDate().plusDays(1);

        // 2. Планируем до 5 будущих циклов
        for (int i = 0; i < 5; i++) {
            // Создаем "фейковый" будущий цикл
            Cycle futureCycle = new Cycle();
            futureCycle.setStartDate(nextCycleStartDate);
            futureCycle.setCycleLength(cycleLength);
            futureCycle.setPeriodLength(periodLength);

            // Рассчитываем поля будущего цикла с использованием калькулятора
            CycleCalculator.calculateCycleFields(futureCycle);

            // Проверяем, попадает ли заданная дата в текущий будущий цикл
            if (!date.isBefore(futureCycle.getStartDate()) && !date.isAfter(futureCycle.getEndDate())) {
                // Проверяем фазы будущего цикла, чтобы добавить соответствующие эмодзи

                // Проверка на день овуляции
                if (futureCycle.getOvulationDate() != null && date.isEqual(futureCycle.getOvulationDate())) {
                    return BotTextConstants.OVULATION_EMOJI; // День овуляции
                }

                // Проверка на фертильное окно
                if (futureCycle.getFertileWindowStartDay() != null && futureCycle.getFertileWindowEndDay() != null) {
                    LocalDate fertileWindowStartDate = futureCycle.getStartDate().plusDays(futureCycle.getFertileWindowStartDay() - 1);
                    LocalDate fertileWindowEndDate = futureCycle.getStartDate().plusDays(futureCycle.getFertileWindowEndDay() - 1);
                    if (!date.isBefore(fertileWindowStartDate) && !date.isAfter(fertileWindowEndDate)) {
                        return BotTextConstants.FERTILE_WINDOW_EMOJI; // Фертильное окно
                    }
                }

                // Проверка на менструацию
                LocalDate periodEndDate = futureCycle.getStartDate().plusDays(periodLength - 1);
                if (!date.isAfter(periodEndDate)) {
                    return BotTextConstants.MENSTRUATION_EMOJI; // Менструация
                }

                // Проверка на фолликулярную фазу
                if (futureCycle.getFollicularPhaseStart() != null && futureCycle.getOvulationDate() != null &&
                        !date.isBefore(futureCycle.getFollicularPhaseStart()) && date.isBefore(futureCycle.getOvulationDate())) {
                    return BotTextConstants.FOLLICULAR_PHASE_EMOJI; // Фолликулярная фаза
                }



                // Проверка на лютеиновую фазу
                if (futureCycle.getLutealPhaseStart() != null && futureCycle.getLutealPhaseEnd() != null &&
                        !date.isBefore(futureCycle.getLutealPhaseStart()) && !date.isAfter(futureCycle.getLutealPhaseEnd())) {
                    return BotTextConstants.LUTEAL_PHASE_EMOJI; // Лютеиновая фаза
                }
            }

            // Обновляем начальную дату для следующего цикла - следующий день после окончания текущего цикла
            nextCycleStartDate = futureCycle.getEndDate().plusDays(1);
        }

        // Если дата не попадает в фазу ни одного из будущих циклов, возвращаем пустую строку
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

    public String generateEmojiList() {
        return "Список обозначений фаз цикла:\n\n" +
                BotTextConstants.MENSTRUATION_EMOJI + " Менструальная фаза\n" +
                BotTextConstants.FOLLICULAR_PHASE_EMOJI + " Фолликулярная фаза\n" +
                BotTextConstants.OVULATION_EMOJI + " Фаза овуляции\n" +
                BotTextConstants.LUTEAL_PHASE_EMOJI + " Лютеиновая фаза\n" +
                BotTextConstants.FERTILE_WINDOW_EMOJI + " Фертильное окно\n" +
                BotTextConstants.CYCLE_DELAY_EMOJI + " Задержка цикла";
    }

}
