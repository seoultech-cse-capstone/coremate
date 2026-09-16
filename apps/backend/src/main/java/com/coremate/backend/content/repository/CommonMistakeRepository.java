package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.CommonMistake;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonMistakeRepository extends JpaRepository<CommonMistake, Long> {

    List<CommonMistake> findAllByConcept_ConceptIdAndActiveTrue(Long conceptId);

    /** 오개념 코드 → 제목. 분석표가 반복된 오개념을 제목으로 보여 줄 때 쓴다. */
    List<CommonMistake> findAllByMistakeCodeIn(Collection<String> mistakeCodes);
}
