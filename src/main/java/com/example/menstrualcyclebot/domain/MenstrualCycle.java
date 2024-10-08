package com.example.menstrualcyclebot.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "menstrual_cycles")
public class MenstrualCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Генерация уникального идентификатора
    private Long cycleId;            // Уникальный идентификатор цикла

    @ManyToOne
    @JoinColumn(name = "user_chat_id", nullable = false) // Связь с пользователем через chatId
    private User user;              // Связь с сущностью User

    private LocalDate startDate;     // Дата начала менструации
    private int cycleLength;         // Длина цикла (в днях)
    private int periodLength;        // Длительность менструации (в днях)
    private int ovulationDay;        // День овуляции в цикле
    private int ovulationStartDay;   // Начало овуляторного периода
    private int ovulationEndDay;     // Конец овуляторного периода
    private LocalDate ovulationDate; // Дата овуляции

    private LocalDate follicularPhaseStart; // Дата начала фолликулярной фазы
    private LocalDate follicularPhaseEnd;   // Дата окончания фолликулярной фазы (до овуляции)

    private LocalDate lutealPhaseStart;     // Дата начала лютеиновой фазы
    private LocalDate lutealPhaseEnd;       // Дата окончания лютеиновой фазы (до конца цикла)

}
