package com.example.menstrualcyclebot.state;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.presentation.Bot;
import com.example.menstrualcyclebot.service.dbservices.CycleService;
import com.example.menstrualcyclebot.service.dbservices.UserCycleManagementService;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public class AwaitingCycleDeletionState implements UserStateHandler {

    private final CycleService cycleService;
    private final List<LocalDate> completedCycleEndDates;
    private final UserCycleManagementService userCycleManagementService;


    public AwaitingCycleDeletionState(CycleService cycleService, List<LocalDate> completedCycleEndDates, UserCycleManagementService userCycleManagementService) {
        this.cycleService = cycleService;
        this.completedCycleEndDates = completedCycleEndDates;
        this.userCycleManagementService = userCycleManagementService;
    }

    @Override
    public void handleState(Bot bot, Update update, Cycle cycle) {
        long chatId = update.getMessage().getChatId();
        String messageText = update.getMessage().getText();

        try {
            // Преобразуем ввод в номер
            int cycleNumber = Integer.parseInt(messageText);

            if (cycleNumber < 1 || cycleNumber > completedCycleEndDates.size()) {
                bot.sendMessage(chatId, "Неверный номер. Введите номер из списка.");
                return;
            }

            // Находим дату завершённого цикла, который нужно удалить
            LocalDate cycleEndDate = completedCycleEndDates.get(cycleNumber - 1);
            cycleService.deleteCycleByEndDateAndChatId(chatId, cycleEndDate); // Удаляем цикл по дате завершения и chatId

            bot.sendMessage(chatId, "Цикл с номером " + cycleNumber + " успешно удалён.");
            bot.handleHistoricalCycleData(chatId); // Обновляем список завершённых циклов
            bot.changeUserState(chatId, new AwaitingHistoricalCycleDataState(cycleService, userCycleManagementService, chatId));
        } catch (NumberFormatException e) {
            bot.sendMessage(chatId, "Пожалуйста, введите корректный номер.");
        }
    }
}
