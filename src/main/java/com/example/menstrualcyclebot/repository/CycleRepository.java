package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.MenstrualCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface CycleRepository extends JpaRepository<MenstrualCycle, Long> {

}
