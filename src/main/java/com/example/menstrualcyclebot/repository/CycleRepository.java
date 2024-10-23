package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {
    List<Cycle> findAllByStatus(CycleStatus status);
    Optional<Cycle> findFirstByUser_ChatIdAndStatus(long chatId, CycleStatus status);
    Optional<Cycle> findFirstByUser_ChatIdAndStatusIn(Long chatId, List<CycleStatus> statuses);

}
