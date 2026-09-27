package com.coremate.backend.attendance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 출석 보상 정산이 끝난 회차가 attendance.reward_claimed 로 저장되는지 검증한다.
 *
 * <p>XP 지급이 벌크 update 로 영속성 컨텍스트를 비우기 때문에 이 시점의 attendance 는 준영속이다.
 * 예전에는 markRewardClaimed() 만 부르고 끝내서 더티 체킹이 일어나지 않았고, 응답은 true 인데
 * DB 는 false 로 남아 출석 화면이 계속 "아직 지급되지 않았어요"와 수령 버튼을 띄웠다.
 * 그래서 여기서는 저장이 실제로 요청되는지를 본다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("출석 보상 수령 플래그")
class AttendanceRewardClaimedTest {

    private static final Long USER_ID = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 24);

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RewardService rewardService;

    @InjectMocks private AttendanceService attendanceService;

    @Test
    @DisplayName("오늘의 퀴즈로 출석 보상을 처음 받으면 수령 완료로 저장된다")
    void marksClaimedAfterDailyQuizGrant() {
        givenTodayAttendance();
        givenPolicyReward(RewardType.DAILY_QUIZ_COMPLETE, granted());
        givenPolicyReward(RewardType.ATTENDANCE_REWARD, granted());

        attendanceService.processDailyQuizCompletion(USER_ID, TODAY, null, 100L);

        assertThat(savedAttendance().isRewardClaimed()).isTrue();
    }

    @Test
    @DisplayName("이미 받은 보상이면 재수령 요청에서도 수령 완료로 정정된다")
    void marksClaimedWhenAlreadyRewarded() {
        Attendance attendance = givenStoredAttendance();
        givenPolicyReward(RewardType.ATTENDANCE_REWARD, skipped(RewardSkipReason.ALREADY_REWARDED));

        attendanceService.claimReward(USER_ID, 1L);

        assertThat(savedAttendance().isRewardClaimed()).isTrue();
        assertThat(attendance.isRewardClaimed()).isTrue();
    }

    @Test
    @DisplayName("정답률 미달처럼 아직 정산되지 않은 사유면 수령 완료로 두지 않는다")
    void keepsUnclaimedWhenNotSettled() {
        givenStoredAttendance();
        givenPolicyReward(RewardType.ATTENDANCE_REWARD, skipped(RewardSkipReason.DAILY_LIMIT_EXCEEDED));

        attendanceService.claimReward(USER_ID, 1L);

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    // ---------- fixtures ----------

    private Attendance givenTodayAttendance() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        Attendance attendance = new Attendance(user, TODAY, null);
        when(attendanceRepository.findByUser_UserIdAndAttendanceDate(USER_ID, TODAY))
                .thenReturn(Optional.of(attendance));
        return attendance;
    }

    private Attendance givenStoredAttendance() {
        User user = mock(User.class);
        Attendance attendance = new Attendance(user, TODAY, null);
        when(attendanceRepository.findByAttendanceIdAndUser_UserId(1L, USER_ID))
                .thenReturn(Optional.of(attendance));
        return attendance;
    }

    private void givenPolicyReward(RewardType rewardType, RewardLog log) {
        when(rewardService.grantPolicyReward(
                eq(USER_ID), eq(rewardType), any(), any(), any(), anyLong(), any()))
                .thenReturn(log);
    }

    private Attendance savedAttendance() {
        ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository).save(captor.capture());
        return captor.getValue();
    }

    private RewardLog granted() {
        RewardLog log = mock(RewardLog.class);
        when(log.getRewardStatus()).thenReturn(RewardStatus.GRANTED);
        return log;
    }

    private RewardLog skipped(RewardSkipReason reason) {
        RewardLog log = mock(RewardLog.class);
        when(log.getRewardStatus()).thenReturn(RewardStatus.SKIPPED);
        when(log.getSkipReason()).thenReturn(reason);
        return log;
    }
}
