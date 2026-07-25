package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.entity.ConceptDiagnosticProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptDiagnosticProfileRepository extends JpaRepository<ConceptDiagnosticProfile, Long> {

    Optional<ConceptDiagnosticProfile> findByConceptId(Long conceptId);
}
