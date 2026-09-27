package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.ConceptRelation;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConceptRelationRepository extends JpaRepository<ConceptRelation, Long> {

    List<ConceptRelation> findAllBySourceConcept_ConceptIdAndActiveTrue(Long sourceConceptId);

    /**
     * 주어진 개념들이 한쪽 끝에 걸린 활성 관계. 약점 분석표의 「먼저 볼 개념」(prerequisite)과
     * 「헷갈리는 개념」(confusing)을 한 번에 읽는다.
     *
     * <p>개념 이름을 함께 쓰므로 양쪽 개념을 fetch join 해 분석표 개념 수만큼 쿼리가 늘지 않게 한다.
     * 방향 해석은 호출부에서 한다 — prerequisite 은 source 가 선수 개념이고, confusing 은 양방향이다.
     */
    @Query("""
            select r from ConceptRelation r
            join fetch r.sourceConcept
            join fetch r.targetConcept
            where r.active = true
              and r.relationType in :relationTypes
              and (r.sourceConcept.conceptId in :conceptIds or r.targetConcept.conceptId in :conceptIds)
            """)
    List<ConceptRelation> findActiveRelations(@Param("relationTypes") Collection<String> relationTypes,
            @Param("conceptIds") Collection<Long> conceptIds);
}
