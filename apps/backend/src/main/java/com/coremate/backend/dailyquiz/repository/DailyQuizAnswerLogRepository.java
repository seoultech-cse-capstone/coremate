package com.coremate.backend.dailyquiz.repository;

import com.coremate.backend.dailyquiz.entity.DailyQuizAnswerLog;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyQuizAnswerLogRepository extends JpaRepository<DailyQuizAnswerLog, Long> {

    List<DailyQuizAnswerLog> findByUserIdAndQuizDateOrderBySortOrderAsc(Long userId, LocalDate quizDate);

    void deleteByUserIdAndQuizDate(Long userId, LocalDate quizDate);

    long deleteByQuizDateBefore(LocalDate date);
}
