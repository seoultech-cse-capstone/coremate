package com.coremate.backend.user.service;

import com.coremate.backend.attendance.entity.Attendance;
import com.coremate.backend.attendance.repository.AttendanceRepository;
import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.user.dto.request.ChangePasswordRequest;
import com.coremate.backend.user.dto.request.UpdateNicknameRequest;
import com.coremate.backend.user.dto.request.UpdateUserProfileRequest;
import com.coremate.backend.user.dto.response.MyProfileResponse;
import com.coremate.backend.user.dto.response.UserLearningProgressResponse;
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

    public MyProfileResponse getMyProfile(Long userId) {
        User user = findUser(userId);
        return toMyProfileResponse(user);
    }

    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findUser(userId);

        if (request.getNickname() != null && !request.getNickname().isBlank()
                && !request.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNicknameAndUserIdNot(request.getNickname(), userId)) {
                throw new CoreMateException(ErrorCode.DUPLICATE_NICKNAME);
            }
        }

        user.updateProfile(request.getNickname(), request.getSchoolName(), request.getSchoolEmail());
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

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new CoreMateException(ErrorCode.INVALID_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public UserLearningProgressResponse getLearningProgress(Long userId) {
        User user = findUser(userId);
        return UserLearningProgressResponse.builder()
                .userId(user.getUserId())
                .completedConceptCount(0)
                .completedCategoryMockCount(0)
                .completedSubjectMockCount(0)
                .overallAchievementRate("0.00")
                .build();
    }

    public List<UserSubjectStatResponse> getSubjectStats(Long userId) {
        findUser(userId);
        return List.of();
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
                .build();
    }
}
