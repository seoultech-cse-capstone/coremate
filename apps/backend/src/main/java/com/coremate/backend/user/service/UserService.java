package com.coremate.backend.user.service;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.auth.entity.SessionRevokeReason;
import com.coremate.backend.auth.service.UserSessionService;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.dto.request.ChangePasswordRequest;
import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.request.UpdatePrivacyRequest;
import com.coremate.backend.user.dto.request.UpdateUserProfileRequest;
import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.progress.service.ProgressService;
import com.coremate.backend.user.dto.response.UserSubjectStatResponse;
import com.coremate.backend.user.dto.response.UserSummaryResponse;
import java.time.LocalDate;
import java.util.List;
import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProgressService progressService;
    private final UserSessionService userSessionService;

    public MyProfileResponse getMyProfile(Long userId) {
        User user = findUser(userId);
        return toMyProfileResponse(user);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);

        // 닉네임 무료 변경 경로 폐쇄 — 닉네임은 닉네임 변경권(상점)으로만 바꾼다. (#202)
        // 학교 정보 등 나머지 프로필 수정은 그대로 두고, 닉네임만 null 로 넘겨 변경되지 않게 한다.
        // if (request.getNickname() != null && !request.getNickname().isBlank()
        //         && !request.getNickname().equals(user.getNickname())) {
        //     if (userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
        //         throw new CoreMateException(ErrorCode.DUPLICATE_NICKNAME);
        //     }
        // }

        user.updateProfile(null, request.getSchoolName(), request.getSchoolEmail());
        return toMyProfileResponse(user);
    }

    @Transactional
    public MyProfileResponse updatePrivacy(Long userId, UpdatePrivacyRequest request) {
        User user = findUser(userId);
        user.updateLearningPublic(request.getLearningPublic());
        return toMyProfileResponse(user);
    }

    @Transactional
    public UserSummaryResponse updateNickname(Long userId, UpdateNicknameRequest request) {
        User user = findUser(userId);

        if (userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
            throw new CoreMateException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(request.getNickname());

        return UserSummaryResponse.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .level(user.getLevel())
                .avatarImageUrl(null)
                .build();
    }

    /**
     * 로그인 상태 비밀번호 변경. 성공하면 지금 기기만 남기고 다른 기기를 끊는다(#250).
     *
     * <p>비밀번호를 바꾸는 흔한 이유가 "누가 내 계정을 쓰는 것 같다"인데, 다른 기기 세션이 살아
     * 있으면 바꿔도 그 기기는 계속 쓸 수 있다. 지금 기기는 방금 현재 비밀번호로 본인임을 확인했으니
     * 남긴다. 전 기기를 끊는 비밀번호 재설정과 다른 점이다.
     */
    @Transactional
    public void changePassword(Long userId, String currentSessionId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new CoreMateException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
        // 같은 트랜잭션이라 비밀번호 저장이 실패하면 해지도 함께 롤백된다.
        userSessionService.revokeOthers(userId, currentSessionId, SessionRevokeReason.PASSWORD_CHANGE);
    }

    /**
     * 마이페이지 육각형 스탯(FR-STAT-01). 과목별 지표는 progress 도메인이 단일 출처라
     * 그대로 재사용하고 화면에 필요한 5개 필드만 추린다.
     *
     * <p>`user_subject_stats` 물리 테이블은 만들지 않는다(2026-08-21 결정). 조회 시점에
     * solve_sessions/solve_logs 를 집계하므로 별도 갱신 처리나 백필이 필요 없다.
     */
    public List<UserSubjectStatResponse> getSubjectStats(Long userId) {
        findUser(userId);
        return progressService.getSubjectProgress(userId).stream()
                .map(progress -> UserSubjectStatResponse.builder()
                        .subjectId(progress.subjectId())
                        .solvedQuestionCount(progress.solvedQuestionCount())
                        .correctQuestionCount(progress.correctQuestionCount())
                        .wrongQuestionCount(progress.wrongQuestionCount())
                        .achievementRate(progress.achievementRate())
                        .build())
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreMateException(ErrorCode.USER_NOT_FOUND));
    }

    private MyProfileResponse toMyProfileResponse(User user) {
        Long userId = user.getUserId();
        LocalDate lastAttendanceDate = attendanceRepository
                .findTopByUser_UserIdOrderByAttendanceDateDesc(userId)
                .map(Attendance::getAttendanceDate)
                .orElse(null);

        return MyProfileResponse.builder()
                .userId(userId)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .level(user.getLevel())
                .totalXp(user.getTotalXp())
                .schoolName(user.getSchoolName())
                .schoolEmail(user.getSchoolEmail())
                .schoolVerified(user.isSchoolVerified())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .attendedToday(LocalDate.now().equals(lastAttendanceDate))
                .attendanceCount(attendanceRepository.countByUser_UserId(userId))
                .lastAttendanceDate(lastAttendanceDate)
                .learningPublic(user.isLearningPublic())
                .hasPassword(user.getPasswordHash() != null)
                .build();
    }
}
