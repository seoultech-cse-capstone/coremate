package com.coremate.backend.notification.repository;

import com.coremate.backend.notification.entity.NotificationSendLog;
import com.coremate.backend.notification.entity.type.NotificationType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationSendLogRepository extends JpaRepository<NotificationSendLog, Long> {

    // 정기 알림의 당일 중복 발송 판정용(NFR-DATA-09).
    // 건너뜀(SKIPPED)도 발송을 시도한 것으로 보고 제외한다. 수신 거부한 사용자에게
    // 실행할 때마다 로그만 쌓이는 것을 막는다.
    @Query("select distinct l.userId from NotificationSendLog l "
            + "where l.notificationType = :type and l.sentAt >= :start and l.sentAt < :end")
    List<Long> findUserIdsSentBetween(
            @Param("type") NotificationType type,
            @Param("start") LocalDateTime startInclusive,
            @Param("end") LocalDateTime endExclusive);

    // 회원 탈퇴. 기기 토큰과 함께 그 기기로 보낸 기록도 지운다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from NotificationSendLog l where l.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
