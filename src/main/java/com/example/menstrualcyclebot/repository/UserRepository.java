package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.menstrualCycles WHERE u.chatId = :chatId")
    Optional<User> findUserWithCycles(@Param("chatId") Long chatId);

}
