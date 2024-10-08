package com.example.menstrualcyclebot.service;

import com.example.menstrualcyclebot.domain.CycleInfo;
import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class CycleService {
    private final CycleCalculationService cycleCalculationService;

    public CycleService(CycleCalculationService cycleCalculationService) {
        this.cycleCalculationService = cycleCalculationService;
    }

    public CycleInfo calculateCurrentCycle(int cycleLength, int periodLength, LocalDate lastCycleStartDate) {
        return cycleCalculationService.calculateCycleInfo(cycleLength, periodLength, lastCycleStartDate);
    }

    public List<CycleInfo> getCycleForecast(int cycleLength, int periodLength, LocalDate lastCycleStartDate) {
        return cycleCalculationService.calculateForecast(cycleLength, periodLength, lastCycleStartDate);
    }
}