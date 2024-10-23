package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.repository.CycleRepository;
import com.example.menstrualcyclebot.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CycleRecalculationService {

    private final CycleRepository cycleRepository;
    private final NotificationService notificationService;  // Внедрение нового сервиса для отправки уведомлений

    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Moscow")  // Запуск каждый день в 6:00 утра по Москве
    public void recalculateAndUpdateCycles() {
        // Получаем все активные циклы (статус ACTIVE)
        List<Cycle> activeCycles = cycleRepository.findAllByStatus(CycleStatus.ACTIVE);

        for (Cycle cycle : activeCycles) {
            LocalDate today = LocalDate.now();

            // Пересчёт ожидаемой даты завершения
            LocalDate expectedEndDate = cycle.getStartDate().plusDays(cycle.getCycleLength());
            cycle.setExpectedEndDate(expectedEndDate);

            // Если текущая дата больше ожидаемой даты завершения, значит, это задержка
            if (today.isAfter(expectedEndDate)) {
                int daysOverdue = (int) (today.toEpochDay() - expectedEndDate.toEpochDay());
                cycle.setDelayDays(daysOverdue);
                cycle.setStatus(CycleStatus.DELAYED);

                // Если задержка больше 14 дней, цикл считается завершённым
                if (daysOverdue >= 99) {
                    cycle.setEndDate(today);
                    cycle.setStatus(CycleStatus.COMPLETED);
                }

                // Отправка уведомления о задержке
                String message = "У вас задержка на " + daysOverdue + " дней. Пожалуйста, проверьте свои данные.";
                notificationService.notifyUser(cycle.getUser().getChatId(), message);
            } else {
                cycle.setDelayDays(0);
                cycle.setStatus(CycleStatus.ACTIVE);
            }

            // Сохраняем изменения в базе данных
            cycleRepository.save(cycle);
        }
    }
}
