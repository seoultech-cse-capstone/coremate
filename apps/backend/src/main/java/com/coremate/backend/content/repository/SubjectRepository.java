package com.coremate.backend.content.repository;

import com.coremate.backend.content.entity.Subject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findAllByActiveTrueOrderBySortOrderAsc();

    Optional<Subject> findBySubjectIdAndActiveTrue(Long subjectId);
}
