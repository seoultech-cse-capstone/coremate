package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.ConceptRelation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRelationRepository extends JpaRepository<ConceptRelation, Long> {

    List<ConceptRelation> findAllBySourceConcept_ConceptIdAndActiveTrue(Long sourceConceptId);
}
