package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.CommonMistake;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonMistakeRepository extends JpaRepository<CommonMistake, Long> {

    List<CommonMistake> findAllByConcept_ConceptIdAndActiveTrue(Long conceptId);
}
