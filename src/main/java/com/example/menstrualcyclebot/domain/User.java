package com.example.menstrualcyclebot.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table(name = "users")
public class User {

    @Id
    private Long chatId;  // chatId используется как уникальный идентификатор пользователя

    private String username;

    private boolean setupComplete = false;  // Флаг, указывающий, прошел ли пользователь начальную настройку

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MenstrualCycle> menstrualCycles; // Список циклов, связанных с пользователем
}
