package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.service.dbservices.UserService;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.UserUtils;
import com.example.menstrualcyclebot.utils.BotTextConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import static com.example.menstrualcyclebot.domain.CycleStatus.DELAYED;

@Slf4j
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
        LocalDate today = LocalDate.now();

        // Add today's info button
        addTodayRow(keyboard, today);

        // Add weekday row
        addWeekdayRow(keyboard);

        // Fetch user and their cycles
        User user = getUser(userChatId, update);
        List<Cycle> sortedCycles = getSortedCycles(user.getChatId());

        // Fill calendar days
        fillCalendarDays(year, month, today, keyboard, sortedCycles);

        // Add question mark logic
        addQuestionMark(keyboard);

        // Add navigation buttons
        addNavigationRow(keyboard, year, month);

        inlineKeyboardMarkup.setKeyboard(keyboard);
        return inlineKeyboardMarkup;
    }

    private void addTodayRow(List<List<InlineKeyboardButton>> keyboard, LocalDate today) {
        String monthName = today.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"));
        String todayText = String.format("Сегодня: %d %s", today.getDayOfMonth(), monthName);

        InlineKeyboardButton infoButton = new InlineKeyboardButton();
        infoButton.setText(todayText);
        infoButton.setCallbackData("today_info");

        keyboard.add(Collections.singletonList(infoButton));
    }

    private void addWeekdayRow(List<List<InlineKeyboardButton>> keyboard) {
        List<InlineKeyboardButton> weekDaysRow = Arrays.asList(
                createButton("Пн"),
                createButton("Вт"),
                createButton("Ср"),
                createButton("Чт"),
                createButton("Пт"),
                createButton("Сб"),
                createButton("Вс")
        );
        keyboard.add(weekDaysRow);
    }

    private User getUser(long userChatId, Update update) {
        return userService.findById(userChatId).orElseGet(() -> {
            User newUser = UserUtils.createNewUser(update);
            userService.save(newUser);
            return newUser;
        });
    }

    private List<Cycle> getSortedCycles(Long userId) {
        User user = userService.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Cycle> cycles = new ArrayList<>(user.getCycles());
        cycles.sort(Comparator.comparing(Cycle::getStartDate));
        return cycles;
    }

    private void fillCalendarDays(int year, int month, LocalDate today, List<List<InlineKeyboardButton>> keyboard, List<Cycle> sortedCycles) {
        LocalDate firstDayOfMonth = LocalDate.of(year, month, 1);
        int daysInMonth = firstDayOfMonth.lengthOfMonth();
        int firstDayOfWeek = firstDayOfMonth.getDayOfWeek().getValue(); // 1 = Пн, 7 = Вс
        int currentDay = 1;

        for (int i = 0; i < 6; i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();

            for (int j = 1; j <= 7; j++) {
                if (i == 0 && j < firstDayOfWeek) {
                    row.add(createButton(" "));
                } else if (currentDay <= daysInMonth) {
                    String dayText = String.valueOf(currentDay);

                    for (Cycle cycle : sortedCycles) {
                        String emoji = getEmojiForDay(cycle, year, month, currentDay);
                        if (!emoji.isEmpty()) {
                            dayText += emoji;
                            break;
                        }
                    }

                    row.add(createButton(dayText));
                    currentDay++;
                } else {
                    row.add(createButton(" "));
                }
            }

            keyboard.add(row);
            if (currentDay > daysInMonth) {
                break;
            }
        }
    }

    private void addQuestionMark(List<List<InlineKeyboardButton>> keyboard) {
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

        if (!addedQuestionMark) {
            for (List<InlineKeyboardButton> row : keyboard) {
                if (row.size() == 7 && row.stream().anyMatch(btn -> btn.getText().matches("\\d+"))) {
                    InlineKeyboardButton firstMondayButton = row.get(0);
                    firstMondayButton.setText(firstMondayButton.getText() + " " + BotTextConstants.QUESTION_MARK_EMOJI);
                    firstMondayButton.setCallbackData("info_question_mark_monday");
                    break;
                }
            }
        }
    }

    private void addNavigationRow(List<List<InlineKeyboardButton>> keyboard, int year, int month) {
        List<InlineKeyboardButton> navigationRow = new ArrayList<>();
        navigationRow.add(createNavigationButton(" ⬅️ ", year, month - 1));

        String monthDisplayName = russianMonths.get(month - 1);
        InlineKeyboardButton monthButton = new InlineKeyboardButton();
        monthButton.setText(monthDisplayName);
        monthButton.setCallbackData("noop");
        navigationRow.add(monthButton);

        navigationRow.add(createNavigationButton(" ➡️ ", year, month + 1));
        keyboard.add(navigationRow);
    }


    private String getEmojiForDay(Cycle cycle, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);

        // Получение всех циклов пользователя
        List<Cycle> allCycles = getAllCyclesWithFuture(cycle);

        for (Cycle userCycle : allCycles) {
            if (!date.isBefore(userCycle.getStartDate()) &&
                    (userCycle.getExpectedEndDate() == null || !date.isAfter(userCycle.getExpectedEndDate()))) {
                return getPhaseEmojiForDate(userCycle, date);
            }
        }

        return "";
    }

    private List<Cycle> getAllCyclesWithFuture(Cycle currentCycle) {
        List<Cycle> allCycles = new ArrayList<>(getSortedCycles(currentCycle.getUser().getChatId()));

        Optional<Cycle> baseCycleOptional = allCycles.stream()
                .filter(cycle -> cycle.getStatus() == CycleStatus.ACTIVE || cycle.getStatus() == CycleStatus.DELAYED)
                .findFirst();

        if (baseCycleOptional.isEmpty()) {
            log.warn("No active or delayed cycle found for user.");
            return allCycles;
        }

        Cycle baseCycle = baseCycleOptional.get();

        LocalDate nextStartDate = baseCycle.getStatus() == CycleStatus.DELAYED
                ? LocalDate.now().plusDays(1)
                : baseCycle.getExpectedEndDate() != null
                ? baseCycle.getExpectedEndDate().plusDays(1)
                : baseCycle.getStartDate().plusDays(baseCycle.getCycleLength());

        for (int i = 0; i < 5; i++) {
            Cycle futureCycle = new Cycle();
            futureCycle.setStartDate(nextStartDate);
            futureCycle.setCycleLength(baseCycle.getCycleLength());
            futureCycle.setPeriodLength(baseCycle.getPeriodLength());
            futureCycle.setStatus(CycleStatus.FUTURE);

            CycleCalculator.calculateCycleFields(futureCycle);

            allCycles.add(futureCycle);

            nextStartDate = futureCycle.getExpectedEndDate().plusDays(1);
        }

        return allCycles;
    }

    private String planFutureCycles(Cycle currentCycle, LocalDate date) {
        log.debug("Planning future cycles for date: {} using cycle: {}", date, currentCycle);

        int cycleLength = currentCycle.getCycleLength();
        int periodLength = currentCycle.getPeriodLength();

        LocalDate nextCycleStartDate = currentCycle.getExpectedEndDate() != null
                ? currentCycle.getExpectedEndDate().plusDays(1)
                : currentCycle.getStartDate().plusDays(cycleLength);

        while (nextCycleStartDate.isBefore(date.plusDays(1))) {
            Cycle futureCycle = new Cycle();
            futureCycle.setStartDate(nextCycleStartDate);
            futureCycle.setCycleLength(cycleLength);
            futureCycle.setPeriodLength(periodLength);
            futureCycle.setEndDate(nextCycleStartDate.plusDays(periodLength - 1));

            CycleCalculator.calculateCycleFields(futureCycle);

            log.debug("Future cycle: startDate={}, endDate={}, periodLength={}, fertileWindow=[{}-{}]",
                    futureCycle.getStartDate(),
                    futureCycle.getEndDate(),
                    futureCycle.getPeriodLength(),
                    futureCycle.getFertileWindowStartDay(),
                    futureCycle.getFertileWindowEndDay());

            if (!date.isBefore(futureCycle.getStartDate()) && !date.isAfter(futureCycle.getEndDate())) {
                return getPhaseEmojiForDate(futureCycle, date);
            }

            nextCycleStartDate = futureCycle.getEndDate().plusDays(1);
        }

        return BotTextConstants.CYCLE_DELAY_EMOJI;
    }

    private String getPhaseEmojiForDate(Cycle cycle, LocalDate date) {
        log.debug("Determining phase emoji for date: {} and cycle: {}", date, cycle);
        // Приоритет для первого дня цикла
        if (date.isEqual(cycle.getStartDate())  ) {
            log.debug("Date {} matches start date of the cycle: {}", date, cycle.getStartDate());
            return BotTextConstants.MENSTRUATION_EMOJI;
        }
        if (cycle.getOvulationDate() != null && date.isEqual(cycle.getOvulationDate())) {
            log.debug("Date {} matches ovulation date: {}", date, cycle.getOvulationDate());
            return BotTextConstants.OVULATION_EMOJI;
        }

        if (cycle.getFertileWindowStartDay() != null && cycle.getFertileWindowEndDay() != null) {
            LocalDate fertileStart = cycle.getStartDate().plusDays(cycle.getFertileWindowStartDay() - 1);
            LocalDate fertileEnd = cycle.getStartDate().plusDays(cycle.getFertileWindowEndDay() - 1);
            if (!date.isBefore(fertileStart) && !date.isAfter(fertileEnd)) {
                log.debug("Date {} is within fertile window: [{} - {}]", date, fertileStart, fertileEnd);
                return BotTextConstants.FERTILE_WINDOW_EMOJI;
            }
        }



        if (!date.isBefore(cycle.getStartDate()) && !date.isAfter(cycle.getStartDate().plusDays(cycle.getPeriodLength() - 1))) {
            log.debug("Date {} is within menstruation period: [{} - {}]",
                    date, cycle.getStartDate(),
                    cycle.getStartDate().plusDays(cycle.getPeriodLength() - 1));
            return BotTextConstants.MENSTRUATION_EMOJI;
        }


        if (cycle.getFollicularPhaseStart() != null && cycle.getOvulationDate() != null &&
                !date.isBefore(cycle.getFollicularPhaseStart()) && date.isBefore(cycle.getOvulationDate())) {
            log.debug("Date {} is within follicular phase: [{} - {}]", date,
                    cycle.getFollicularPhaseStart(), cycle.getOvulationDate());
            return BotTextConstants.FOLLICULAR_PHASE_EMOJI;
        }

        if (cycle.getLutealPhaseStart() != null && cycle.getLutealPhaseEnd() != null &&
                !date.isBefore(cycle.getLutealPhaseStart()) && !date.isAfter(cycle.getLutealPhaseEnd())) {
            log.debug("Date {} is within luteal phase: [{} - {}]", date,
                    cycle.getLutealPhaseStart(), cycle.getLutealPhaseEnd());
            return BotTextConstants.LUTEAL_PHASE_EMOJI;
        }


        if (cycle.getDelayDays() != 0 ) {
            if (date.isAfter(cycle.getStartDate().plusDays(cycle.getCycleLength() - 1))) {
            log.debug("Date {} is beyond cycle length. Returning delay emoji.", date);
            return BotTextConstants.CYCLE_DELAY_EMOJI;
        }}



        log.debug("No phase matched for date: {} and cycle: {}", date, cycle);
        return "";
    }


    private InlineKeyboardButton createButton(String text) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(text);
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
