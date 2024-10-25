package com.example.menstrualcyclebot.utils;

import com.example.menstrualcyclebot.domain.Cycle;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Component
public class CycleCalculator {

    public static void calculateCycleFields(Cycle cycle) {
        int D = cycle.getCycleLength();
        int periodLength = cycle.getPeriodLength();
        LocalDate startDate = cycle.getStartDate();

        // 1. Расчет дня овуляции
        int ovulationDay = (int) Math.round(D / 2.0);
        cycle.setOvulationDay(ovulationDay);

        LocalDate ovulationDate = startDate.plusDays(ovulationDay - 1);
        cycle.setOvulationDate(ovulationDate);

        // 2. Расчет начала и конца овуляторного периода
        int ovulationStartDay = ovulationDay - 3;
        int ovulationEndDay = ovulationDay + 1;
        cycle.setOvulationStartDay(ovulationStartDay);
        cycle.setOvulationEndDay(ovulationEndDay);

        // 3. Даты овуляторного периода
        LocalDate ovulationStartDate = startDate.plusDays(ovulationStartDay - 1);
        LocalDate ovulationEndDate = startDate.plusDays(ovulationEndDay - 1);

        // 4. Дата окончания менструации
        LocalDate menstruationEndDate = startDate.plusDays(periodLength - 1);

        // 5. Фолликулярная фаза
        LocalDate follicularPhaseStart = menstruationEndDate.plusDays(1);
        LocalDate follicularPhaseEnd = ovulationStartDate.minusDays(1);
        cycle.setFollicularPhaseStart(follicularPhaseStart);
        cycle.setFollicularPhaseEnd(follicularPhaseEnd);

        // 6. Лютеиновая фаза
        // Начинается на второй день после окончания овуляции
        LocalDate lutealPhaseStart = ovulationEndDate.plusDays(2);
        LocalDate lutealPhaseEnd = startDate.plusDays(D - 1);
        cycle.setLutealPhaseStart(lutealPhaseStart);
        cycle.setLutealPhaseEnd(lutealPhaseEnd);

        // 7. Расчет даты конца цикла
        LocalDate endDate = startDate.plusDays(D - 1);
        cycle.setEndDate(endDate);  // Устанавливаем конец цикла
    }

    public static void recalculateCycleFieldsBasedOnEndDate(Cycle cycle) {
        LocalDate startDate = cycle.getStartDate();
        LocalDate endDate = cycle.getEndDate();

        // 1. Перерасчет длины цикла на основе фактической даты завершения
        int recalculatedCycleLength = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        cycle.setCycleLength(recalculatedCycleLength);

        // 2. Лютеиновая фаза
        // Начинается на второй день после окончания овуляции
        LocalDate lutealPhaseEnd = endDate;
        cycle.setLutealPhaseEnd(lutealPhaseEnd);  // Устанавливаем конец лютеиновой фазы
    }

}
