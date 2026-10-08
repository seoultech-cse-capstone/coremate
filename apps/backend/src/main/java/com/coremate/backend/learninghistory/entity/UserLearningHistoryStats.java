package com.coremate.backend.learninghistory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 친구 상세 프로필 Learning History 섹션에 표시할 사용자 단위 누적 지표.
 * 사용자당 1행으로 유지되며, 풀이 완료 시점에 누적 갱신된다.
 */
@Entity
@Table(name = "user_learning_history_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLearningHistoryStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_learning_history_stat_id")
    private Long userLearningHistoryStatId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** ① 모의고사(과목/대분류/중분류) 총 풀이 시간 합(초). 개념 문제집 제외 */
    @Column(name = "total_mock_solve_time_sec", nullable = false)
    private int totalMockSolveTimeSec;

    /** ② 개념 문제집에서 맞힌 서로 다른(distinct) 문제 수 */
    @Column(name = "concept_correct_question_count", nullable = false)
    private int conceptCorrectQuestionCount;

    /** ③ PASS(전 문제 정답)로 통과한 서로 다른 개념 수 */
    @Column(name = "passed_concept_count", nullable = false)
    private int passedConceptCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private UserLearningHistoryStats(Long userId) {
        this.userId = userId;
        this.totalMockSolveTimeSec = 0;
        this.conceptCorrectQuestionCount = 0;
        this.passedConceptCount = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public static UserLearningHistoryStats initFor(Long userId) {
        return new UserLearningHistoryStats(userId);
    }

    public void addMockSolveTime(int seconds) {
        this.totalMockSolveTimeSec += seconds;
        touch();
    }

    public void addConceptCorrectQuestions(int count) {
        this.conceptCorrectQuestionCount += count;
        touch();
    }

    public void increasePassedConcept() {
        this.passedConceptCount += 1;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
