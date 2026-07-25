package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import com.coremate.backend.weakness.entity.QuestionDiagnosticProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionDiagnosticProfileRepository extends JpaRepository<QuestionDiagnosticProfile, Long> {

    Optional<QuestionDiagnosticProfile> findByQuestionId(Long questionId);

    List<QuestionDiagnosticProfile> findByReviewStatus(DiagnosticReviewStatus reviewStatus);
}
