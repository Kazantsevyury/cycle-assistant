package com.example.menstrualcyclebot.domain;

import lombok.ToString;
import java.time.LocalDate;

@ToString
public class CycleInfo {
    private final LocalDate startDate;
    private final LocalDate periodEndDate;
    private final LocalDate follicularPhaseEndDate;
    private final LocalDate ovulationDate;
    private final LocalDate lutealPhaseStartDate;
    private final LocalDate lutealPhaseEndDate;

    public CycleInfo(LocalDate startDate, LocalDate periodEndDate, LocalDate follicularPhaseEndDate,
                     LocalDate ovulationDate, LocalDate lutealPhaseStartDate, LocalDate lutealPhaseEndDate) {
        this.startDate = startDate;
        this.periodEndDate = periodEndDate;
        this.follicularPhaseEndDate = follicularPhaseEndDate;
        this.ovulationDate = ovulationDate;
        this.lutealPhaseStartDate = lutealPhaseStartDate;
        this.lutealPhaseEndDate = lutealPhaseEndDate;
    }
    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getOvulationDate() {
        // Овуляция наступает на 14-й день
        return startDate.plusDays(14);
    }

    public int getOvulationDay() {
        // Овуляция на 14-й день цикла
        return 14;
    }

    public int getOvulationStartDay() {
        // Начало овуляторного периода на 13-й день
        return 13;
    }

    public int getOvulationEndDay() {
        // Конец овуляторного периода на 15-й день
        return 15;
    }

    public LocalDate getFollicularPhaseStart() {
        // Фолликулярная фаза начинается с первого дня цикла
        return startDate;
    }

    public LocalDate getFollicularPhaseEnd() {
        // Фолликулярная фаза длится до дня овуляции (13-й день)
        return startDate.plusDays(13);
    }

    public LocalDate getLutealPhaseStart() {
        // Лютеиновая фаза начинается после овуляции (15-й день)
        return startDate.plusDays(15);
    }

    public LocalDate getLutealPhaseEnd() {
        // Лютеиновая фаза длится до конца цикла (28-й день)
        return startDate.plusDays(27);
    }
}
