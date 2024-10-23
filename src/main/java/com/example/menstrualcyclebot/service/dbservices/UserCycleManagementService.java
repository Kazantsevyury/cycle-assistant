package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
        Cycle actualCycle = cycleService.getActualCycle(cycles);

        if (actualCycle == null) {
            throw new IllegalArgumentException("Нет активного цикла. Пожалуйста, начните новый цикл.");
        }

        // Вычисляем текущий день цикла
        LocalDate today = LocalDate.now();
        int currentDay = (int) (today.toEpochDay() - actualCycle.getStartDate().toEpochDay()) + 1;

        // Проверка корректности текущего дня
        if (currentDay < 1) {
            throw new IllegalArgumentException("Некорректная дата начала цикла. Пожалуйста, проверьте ваши данные.");
        }

        return currentDay;
    }
}
