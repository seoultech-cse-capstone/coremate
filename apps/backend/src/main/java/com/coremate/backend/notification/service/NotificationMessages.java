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
