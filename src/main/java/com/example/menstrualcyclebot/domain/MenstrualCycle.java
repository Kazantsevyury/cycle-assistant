package com.example.menstrualcyclebot.domain;

import lombok.Data;

import java.time.LocalDate;

@Data
public class MenstrualCycle {
    private Long userId;        // Идентификатор пользователя (например, chatId)
    private Long cycleId;       // Уникальный идентификатор цикла
    private LocalDate startDate; // Дата начала менструации
    private int cycleLength;    // Длина цикла (в днях)
    private int periodLength;   // Длительность менструации (в днях)
}
