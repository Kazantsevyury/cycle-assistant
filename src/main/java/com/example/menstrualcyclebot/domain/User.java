package com.example.menstrualcyclebot.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;  // chatId используется как уникальный идентификатор пользователя

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "setup_complete", nullable = false)
    private boolean setupComplete = false;  // Флаг, указывающий, прошел ли пользователь начальную настройку

    @Column(name = "name", length = 50)
    private String name; // Имя пользователя

    @Column(name = "salutation", length = 50)
    private String salutation; // Обращение к пользователю

    @Column(name = "birth_date")
    private LocalDate birthDate; // День рождения пользователя

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Cycle> cycles; // Список циклов, связанных с пользователем

    @Column(name = "time_zone")
    private ZoneId timeZone;

    // General Recommendations
    @Column(name = "timing_of_fertility_general_recommendations")
    private String timingOfGeneralRecommendations;

    @Column(name = "physical_activity_enabled_notification", nullable = false)
    private boolean physicalActivityEnabled = true;

    @Column(name = "nutrition_enabled_notification", nullable = false)
    private boolean nutritionEnabled = true;

    @Column(name = "work_productivity_notification", nullable = false)
    private boolean workProductivityNotification = true;

    @Column(name = "relationships_communication_notification", nullable = false)
    private boolean relationshipsCommunicationNotification = true;

    @Column(name = "care_notification", nullable = false)
    private boolean careNotification = true;

    @Column(name = "emotional_wellbeing_notification", nullable = false)
    private boolean emotionalWellbeingNotification = true;

    @Column(name = "sex_notification", nullable = false)
    private boolean sexNotification= true;

    // fertility_window
    @Column(name = "fertility_window_notification_enabled", nullable = false)
    private boolean fertilityWindowNotificationEnabled = true; // Fertility window notifications - boolean

    @Column(name = "timing_of_fertility_window_notifications")
    private String timingOfFertilityWindowNotifications;  // Timing of Fertility window notifications

    @Column(name = "days_before_fertility_window_notifications")
    private int daysBeforeFertilityWindowNotifications;  // How many days before Fertility window notifications

    // menstruation_start_notification
    @Column(name = "menstruation_start_notification_enabled", nullable = false)
    private boolean menstruationStartNotificationEnabled = true; // Menstruation start notifications - boolean

    @Column(name = "timing_of_menstruation_start_notifications")
    private String timingOfMenstruationStartNotifications;  // Timing of Menstruation start notifications

    @Column(name = "days_before_menstruation_start_notifications")
    private int daysBeforeMenstruationStartNotifications;  // How many days before Menstruation start notifications
}