package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import com.example.menstrualcyclebot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CycleRepository extends JpaRepository<MenstrualCycle, Long> {
    List<MenstrualCycle> findByUser(User user);
}
