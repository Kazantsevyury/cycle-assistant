package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "cycles")
    Optional<User> findByChatId(Long chatId);
}
