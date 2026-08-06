/** 백엔드 notification API 응답 타입. 필드는 서버 응답과 1:1로 맞춘다. */

/** 사용자가 켜고 끄는 단위. SYSTEM(계정 보안)은 설정 대상이 아니라 서버가 내려주지 않는다. */
export type NotificationCategory = "DAILY" | "FRIEND" | "LEARNING" | "REWARD";

export type NotificationType =
  | "DAILY_QUIZ_OPEN"
  | "ATTENDANCE_REMINDER"
  | "FRIEND_POKE"
  | "FRIEND_REQUEST_RECEIVED"
  | "FRIEND_REQUEST_ACCEPTED"
  | "LEARNING_REMINDER"
  | "LEVEL_UP"
  | "NOTICE_EVENT"
  | "NEW_DEVICE_LOGIN";

/** 알림을 눌렀을 때 이동할 화면 종류. NONE이면 앱만 열고 홈에 머문다. */
export type NotificationTargetType = "NONE" | "FRIEND" | "DAILY_QUIZ" | "MYPAGE" | "SETTING";

export type AppNotification = {
  notificationId: number;
  category: NotificationCategory | "SYSTEM";
  notificationType: NotificationType;
  title: string;
  body: string;
  targetType: NotificationTargetType;
  targetId: number | null;
  isRead: boolean;
  createdAt: string;
  readAt: string | null;
};

export type NotificationSetting = {
  category: NotificationCategory;
  enabled: boolean;
};

export type UnreadCount = {
  unreadCount: number;
};

export type DevicePlatform = "IOS" | "ANDROID";
