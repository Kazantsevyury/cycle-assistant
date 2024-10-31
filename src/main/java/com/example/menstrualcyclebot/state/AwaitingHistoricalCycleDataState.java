package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.service.dbservices.CycleService;
import com.example.menstrualcyclebot.service.dbservices.UserCycleManagementService;
import com.example.menstrualcyclebot.utils.CycleCalculator;
import com.example.menstrualcyclebot.utils.DateParserUtils;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.menstrualcyclebot.utils.UIUtils.createCycleDatesKeyboard;

@Slf4j
public class AwaitingHistoricalCycleDataState implements UserStateHandler {
    private final CycleService cycleService;
    private final UserCycleManagementService userCycleManagementService;
    private final List<Long> completedCycleIds; // Поле для хранения ID завершённых циклов
    private final List<LocalDate> completedCycleEndDates; // Поле для хранения дат завершения завершённых циклов
    private final int cycleLimit = 6; // Лимит на количество завершённых циклов

    public AwaitingHistoricalCycleDataState(CycleService cycleService, UserCycleManagementService userCycleManagementService, long chatId) {
        this.cycleService = cycleService;
        this.userCycleManagementService = userCycleManagementService;

        // Инициализация списков для хранения данных завершённых циклов
        this.completedCycleIds = new ArrayList<>();
        this.completedCycleEndDates = new ArrayList<>();

        // Получаем последние завершённые циклы из базы данных и сохраняем их ID и даты
        List<Cycle> completedCycles = cycleService.findLastCompletedCyclesByChatId(chatId, cycleLimit);
        for (Cycle cycle : completedCycles) {
            this.completedCycleIds.add(cycle.getCycleId());           // Сохраняем ID цикла
            this.completedCycleEndDates.add(cycle.getEndDate());      // Сохраняем дату завершения
        }
    }

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        String messageText = update.getMessage().getText();
        long chatId = update.getMessage().getChatId();

        log.debug("AwaitingHistoricalCycleDataState active for chatId {}, messageText: {}", chatId, messageText);

        // Проверяем и обрабатываем введённую дату
        LocalDate startDate = DateParserUtils.parseDateWithMultipleFormats(messageText);
        if (startDate == null) {
            bot.sendMessage(chatId, "Введите дату завершённого цикла в формате: dd.MM.yyyy, d.MM.yyyy, d.M.yyyy или dd-MM-yyyy.");
            return;
        }
        if (startDate.isAfter(LocalDate.now())) {
            bot.sendMessage(chatId, "Дата не может быть в будущем. Введите корректную дату:");
            return;
        }

        // Получаем пользователя
        Optional<User> userOptional = bot.getUserService().findById(chatId);
        if (userOptional.isEmpty()) {
            bot.sendMessage(chatId, "Пользователь не найден.");
            log.error("User not found for chatId {}", chatId);
            return;
        }

        User user = userOptional.get();
        LocalDate endDate = startDate.plusDays(cycle.getCycleLength()).plusDays(cycle.getDelayDays());

        // Проверяем пересечения цикла
        if (cycleService.hasOverlappingCycle(chatId, startDate, endDate)) {
            bot.sendMessage(chatId, "Цикл пересекается с уже существующим циклом. Введите корректные данные.");
            log.warn("Найдено пересечение для нового цикла: startDate={}, endDate={} для chatId={}", startDate, endDate, chatId);
            return;
        }

        // Сохранение нового завершённого цикла, только если нет пересечений
        saveHistoricalCycle(bot, chatId, user, startDate, cycle);
    }


    private void saveHistoricalCycle(Bot bot, long chatId, User user, LocalDate startDate, Cycle cycle) {
        try {
            // Получаем активный или задержанный цикл пользователя для копирования данных
            Optional<Cycle> activeCycle = cycleService.findActiveOrDelayedCycleByChatId(chatId);
            if (activeCycle.isPresent()) {
                cycle.setUser(user);
                cycle.setStartDate(startDate);
                cycle.setCycleLength(activeCycle.get().getCycleLength());
                cycle.setPeriodLength(activeCycle.get().getPeriodLength());

                // Расчёт всех полей цикла с помощью CycleCalculator
                CycleCalculator.calculateCycleFields(cycle);
                int D = cycle.getCycleLength();
                LocalDate endDate = startDate.plusDays(D - 1);
                cycle.setEndDate(endDate);
                cycle.setStatus(CycleStatus.COMPLETED);
                // Сохраняем завершённый цикл через cycleService
                cycleService.saveHistoricalCycle(cycle);

                // Получаем обновлённый список завершённых циклов после сохранения
                List<LocalDate> completedCycleEndDates = userCycleManagementService.findLastCompletedCycleEndDatesByChatId(chatId, cycleLimit);

                // Формируем сообщение с обновлённым списком завершённых циклов
                StringBuilder message = new StringBuilder("Обновлённый список завершённых циклов:\n");
                for (int i = 0; i < completedCycleEndDates.size(); i++) {
                    message.append(i + 1).append(". ").append(completedCycleEndDates.get(i)).append("\n");
                }

                // Отправляем сообщение с обновлённым списком и предложением ввести ещё один цикл
                bot.sendMessage(chatId, message.toString());
                bot.sendMessageWithKeyboard(chatId, "Хотите ввести ещё один цикл?", createCycleDatesKeyboard());

                // Сбрасываем состояние пользователя на NoneState, чтобы он мог выбрать действия на новой клавиатуре
                bot.changeUserState(chatId, new NoneState());
            } else {
                bot.sendMessage(chatId, "У вас нет активного или задержанного цикла для получения данных.");
                log.warn("No active or delayed cycle found for chatId {}", chatId);
            }
        } catch (IllegalArgumentException e) {
            bot.sendMessage(chatId, "Ошибка: " + e.getMessage());
            log.error("Ошибка при сохранении исторического цикла для chatId {}: {}", chatId, e.getMessage());
        } catch (Exception e) {
            bot.sendMessage(chatId, "Произошла ошибка при сохранении цикла. Попробуйте позже.");
            log.error("Unexpected error while saving historical cycle for chatId {}: {}", chatId, e.getMessage(), e);
        }
    }
}
