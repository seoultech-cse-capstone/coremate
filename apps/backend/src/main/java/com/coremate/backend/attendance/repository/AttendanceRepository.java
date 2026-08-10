package com.coremate.backend.attendance.repository;

import com.coremate.backend.attendance.entity.Attendance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByUser_UserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    long countByUser_UserId(Long userId);

    // 마이페이지 "최근 출석" 표시용: 가장 최근 출석 1건
    Optional<Attendance> findTopByUser_UserIdOrderByAttendanceDateDesc(Long userId);

    @EntityGraph(attributePaths = "user")
    Optional<Attendance> findByUser_UserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    @EntityGraph(attributePaths = "user")
    Optional<Attendance> findByAttendanceIdAndUser_UserId(Long attendanceId, Long userId);

    List<Attendance> findByUser_UserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
