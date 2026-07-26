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
