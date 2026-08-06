package com.coremate.backend.notification.repository;

import com.coremate.backend.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// 정기 알림 대상자를 뽑는 전용 조회. 알림 발송에만 쓰이는 질의라 UserRepository 를
// 알림 관심사로 채우지 않고 여기에 모았다.
public interface NotificationTargetRepository extends Repository<User, Long> {

    @Query("select u.userId from User u where u.status = com.coremate.backend.user.entity.UserStatus.ACTIVE")
    List<Long> findActiveUserIds();

    // 그날 출석하지 않은 회원. 출석은 오늘의 CS 퀴즈를 풀면 처리된다.
    @Query("select u.userId from User u "
            + "where u.status = com.coremate.backend.user.entity.UserStatus.ACTIVE "
            + "and not exists ("
            + "  select 1 from Attendance a "
            + "  where a.user.userId = u.userId and a.attendanceDate = :date"
            + ")")
    List<Long> findActiveUserIdsWithoutAttendanceOn(@Param("date") LocalDate date);

    // 기준 시각 이후 풀이 회차를 시작한 적이 없는 회원.
    @Query("select u.userId from User u "
            + "where u.status = com.coremate.backend.user.entity.UserStatus.ACTIVE "
            + "and not exists ("
            + "  select 1 from SolveSession s "
            + "  where s.userId = u.userId and s.createdAt >= :since"
            + ")")
    List<Long> findActiveUserIdsWithoutSolveSessionSince(@Param("since") LocalDateTime since);
}
