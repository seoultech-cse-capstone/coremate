package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.Concept;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptRepository extends JpaRepository<Concept, Long> {

    List<Concept> findAllByCategoryLv2_CategoryLv2IdAndActiveTrueOrderBySortOrderAsc(Long categoryLv2Id);

    Optional<Concept> findByConceptIdAndActiveTrue(Long conceptId);

    List<Concept> findAllByCategoryLv2_CategoryLv1_CategoryLv1IdAndActiveTrue(Long categoryLv1Id);

    List<Concept> findAllByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(Long subjectId);

    // 복습탭 개념 카운팅의 "총 개념": 전체 활성 개념 수
    long countByActiveTrue();

    // 과목별 진행률의 "전체 개념 수": 해당 과목 하위의 전체 활성 개념 수
    long countByCategoryLv2_CategoryLv1_Subject_SubjectIdAndActiveTrue(Long subjectId);
}
