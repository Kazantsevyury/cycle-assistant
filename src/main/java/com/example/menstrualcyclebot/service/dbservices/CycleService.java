package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.repository.CycleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CycleService {

    private final CycleRepository cycleRepository;

    public CycleService(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    // Найти цикл по ID
    public Optional<Cycle> findById(Long cycleId) {
        return cycleRepository.findById(cycleId);
    }

    // Найти все активные циклы, у которых endDate равен null
    public List<Cycle> findAllActiveCycles() {
        return cycleRepository.findByEndDateIsNull();
    }
    // Найти все циклы
    public List<Cycle> findAll() {
        return cycleRepository.findAll();
    }

    // Сохранить цикл
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
}

