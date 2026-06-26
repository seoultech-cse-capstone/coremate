package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.CategoryLv2;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryLv2Repository extends JpaRepository<CategoryLv2, Long> {

    List<CategoryLv2> findAllByCategoryLv1_CategoryLv1IdAndActiveTrueOrderBySortOrderAsc(Long categoryLv1Id);
}
