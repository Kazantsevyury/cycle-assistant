package com.example.menstrualcyclebot.service.sbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.repository.CycleRepository;
import org.springframework.stereotype.Service;

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
}
