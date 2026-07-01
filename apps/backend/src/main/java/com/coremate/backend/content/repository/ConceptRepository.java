package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.Concept;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    List<Concept> findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(Long categoryLv2Id);
}
