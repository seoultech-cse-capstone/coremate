package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.dto.type.DiagnosticReviewStatus;
import com.coremate.backend.weakness.entity.ConceptDiagnosticProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptDiagnosticProfileRepository extends JpaRepository<ConceptDiagnosticProfile, Long> {

    Optional<ConceptDiagnosticProfile> findByConceptId(Long conceptId);

    /** 승인된 프로필만 사용한다(문서: 승인분만 AI 입력·화면 사용). */
    Optional<ConceptDiagnosticProfile> findByConceptIdAndReviewStatusAndDiagnosticProfileVersion(
            Long conceptId, DiagnosticReviewStatus reviewStatus, String diagnosticProfileVersion);
}
