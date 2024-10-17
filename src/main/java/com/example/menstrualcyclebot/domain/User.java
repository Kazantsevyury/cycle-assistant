package com.example.menstrualcyclebot.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
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

    @Column(name = "name", length = 100)
    private String name; // Имя пользователя

    @Column(name = "salutation", length = 20)
    private String salutation; // Обращение к пользователю

    @Column(name = "birth_date")
    private LocalDate birthDate; // День рождения пользователя

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<Cycle> cycles; // Список циклов, связанных с пользователем
}