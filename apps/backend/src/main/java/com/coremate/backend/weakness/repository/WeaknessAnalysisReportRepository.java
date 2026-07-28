package com.coremate.backend.weakness.repository;

import com.coremate.backend.weakness.dto.type.AnalysisReportStatus;
import com.coremate.backend.weakness.entity.WeaknessAnalysisReport;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeaknessAnalysisReportRepository extends JpaRepository<WeaknessAnalysisReport, Long> {

    // 삭제 상태를 제외한 사용자 분석표 목록을 최신순으로 조회한다.
    List<WeaknessAnalysisReport> findByUserIdAndStatusNotOrderByCreatedAtDesc(Long userId, AnalysisReportStatus status);

    // 소유자 검증을 포함한 단건 조회.
    Optional<WeaknessAnalysisReport> findByAnalysisReportIdAndUserId(Long id, Long userId);

    // 동일 요청 지문+상태로 중복 요청을 판별한다.
    Optional<WeaknessAnalysisReport> findByRequestFingerprintAndStatus(String fingerprint, AnalysisReportStatus status);
}
