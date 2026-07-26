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

        if (attendanceReward.getRewardStatus() == RewardStatus.GRANTED) {
            attendance.markRewardClaimed();
        }

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
        if (attendanceReward.getRewardStatus() == RewardStatus.GRANTED) {
            attendance.markRewardClaimed();
        }
        return toRewardResponse(attendance, List.of(attendanceReward));
    }

    public AttendanceRewardResponse getTodayReward(Long userId) {
        return attendanceRepository.findByUser_UserIdAndAttendanceDate(userId, LocalDate.now())
                .map(this::getRewardSummaryFromLogs)
                .orElseGet(() -> emptyRewardResponse(LocalDate.now()));
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
