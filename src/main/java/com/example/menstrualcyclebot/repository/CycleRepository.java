package com.example.menstrualcyclebot.repository;

import com.example.menstrualcyclebot.domain.Cycle;
import com.example.menstrualcyclebot.domain.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CycleRepository extends JpaRepository<Cycle, Long> {
    List<Cycle> findAllByStatus(CycleStatus status);
    Optional<Cycle> findFirstByUser_ChatIdAndStatusIn(Long chatId, List<CycleStatus> statuses);
    // Метод для поиска завершенных циклов с ограничением по количеству
    @Query("SELECT c FROM Cycle c WHERE c.user.chatId = :chatId AND c.status = :status ORDER BY c.endDate DESC")
    List<Cycle> findCompletedCyclesByChatIdAndStatus(@Param("chatId") long chatId, @Param("status") CycleStatus status);
    void deleteByCycleId(Long cycleId);

  

    Optional<Cycle> findByUser_ChatIdAndStatus(long chatId, CycleStatus cycleStatus);

    @Query("SELECT c FROM Cycle c WHERE c.user.chatId = :chatId AND c.status = com.example.menstrualcyclebot.domain.CycleStatus.COMPLETED ORDER BY c.endDate DESC")
    List<Cycle> findListByUser_ChatIdAndStatus(@Param("chatId") long chatId);

    @Modifying
    @Transactional
    void deleteByUserChatIdAndEndDate(long chatId, LocalDate endDate);

    Optional<Cycle> findByUserChatIdAndEndDate(long chatId, LocalDate endDate);

    List<Cycle> findByUserChatId(Long userId);
}
