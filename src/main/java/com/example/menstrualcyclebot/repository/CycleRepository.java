package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {

}
