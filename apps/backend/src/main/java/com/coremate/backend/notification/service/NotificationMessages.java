package com.coremate.backend.notification.service;

import com.coremate.backend.notification.entity.type.NotificationTargetType;
import com.coremate.backend.notification.entity.type.NotificationType;

// 알림 문구를 한곳에 모은다. 요구사항에는 유형과 트리거만 정의돼 있고 문구는 구현에서 정한다.
// 문구는 데모와 사용 반응에 따라 자주 바뀌므로, 도메인 서비스에 흩어 두지 않고 여기서만 고친다.
public final class NotificationMessages {

    private NotificationMessages() {
    }

    public static NotificationCommand friendPoke(Long receiverId, String senderNickname, Long senderId) {
        return NotificationCommand.of(
                receiverId,
                NotificationType.FRIEND_POKE,
                senderNickname + "님이 콕 찔렀어요",
                "같이 공부하러 갈까요?",
                NotificationTargetType.FRIEND,
                senderId
        );
    }

    public static NotificationCommand friendRequestReceived(Long receiverId, String requesterNickname, Long requesterId) {
        return NotificationCommand.of(
                receiverId,
                NotificationType.FRIEND_REQUEST_RECEIVED,
                "새 친구 요청",
                requesterNickname + "님이 친구 요청을 보냈어요",
                NotificationTargetType.FRIEND,
                requesterId
        );
    }

    public static NotificationCommand friendRequestAccepted(Long requesterId, String receiverNickname, Long receiverId) {
        return NotificationCommand.of(
                requesterId,
                NotificationType.FRIEND_REQUEST_ACCEPTED,
                "친구가 되었어요",
                receiverNickname + "님이 친구 요청을 수락했어요",
                NotificationTargetType.FRIEND,
                receiverId
        );
    }

    public static NotificationCommand levelUp(Long userId, int level) {
        return NotificationCommand.of(
                userId,
                NotificationType.LEVEL_UP,
                "레벨 " + level + " 달성!",
                "축하해요. 마이페이지에서 새 레벨을 확인해 보세요",
                NotificationTargetType.MYPAGE,
                (long) level
        );
    }

    public static NotificationCommand dailyQuizOpen(Long userId) {
        return NotificationCommand.of(
                userId,
                NotificationType.DAILY_QUIZ_OPEN,
                "오늘의 CS 퀴즈가 나왔어요",
                "6문제 풀고 출석까지 한 번에 끝내볼까요?",
                NotificationTargetType.DAILY_QUIZ,
                null
        );
    }

    public static NotificationCommand attendanceReminder(Long userId) {
        return NotificationCommand.of(
                userId,
                NotificationType.ATTENDANCE_REMINDER,
                "오늘 아직 출석 전이에요",
                "오늘의 퀴즈를 풀면 출석이 완료됩니다",
                NotificationTargetType.DAILY_QUIZ,
                null
        );
    }

    // 이동 대상을 두지 않는다. 알림을 누르면 앱이 열리면서 홈으로 들어가므로
    // FR-NOTI-07 이 말하는 "홈 화면으로 이동"이 그대로 성립한다.
    public static NotificationCommand learningReminder(Long userId) {
        return NotificationCommand.of(
                userId,
                NotificationType.LEARNING_REMINDER,
                "며칠 쉬었네요",
                "가볍게 한 문제집만 풀어볼까요?",
                NotificationTargetType.NONE,
                null
        );
    }

    // 백그라운드 분석이 끝나면 보낸다(#323). 누르면 해당 분석표 상세로 이동하도록 target_id 에 리포트 ID 를 싣는다.
    public static NotificationCommand weaknessReportReady(Long userId, Long reportId) {
        return NotificationCommand.of(
                userId,
                NotificationType.WEAKNESS_REPORT_READY,
                "약점 분석표가 완성됐어요",
                "지금 바로 확인해 보세요",
                NotificationTargetType.WEAKNESS_REPORT,
                reportId
        );
    }

    public static NotificationCommand newDeviceLogin(Long userId, String deviceName) {
        return NotificationCommand.of(
                userId,
                NotificationType.NEW_DEVICE_LOGIN,
                "새로운 기기에서 로그인",
                deviceName + "에서 로그인했어요. 본인이 아니라면 기기 관리에서 로그아웃하세요",
                NotificationTargetType.SETTING,
                null
        );
    }
}
