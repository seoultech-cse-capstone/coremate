package com.coremate.backend.support.repository;

import com.coremate.backend.support.entity.BugReport;
import com.coremate.backend.support.entity.type.BugReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BugReportRepository extends JpaRepository<BugReport, Long> {

    @EntityGraph(attributePaths = "user")
    List<BugReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<BugReport> findByStatusOrderByCreatedAtDesc(BugReportStatus status);
}
