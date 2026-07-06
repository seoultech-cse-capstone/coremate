package com.coremate.backend.dailyquiz.repository;

import com.coremate.backend.dailyquiz.entity.DailyQuizSet;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyQuizSetRepository extends JpaRepository<DailyQuizSet, Long> {

    @EntityGraph(attributePaths = {
            "question",
            "question.concept",
            "question.concept.categoryLv2",
            "question.concept.categoryLv2.categoryLv1",
            "question.concept.categoryLv2.categoryLv1.subject",
            "subject"
    })
    List<DailyQuizSet> findByQuizDateOrderBySortOrderAsc(LocalDate quizDate);

    Optional<DailyQuizSet> findFirstByQuizDateOrderBySortOrderAsc(LocalDate quizDate);
}
