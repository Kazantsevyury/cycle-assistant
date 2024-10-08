package com.example.menstrualcyclebot.service;


import com.example.menstrualcyclebot.domain.CycleInfo;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CycleCalculationService {

    public CycleInfo calculateCycleInfo(int cycleLength, int periodLength, LocalDate lastCycleStartDate) {
        LocalDate ovulationDate = lastCycleStartDate.plusDays(cycleLength / 2);
        LocalDate follicularPhaseEnd = ovulationDate.minusDays(1);
        LocalDate lutealPhaseStart = ovulationDate.plusDays(1);
        LocalDate lutealPhaseEnd = lastCycleStartDate.plusDays(cycleLength - 1);

        return new CycleInfo(
                lastCycleStartDate,
                lastCycleStartDate.plusDays(periodLength - 1),
                follicularPhaseEnd,
                ovulationDate,
                lutealPhaseStart,
                lutealPhaseEnd
        );
    }

    public List<CycleInfo> calculateForecast(int cycleLength, int periodLength, LocalDate lastCycleStartDate) {
        List<CycleInfo> forecast = new ArrayList<>();
        LocalDate currentCycleStart = lastCycleStartDate;

        for (int i = 0; i < 6; i++) { // Calculate for 6 months ahead
            CycleInfo cycleInfo = calculateCycleInfo(cycleLength, periodLength, currentCycleStart);
            forecast.add(cycleInfo);
            currentCycleStart = currentCycleStart.plusDays(cycleLength);
        }

        return forecast;
    }
}