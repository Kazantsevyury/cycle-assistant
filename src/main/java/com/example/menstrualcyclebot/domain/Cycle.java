package com.example.menstrualcyclebot.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "menstrual_cycles")
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Генерация уникального идентификатора
    @Column(name = "cycle_id") // Явно указываем имя колонки
    private Long cycleId;            // Уникальный идентификатор цикла

    @ManyToOne
    @JoinColumn(name = "user_chat_id", nullable = false) // Связь с пользователем через chatId
    private User user;              // Связь с сущностью User

    @Column(name = "start_date", nullable = false) // Дата начала менструации
    private LocalDate startDate;

    @Column(name = "cycle_length", nullable = false) // Длина цикла (в днях)
    private int cycleLength;

    @Column(name = "period_length", nullable = false) // Длительность менструации (в днях)
    private int periodLength;

    @Column(name = "ovulation_day") // День овуляции в цикле
    private int ovulationDay;

    @Column(name = "fertile_window_start_day") // Начало фертильного окна
    private Integer fertileWindowStartDay;

    @Column(name = "fertile_window_end_day") // Конец фертильного окна
    private Integer fertileWindowEndDay;

    @Column(name = "ovulation_date") // Дата овуляции
    private LocalDate ovulationDate;

    @Column(name = "follicular_phase_start") // Дата начала фолликулярной фазы
    private LocalDate follicularPhaseStart;

    @Column(name = "follicular_phase_end") // Дата окончания фолликулярной фазы (до овуляции)
    private LocalDate follicularPhaseEnd;

    @Column(name = "luteal_phase_start") // Дата начала лютеиновой фазы
    private LocalDate lutealPhaseStart;

    @Column(name = "luteal_phase_end") // Дата окончания лютеиновой фазы (до конца цикла)
    private LocalDate lutealPhaseEnd;

    @Column(name = "end_date") // Дата завершения цикла, если он завершён
    private LocalDate endDate;

    @Column(name = "delay_days") // Количество дней задержки
    private int delayDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status") // Храним статус как строку в базе
    private CycleStatus status;

    @Column(name = "expected_end_date") // Ожидаемая дата завершения цикла
    private LocalDate expectedEndDate;

    @Column(name = "is_extended") // Был ли цикл продлён
    private boolean isExtended;

    @Column(name = "notes") // Дополнительные заметки
    private String notes;
}
