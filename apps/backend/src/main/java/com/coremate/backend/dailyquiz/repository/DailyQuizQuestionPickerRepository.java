package com.coremate.backend.dailyquiz.repository;

import com.coremate.backend.content.entity.Question;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface DailyQuizQuestionPickerRepository extends Repository<Question, Long> {

    // 운영 승인된 문제 풀이 데이터 중 오늘의 퀴즈 후보를 정책 순서로 가져온다.
    @EntityGraph(attributePaths = {
            "concept",
            "concept.categoryLv2",
            "concept.categoryLv2.categoryLv1",
            "concept.categoryLv2.categoryLv1.subject"
    })
    @Query("""
            select q from Question q
            where q.active = true
              and q.dailyQuizEligible = true
            order by q.questionId asc
            """)
    List<Question> findDailyQuizCandidates(Pageable pageable);
}
