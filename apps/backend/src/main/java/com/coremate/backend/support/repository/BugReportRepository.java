package com.coremate.backend.support.repository;

import com.coremate.backend.support.entity.BugReport;
import com.coremate.backend.support.entity.type.BugReportStatus;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BugReportRepository extends JpaRepository<BugReport, Long> {

    @EntityGraph(attributePaths = "user")
    List<BugReport> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<BugReport> findByStatusOrderByCreatedAtDesc(BugReportStatus status);

    // 회원 탈퇴.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from BugReport r where r.user.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
