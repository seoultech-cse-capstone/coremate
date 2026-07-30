package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import com.coremate.backend.weakness.entity.QuestionDiagnosticProfile;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionDiagnosticProfileRepository extends JpaRepository<QuestionDiagnosticProfile, Long> {

    Optional<QuestionDiagnosticProfile> findByQuestionId(Long questionId);

    List<QuestionDiagnosticProfile> findByReviewStatus(DiagnosticReviewStatus reviewStatus);

    /** 승인된 프로필만 사용한다(문서: 승인분만 AI 입력·화면 사용). */
    List<QuestionDiagnosticProfile> findByQuestionIdInAndReviewStatusAndDiagnosticProfileVersion(
            Collection<Long> questionIds, DiagnosticReviewStatus reviewStatus, String diagnosticProfileVersion);
}
