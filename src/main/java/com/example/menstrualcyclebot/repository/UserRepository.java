package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "cycles")
    Optional<User> findByChatId(Long chatId);

    @Query("SELECT u.chatId FROM User u")
    List<Long> findAllChatIds();

    @Query("SELECT u FROM User u WHERE u.timingOfGeneralRecommendations IS NOT NULL")
    List<User> findAllByTimingOfGeneralRecommendationsIsNotNull();

}
