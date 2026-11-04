package com.coremate.backend.auth.service;

import com.coremate.backend.auth.repository.UserSessionRepository;
import com.coremate.backend.bookmark.repository.BookmarkRepository;
import com.coremate.backend.friend.repository.FriendPokeRepository;
import com.coremate.backend.friend.repository.FriendRepository;
import com.coremate.backend.notification.repository.DeviceTokenRepository;
import com.coremate.backend.notification.repository.NotificationRepository;
import com.coremate.backend.notification.repository.NotificationSendLogRepository;
import com.coremate.backend.notification.repository.UserNotificationSettingRepository;
import com.coremate.backend.support.repository.BugReportRepository;
import com.coremate.backend.support.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 탈퇴한 회원의 데이터 가운데 개인을 알아볼 수 있거나 더 쓸 일이 없는 것을 지운다.
 *
 * <p>지우는 범위는 계정 삭제 안내 페이지와 개인정보 처리방침 제3조에 적은 것과 같다. 회원 행은 풀이·보상
 * 기록이 참조하고 있어 지우지 않고 익명화하며({@code User#withdraw}), 풀이·출석·경험치·코인·보상 기록은
 * 누구 것인지 알 수 없는 상태로 통계에만 쓴다. 탈퇴 트랜잭션 안에서 함께 지워야 반쯤 지워진 채로 남지 않는다.
 */
@Component
@RequiredArgsConstructor
public class WithdrawnUserDataCleaner {

    private final UserSessionRepository userSessionRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationSendLogRepository notificationSendLogRepository;
    private final NotificationRepository notificationRepository;
    private final UserNotificationSettingRepository userNotificationSettingRepository;
    private final BookmarkRepository bookmarkRepository;
    private final InquiryRepository inquiryRepository;
    private final BugReportRepository bugReportRepository;
    private final FriendRepository friendRepository;
    private final FriendPokeRepository friendPokeRepository;

    public void deleteAll(Long userId) {
        // 로그인 기기와 알림
        userSessionRepository.deleteAllByUserId(userId);
        deviceTokenRepository.deleteAllByUserId(userId);
        notificationSendLogRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllOfOrAboutUser(userId);
        userNotificationSettingRepository.deleteAllByUserId(userId);

        // 직접 적은 글: 즐겨찾기 메모, 문의, 버그 신고
        bookmarkRepository.deleteAllByUserId(userId);
        inquiryRepository.deleteAllByUserId(userId);
        bugReportRepository.deleteAllByUserId(userId);

        // 친구 관계·요청·차단, 콕 찌르기
        friendRepository.deleteAllInvolving(userId);
        friendPokeRepository.deleteAllInvolving(userId);
    }
}
