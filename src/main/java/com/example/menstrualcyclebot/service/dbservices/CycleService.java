package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.repository.CycleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class CycleService {

    private final CycleRepository cycleRepository;

    public CycleService(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    // Найти цикл по ID
    @Transactional
    public Optional<Cycle> findById(Long cycleId) {
        return cycleRepository.findById(cycleId);
    }

    // Найти все циклы
    @Transactional
    public List<Cycle> findAll() {
        return cycleRepository.findAll();
    }

    // Сохранить цикл
    @Transactional
    public Cycle save(Cycle cycle) {
        return cycleRepository.save(cycle);
    }

    // Удалить цикл по ID
    public void deleteById(Long cycleId) {
        cycleRepository.deleteById(cycleId);
    }

    // Обновить цикл
    public Cycle updateCycle(Cycle cycle) {
        if (cycleRepository.existsById(cycle.getCycleId())) {
            return cycleRepository.save(cycle);
        } else {
            throw new IllegalArgumentException("Менструальный цикл с ID " + cycle.getCycleId() + " не найден");
        }
    }
    public Optional<Cycle> findActiveCycleByChatId(long chatId) {
        return cycleRepository.findFirstByUser_ChatIdAndStatus(chatId, CycleStatus.ACTIVE);
    }

    public Cycle getActualCycle(List<Cycle> cycles) {
        // Получаем текущую дату
        LocalDate today = LocalDate.now();

        // Ищем цикл, у которого endDate больше, чем текущий день
        for (Cycle cycle : cycles) {
            if (cycle.getEndDate().isAfter(today)) {
                return cycle;  // Возвращаем первый найденный актуальный цикл
            }
        }

        // Если ни один актуальный цикл не найден, выбрасываем исключение
        throw new IllegalArgumentException("У пользователя нет актуальных циклов.");
    }
    public Optional<Cycle> findActiveOrDelayedCycleByChatId(Long chatId) {
        // Ищем цикл со статусом ACTIVE или DELAYED
        return cycleRepository.findFirstByUser_ChatIdAndStatusIn(
                chatId, Arrays.asList(CycleStatus.ACTIVE, CycleStatus.DELAYED));
    }
    @Transactional
    public void deleteCycleById(Long cycleId) {
        cycleRepository.deleteByCycleId(cycleId);
    }
}

