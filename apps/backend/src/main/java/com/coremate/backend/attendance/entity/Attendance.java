package com.coremate.backend.attendance.entity;

import com.coremate.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// 오늘의 퀴즈 완료로 생성되는 사용자별 하루 1회 출석 기록.
@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_attendance_user_date", columnNames = {"user_id", "attendance_date"}),
        indexes = {
                @Index(name = "idx_attendance_user_id", columnList = "user_id"),
                @Index(name = "idx_attendance_daily_quiz_session_id", columnList = "daily_quiz_session_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "daily_quiz_session_id")
    private Long dailyQuizSessionId;

    @Column(name = "reward_claimed", nullable = false)
    private boolean rewardClaimed;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Attendance(User user, LocalDate attendanceDate, Long dailyQuizSessionId) {
        this.user = user;
        this.attendanceDate = attendanceDate;
        this.dailyQuizSessionId = dailyQuizSessionId;
        this.rewardClaimed = false;
    }

    // 보상 코어에서 실제 지급이 끝났을 때만 출석 보상 수령 완료로 표시한다.
    public void markRewardClaimed() {
        this.rewardClaimed = true;
    }
}
