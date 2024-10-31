package com.example.menstrualcyclebot.service.dbservices;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import com.example.menstrualcyclebot.domain.User;
import com.example.menstrualcyclebot.repository.CycleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CycleService {

    private final CycleRepository cycleRepository;

    public CycleService(CycleRepository cycleRepository) {
        this.cycleRepository = cycleRepository;
    }

    // Метод для проверки пересечения циклов
    public boolean hasOverlappingCycle(Long userId, LocalDate newStartDate, LocalDate newEndDate) {
        List<Cycle> existingCycles = cycleRepository.findByUserChatId(userId);
        log.info("Проверка пересечений для нового цикла: userId={}, newStartDate={}, newEndDate={}", userId, newStartDate, newEndDate);

        for (Cycle cycle : existingCycles) {
            LocalDate existingStartDate = cycle.getStartDate();
            LocalDate existingEndDate = existingStartDate.plusDays(cycle.getCycleLength() + cycle.getDelayDays());

            log.debug("Сравнение с существующим циклом: startDate={}, endDate={}, userId={}", existingStartDate, existingEndDate, userId);

            // Проверка пересечений
            if ((newStartDate.isBefore(existingEndDate) || newStartDate.isEqual(existingEndDate)) &&
                    (newEndDate.isAfter(existingStartDate) || newEndDate.isEqual(existingStartDate))) {
                log.warn("Пересечение найдено! Новый цикл ({} - {}) пересекается с существующим циклом ({} - {})",
                        newStartDate, newEndDate, existingStartDate, existingEndDate);

                // Логируем конкретные точки пересечения
                if (newStartDate.isBefore(existingEndDate) && newEndDate.isAfter(existingStartDate)) {
                    log.info("Полное пересечение между {} и {}", existingStartDate, existingEndDate);
                }
                if (newStartDate.isEqual(existingEndDate)) {
                    log.info("Пересечение на начальной точке нового цикла: newStartDate = existingEndDate = {}", existingEndDate);
                }
                if (newEndDate.isEqual(existingStartDate)) {
                    log.info("Пересечение на конечной точке существующего цикла: newEndDate = existingStartDate = {}", existingStartDate);
                }
                return true;
            }
        }

        log.info("Пересечений не найдено для нового цикла: userId={}, newStartDate={}, newEndDate={}", userId, newStartDate, newEndDate);
        return false;
    }


    // Найти цикл по ID
    @Transactional
    public Optional<Cycle> findById(Long cycleId) {
        return cycleRepository.findById(cycleId);
    }

    // Удалить цикл по chatId и endDate, с использованием deleteCycleById
    @Transactional
    public void deleteCycleByEndDateAndChatId(long chatId, LocalDate endDate) {
        Cycle cycle = cycleRepository.findByUserChatIdAndEndDate(chatId, endDate)
                .orElseThrow(() -> new EntityNotFoundException("Cycle not found with chatId: " + chatId + " and endDate: " + endDate));
        deleteCycleById(cycle.getCycleId());
    }

    // Найти все циклы
    @Transactional
    public List<Cycle> findAll() {
        return cycleRepository.findAll();
    }

    // Сохранить цикл с проверкой на пересечение
    @Transactional
    public Cycle save(Cycle cycle) {/*
        LocalDate startDate = cycle.getStartDate();
        LocalDate endDate = cycle.getEndDate();
        Long userId = cycle.getUser().getChatId();

        if (hasOverlappingCycle(userId, startDate, endDate)) {
            throw new IllegalArgumentException("Цикл пересекается с уже существующим циклом.");
        }
*/
        return cycleRepository.save(cycle);
    }

    // Метод для сохранения завершенного цикла с проверкой
    @Transactional
    public void saveHistoricalCycle(Cycle cycle) {
        cycle.setStatus(CycleStatus.COMPLETED);

        LocalDate startDate = cycle.getStartDate();
        LocalDate endDate = cycle.getEndDate();
        Long userId = cycle.getUser().getChatId();

        if (hasOverlappingCycle(userId, startDate, endDate)) {
            throw new IllegalArgumentException("Цикл пересекается с уже существующим циклом.");
        }

        cycleRepository.save(cycle);
    }

    // Метод для получения актуального цикла
    public Cycle getActualCycle(List<Cycle> cycles) {
        LocalDate today = LocalDate.now();
        for (Cycle cycle : cycles)
            if (cycle.getStatus().equals(CycleStatus.ACTIVE) || cycle.getStatus().equals(CycleStatus.DELAYED)) {
                return cycle;
            }
        throw new IllegalArgumentException("У пользователя нет актуальных циклов.");
    }

    public Optional<Cycle> findActiveOrDelayedCycleByChatId(Long chatId) {
        return cycleRepository.findFirstByUser_ChatIdAndStatusIn(
                chatId, Arrays.asList(CycleStatus.ACTIVE, CycleStatus.DELAYED));
    }

    @Transactional
    public void deleteCycleById(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new EntityNotFoundException("Cycle not found"));
        User user = cycle.getUser();
        user.getCycles().remove(cycle);
        cycleRepository.delete(cycle);
        log.info("Cycle with ID {} has been deleted from the database.", cycleId);
    }

    // Поиск завершенных циклов для пользователя по chatId
    public List<Cycle> findCompletedCyclesByChatId(long chatId) {
        return cycleRepository.findListByUser_ChatIdAndStatus(chatId);
    }

    public List<Cycle> findLastCompletedCyclesByChatId(long chatId, int limit) {
        List<Cycle> completedCycles = cycleRepository.findCompletedCyclesByChatIdAndStatus(chatId, CycleStatus.COMPLETED);
        return completedCycles.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}
