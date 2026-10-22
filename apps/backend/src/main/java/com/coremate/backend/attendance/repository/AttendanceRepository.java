package com.coremate.backend.attendance.repository;

import com.coremate.backend.attendance.entity.Attendance;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByUser_UserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    long countByUser_UserId(Long userId);

    // 친구 목록용 배치(#322): 주어진 사용자들 중 그 날짜에 출석한 사용자 ID 만 추린다.
    @Query("select a.user.userId from Attendance a "
            + "where a.user.userId in :userIds and a.attendanceDate = :date")
    List<Long> findUserIdsAttendedOn(@Param("userIds") Collection<Long> userIds,
            @Param("date") LocalDate date);

    // 친구 목록용 배치(#322): 사용자별 누적 출석 수를 한 번에 센다.
    @Query("select a.user.userId as userId, count(a) as attendanceCount from Attendance a "
            + "where a.user.userId in :userIds group by a.user.userId")
    List<AttendanceCountByUser> countByUserIds(@Param("userIds") Collection<Long> userIds);

    interface AttendanceCountByUser {
        Long getUserId();
        long getAttendanceCount();
    }

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
