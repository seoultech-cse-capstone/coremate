package com.coremate.backend.attendance.service;

import com.coremate.backend.attendance.dto.response.AttendanceCalendarResponse;
import com.coremate.backend.attendance.dto.response.AttendanceResponse;
import com.coremate.backend.attendance.dto.response.AttendanceRewardResponse;
import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.reward.dto.response.RewardLogResponse;
import com.coremate.backend.reward.entity.RewardLog;
import com.coremate.backend.reward.entity.type.RewardSkipReason;
import com.coremate.backend.reward.entity.type.RewardSource;
import com.coremate.backend.reward.entity.type.RewardStatus;
import com.coremate.backend.reward.entity.type.RewardTargetType;
import com.coremate.backend.reward.entity.type.RewardType;
import com.coremate.backend.reward.service.RewardService;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final Set<RewardType> DAILY_QUIZ_REWARD_TYPES = EnumSet.of(
            RewardType.DAILY_QUIZ_COMPLETE,
            RewardType.DAILY_QUIZ_REWARD,
            RewardType.DAILY_QUIZ_ATTENDANCE,
            RewardType.ATTENDANCE_REWARD
    );

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final RewardService rewardService;

    // 홈/친구 화면에서 오늘 출석 여부를 빠르게 판단할 때 사용한다.
    public boolean hasAttended(Long userId, LocalDate attendanceDate) {
        findUser(userId);
        return attendanceRepository.existsByUser_UserIdAndAttendanceDate(userId, attendanceDate);
    }

    // 오늘의 퀴즈 완료 후 자동으로 출석을 만들고 출석 보상을 1회만 지급한다.
    @Transactional
    public AttendanceRewardResponse processDailyQuizCompletion(
            Long userId,
            LocalDate attendanceDate,
            Long dailyQuizSessionId,
            Long dailyQuizSetId
    ) {
        User user = findUser(userId);
        Attendance attendance = attendanceRepository.findByUser_UserIdAndAttendanceDate(userId, attendanceDate)
                .orElseGet(() -> attendanceRepository.save(new Attendance(user, attendanceDate, dailyQuizSessionId)));

        if (attendance.isRewardClaimed()) {
            return getRewardSummaryFromLogs(attendance);
        }

        RewardLog dailyQuizReward = rewardService.grantPolicyReward(
                userId,
                RewardType.DAILY_QUIZ_COMPLETE,
                RewardSource.DAILY_QUIZ,
                dailyQuizSetId,
                RewardTargetType.DAILY,
                toDailyTargetId(attendanceDate),
                dailyQuizSessionId
        );
        RewardLog attendanceReward = grantAttendanceReward(userId, attendance, dailyQuizSessionId);

        markRewardClaimedIfSettled(attendance, attendanceReward);

        return toRewardResponse(attendance, List.of(dailyQuizReward, attendanceReward));
    }

    public AttendanceResponse getTodayAttendance(Long userId) {
        return attendanceRepository.findByUser_UserIdAndAttendanceDate(userId, LocalDate.now())
                .map(this::toAttendanceResponse)
                .orElseThrow(() -> new CoreMateException(ErrorCode.ATTENDANCE_NOT_FOUND));
    }

    public AttendanceCalendarResponse getCalendar(Long userId, String month) {
        findUser(userId);
        YearMonth targetMonth = parseMonth(month);
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();

        List<LocalDate> attendedDates = attendanceRepository
                .findByUser_UserIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(userId, startDate, endDate)
                .stream()
                .map(Attendance::getAttendanceDate)
                .toList();

        return AttendanceCalendarResponse.builder()
                .userId(userId)
                .month(targetMonth.format(MONTH_FORMATTER))
                .attendedDates(attendedDates)
                .totalCount(attendedDates.size())
                .build();
    }

    // 출석 기록은 있지만 보상만 실패/미지급 상태인 경우 수동 재시도할 수 있게 둔다.
    @Transactional
    public AttendanceRewardResponse claimReward(Long userId, Long attendanceId) {
        Attendance attendance = attendanceRepository.findByAttendanceIdAndUser_UserId(attendanceId, userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.ATTENDANCE_NOT_FOUND));

        if (attendance.isRewardClaimed()) {
            return getRewardSummaryFromLogs(attendance);
        }

        RewardLog attendanceReward = grantAttendanceReward(userId, attendance, attendance.getDailyQuizSessionId());
        markRewardClaimedIfSettled(attendance, attendanceReward);
        return toRewardResponse(attendance, List.of(attendanceReward));
    }

    public AttendanceRewardResponse getTodayReward(Long userId) {
        return attendanceRepository.findByUser_UserIdAndAttendanceDate(userId, LocalDate.now())
                .map(this::getRewardSummaryFromLogs)
                .orElseGet(() -> emptyRewardResponse(LocalDate.now()));
    }

    /**
     * 출석 보상이 정산된 회차를 "수령 완료"로 표시한다.
     *
     * <p>이미 받은 건(ALREADY_REWARDED)도 완료로 본다. 그렇지 않으면 지급은 끝났는데
     * 플래그만 false 로 남아, 출석 화면이 계속 "아직 지급되지 않았어요"와 수령 버튼을 띄운다.
     *
     * <p>save 를 명시적으로 부르는 이유 — XP 지급이 벌크 update 로 영속성 컨텍스트를 비우기
     * 때문에(RewardUserProgressRepository.updateLearningProgress 의 clearAutomatically)
     * 이 시점의 attendance 는 준영속이다. 변경만 해 두면 더티 체킹이 일어나지 않아
     * UPDATE 가 나가지 않고, 응답만 true 로 보이고 DB 는 false 로 남는다.
     */
    private void markRewardClaimedIfSettled(Attendance attendance, RewardLog attendanceReward) {
        boolean settled = attendanceReward.getRewardStatus() == RewardStatus.GRANTED
                || attendanceReward.getSkipReason() == RewardSkipReason.ALREADY_REWARDED;
        if (!settled) {
            return;
        }
        attendance.markRewardClaimed();
        attendanceRepository.save(attendance);
    }

    private RewardLog grantAttendanceReward(Long userId, Attendance attendance, Long dailyQuizSessionId) {
        return rewardService.grantPolicyReward(
                userId,
                RewardType.ATTENDANCE_REWARD,
                RewardSource.ATTENDANCE,
                attendance.getAttendanceId(),
                RewardTargetType.DAILY,
                toDailyTargetId(attendance.getAttendanceDate()),
                dailyQuizSessionId
        );
    }

    private AttendanceRewardResponse getRewardSummaryFromLogs(Attendance attendance) {
        List<RewardLogResponse> logs = rewardService.getRewardLogs(attendance.getUser().getUserId())
                .stream()
                .filter(log -> log.createdAt() != null)
                .filter(log -> DAILY_QUIZ_REWARD_TYPES.contains(log.rewardType()))
                .filter(log -> log.rewardStatus() == RewardStatus.GRANTED)
                .filter(log -> attendance.getAttendanceDate().equals(log.createdAt().toLocalDate()))
                .toList();

        int xpAmount = logs.stream().mapToInt(log -> log.xpAmount() == null ? 0 : log.xpAmount()).sum();
        int coinAmount = logs.stream().mapToInt(log -> log.coinAmount() == null ? 0 : log.coinAmount()).sum();

        return AttendanceRewardResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .coinAmount(coinAmount)
                .xpAmount(xpAmount)
                .rewardClaimed(attendance.isRewardClaimed())
                .rewardDate(attendance.getAttendanceDate())
                .build();
    }

    private AttendanceRewardResponse toRewardResponse(Attendance attendance, List<RewardLog> logs) {
        int xpAmount = logs.stream()
                .filter(log -> log.getRewardStatus() == RewardStatus.GRANTED)
                .mapToInt(RewardLog::getXpAmount)
                .sum();
        int coinAmount = logs.stream()
                .filter(log -> log.getRewardStatus() == RewardStatus.GRANTED)
                .mapToInt(RewardLog::getCoinAmount)
                .sum();

        return AttendanceRewardResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .coinAmount(coinAmount)
                .xpAmount(xpAmount)
                .rewardClaimed(attendance.isRewardClaimed())
                .rewardDate(attendance.getAttendanceDate())
                .build();
    }

    private AttendanceRewardResponse emptyRewardResponse(LocalDate rewardDate) {
        return AttendanceRewardResponse.builder()
                .attendanceId(null)
                .coinAmount(0)
                .xpAmount(0)
                .rewardClaimed(false)
                .rewardDate(rewardDate)
                .build();
    }

    private AttendanceResponse toAttendanceResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .userId(attendance.getUser().getUserId())
                .attendanceDate(attendance.getAttendanceDate())
                .rewardClaimed(attendance.isRewardClaimed())
                .createdAt(attendance.getCreatedAt())
                .build();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month, MONTH_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
    }

    private Long toDailyTargetId(LocalDate date) {
        return Long.parseLong(date.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
