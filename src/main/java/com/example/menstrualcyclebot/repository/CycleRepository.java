package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class CycleRepository {

    private final Map<Long, List<MenstrualCycle>> cycleData = new HashMap<>();

    // Сохранение данных о новом цикле
    public void saveCycle(MenstrualCycle cycle) {
        cycleData.computeIfAbsent(cycle.getUserId(), k -> new ArrayList<>()).add(cycle);
    }

    // Получение всех циклов пользователя
    public List<MenstrualCycle> getCycles(Long userId) {
        return cycleData.getOrDefault(userId, new ArrayList<>());
    }

    // Получение последнего цикла пользователя
    public MenstrualCycle getLastCycle(Long userId) {
        List<MenstrualCycle> cycles = cycleData.get(userId);
        if (cycles != null && !cycles.isEmpty()) {
            return cycles.get(cycles.size() - 1);
        }
        return null;
    }

    // Удаление всех данных о циклах пользователя
    public void deleteCycles(Long userId) {
        cycleData.remove(userId);
    }
}
