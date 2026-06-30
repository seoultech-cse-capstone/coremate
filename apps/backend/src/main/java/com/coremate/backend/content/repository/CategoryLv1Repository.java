package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.CategoryLv1;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryLv1Repository extends JpaRepository<CategoryLv1, Long> {

    List<CategoryLv1> findAllBySubject_SubjectIdAndActiveTrueOrderBySortOrderAsc(Long subjectId);

    Optional<CategoryLv1> findByCategoryLv1IdAndActiveTrue(Long categoryLv1Id);
}
