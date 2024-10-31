package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UserCycleManagementService {

    private final UserService userService;
    private final CycleService cycleService;

    public int getCurrentDay(long chatId) {
        // Поиск пользователя по chatId
        Optional<User> optionalUser = userService.findById(chatId);

        if (optionalUser.isEmpty()) {
            // Если пользователя не найдено, выбрасываем исключение
            throw new IllegalArgumentException("Пользователь не найден. Пожалуйста, зарегистрируйтесь.");
        }

        // Получение списка циклов пользователя
        List<Cycle> cycles = optionalUser.get().getCycles();

        if (cycles.isEmpty()) {
            // Если нет циклов, выбрасываем исключение
            throw new IllegalArgumentException("У вас нет активных циклов. Пожалуйста, начните новый цикл.");
        }

        // Пытаемся получить актуальный цикл
        Cycle actualCycle;
        try {
            actualCycle = cycleService.getActualCycle(cycles);
        } catch (IllegalArgumentException e) {
            // Если нет активного цикла, выбрасываем исключение
            throw new IllegalArgumentException("Нет активного цикла. Пожалуйста, начните новый цикл.");
        }

        // Вычисляем текущий день цикла
        LocalDate today = LocalDate.now();
        if (today.isBefore(actualCycle.getStartDate())) {
            throw new IllegalArgumentException("Дата начала цикла в будущем. Проверьте ваши данные.");
        }

        int currentDay = (int) (today.toEpochDay() - actualCycle.getStartDate().toEpochDay()) + 1;

        return currentDay;
    }


    // Получение последних завершенных циклов по chatId с ограничением
    public List<LocalDate> findLastCompletedCycleEndDatesByChatId(long chatId, int limit) {
        List<Cycle> completedCycles = cycleService.findCompletedCyclesByChatId(chatId);
        return completedCycles.stream()
                .map(Cycle::getEndDate)
                .limit(limit)
                .collect(Collectors.toList());
    }

}
