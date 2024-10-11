package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
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
    public Optional<MenstrualCycle> findById(Long cycleId) {
        return cycleRepository.findById(cycleId);
    }

    // Найти все циклы
    public List<MenstrualCycle> findAll() {
        return cycleRepository.findAll();
    }

    // Сохранить цикл
    public MenstrualCycle save(MenstrualCycle cycle) {
        return cycleRepository.save(cycle);
    }

    // Удалить цикл по ID
    public void deleteById(Long cycleId) {
        cycleRepository.deleteById(cycleId);
    }

    // Обновить цикл
    public MenstrualCycle updateCycle(MenstrualCycle cycle) {
        if (cycleRepository.existsById(cycle.getCycleId())) {
            return cycleRepository.save(cycle);
        } else {
            throw new IllegalArgumentException("Менструальный цикл с ID " + cycle.getCycleId() + " не найден");
        }
    }
}
