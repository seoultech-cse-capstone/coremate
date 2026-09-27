package com.coremate.backend.dailyquiz.repository;

import com.coremate.backend.content.entity.Question;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DailyQuizQuestionPickerRepository extends Repository<Question, Long> {

    // 오늘의 퀴즈 후보 ID 전체를 가져온다. 일부만 잘라오면 questionId가 앞선 과목에서만 출제되므로
    // 후보 선별은 서비스에서 날짜 기준으로 수행한다.
    @Query("""
            select q.questionId from Question q
            where q.active = true
              and q.dailyQuizEligible = true
            order by q.questionId asc
            """)
    List<Long> findDailyQuizCandidateIds();

    // 선별된 문항만 출제에 필요한 연관 엔티티와 함께 조회한다.
    @EntityGraph(attributePaths = {
            "concept",
            "concept.categoryLv2",
            "concept.categoryLv2.categoryLv1",
            "concept.categoryLv2.categoryLv1.subject"
    })
    @Query("select q from Question q where q.questionId in :questionIds")
    List<Question> findAllForDailyQuizByIds(@Param("questionIds") List<Long> questionIds);
}
