package com.example.menstrualcyclebot.utils;

import com.example.menstrualcyclebot.domain.Cycle;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class CycleCalculator {

    public static void calculateCycleFields(Cycle cycle) {
        // Получаем длину цикла из объекта cycle
        int D = cycle.getCycleLength();
        // Получаем длительность менструации из объекта cycle
        int periodLength = cycle.getPeriodLength();
        // Получаем начальную дату цикла из объекта cycle
        LocalDate startDate = cycle.getStartDate();

        // 1. Расчет дня овуляции
        // Определяем день овуляции как половину длины цикла, округляя до ближайшего целого
        int ovulationDay = (int) Math.round(D / 2.0);
        cycle.setOvulationDay(ovulationDay);

        // Определяем дату овуляции, добавляя количество дней с начала цикла
        LocalDate ovulationDate = startDate.plusDays(ovulationDay - 1);
        cycle.setOvulationDate(ovulationDate);

        // 2. Расчет начала и конца фертильного окна (раньше овуляторного периода)
        // Начало фертильного окна за 3 дня до овуляции
        int fertileWindowStartDay = ovulationDay - 3;
        // Конец фертильного окна через день после овуляции
        int fertileWindowEndDay = ovulationDay + 1;
        cycle.setFertileWindowStartDay(fertileWindowStartDay); // Сохраняем начало фертильного окна
        cycle.setFertileWindowEndDay(fertileWindowEndDay); // Сохраняем конец фертильного окна

        // 3. Даты фертильного окна
        // Определяем дату начала фертильного окна
        LocalDate fertileWindowStartDate = startDate.plusDays(fertileWindowStartDay - 1);
        // Определяем дату окончания фертильного окна
        LocalDate fertileWindowEndDate = startDate.plusDays(fertileWindowEndDay - 1);

        // 4. Дата окончания менструации
        // Определяем дату окончания менструации, добавляя период к дате начала цикла
        LocalDate menstruationEndDate = startDate.plusDays(periodLength - 1);

        // 5. Фолликулярная фаза
        // Начало фолликулярной фазы на следующий день после окончания менструации
        LocalDate follicularPhaseStart = menstruationEndDate.plusDays(1);
        // Конец фолликулярной фазы за день до начала фертильного окна
        LocalDate follicularPhaseEnd = fertileWindowStartDate.minusDays(1);
        cycle.setFollicularPhaseStart(follicularPhaseStart); // Сохраняем начало фолликулярной фазы
        cycle.setFollicularPhaseEnd(follicularPhaseEnd); // Сохраняем конец фолликулярной фазы

        // 6. Лютеиновая фаза
        // Лютеиновая фаза начинается сразу после фертильного окна
        LocalDate lutealPhaseStart = fertileWindowEndDate.plusDays(1);
        // Конец лютеиновой фазы совпадает с последним днем цикла
        LocalDate lutealPhaseEnd = startDate.plusDays(D - 1);
        cycle.setLutealPhaseStart(lutealPhaseStart); // Сохраняем начало лютеиновой фазы
        cycle.setLutealPhaseEnd(lutealPhaseEnd); // Сохраняем конец лютеиновой фазы

        // 7. Расчет даты конца цикла
        // Определяем дату окончания цикла, добавляя длину цикла к дате начала
        //LocalDate endDate = startDate.plusDays(D - 1);
        //cycle.setEndDate(endDate); // Устанавливаем конец цикла
    }

    public static void recalculateCycleFieldsBasedOnEndDate(Cycle cycle) {
        // Получаем начальную и конечную дату цикла из объекта cycle
        LocalDate startDate = cycle.getStartDate();
        LocalDate endDate = cycle.getEndDate();


        // 1. Перерасчет длины цикла на основе фактической даты завершения
        // Вычисляем длину цикла как разницу между концом и началом цикла + 1 день
        int recalculatedCycleLength = (int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1;
        cycle.setCycleLength(recalculatedCycleLength); // Обновляем длину цикла

        // Проверка, чтобы длительность менструации не превышала длину цикла
        if (cycle.getPeriodLength() > recalculatedCycleLength) {
            // Устанавливаем длительность менструации в длину цикла
            cycle.setPeriodLength(recalculatedCycleLength);

            // Устанавливаем фазы на 0, так как они невозможны при этой длине
            cycle.setOvulationDay(0);
            cycle.setFertileWindowStartDay(0);
            cycle.setFertileWindowEndDay(0);
            cycle.setFollicularPhaseStart(null);
            cycle.setFollicularPhaseEnd(null);
            cycle.setLutealPhaseStart(null);
            cycle.setLutealPhaseEnd(endDate);
        } else {
            // Если длительность менструации в порядке, пересчитываем поля цикла
            calculateCycleFields(cycle);
        }
        // 2. Лютеиновая фаза
        // Лютеиновая фаза заканчивается на дате окончания цикла
        LocalDate lutealPhaseEnd = endDate;
        cycle.setLutealPhaseEnd(lutealPhaseEnd); // Устанавливаем конец лютеиновой фазы
    }
}
