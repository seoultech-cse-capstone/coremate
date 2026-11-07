-- =============================================================================
-- V13__add_weakness_report_notification_enums.sql
--
-- 약점 분석표 완성 알림(#342)이 쓰는 enum 값을 알림 테이블에 추가한다.
--   notifications.notification_type          + WEAKNESS_REPORT_READY
--   notifications.target_type                + WEAKNESS_REPORT
--   notification_send_logs.notification_type + WEAKNESS_REPORT_READY
--
-- #342 는 NotificationType·NotificationTargetType 에 상수만 추가하고 마이그레이션을 빠뜨렸다. 세 컬럼 모두
-- MySQL native enum 이라 완성 알림을 저장할 때 "Data truncated" 로 실패한다(validate 는 enum 목록을 비교하지
-- 않는다). NotificationSender 가 예외를 로그로만 남겨서 분석표는 정상 완료되고, 알림함 저장과 푸시만 빠진다.
--
-- 기존 값과 정렬·NULL·기본값은 V1 정의 그대로 두고 새 값만 끝에 붙인다. 끝에 붙이면 테이블을 다시 쓰지 않는다.
-- 같은 정의로 다시 실행하면 아무것도 바뀌지 않는다. 두 번째 문장에서 실패하면 첫 문장을 되돌리지 않고
-- 실패 이력만 지운 뒤 다시 배포하면 된다.
-- =============================================================================

ALTER TABLE notifications
    MODIFY COLUMN notification_type ENUM('ATTENDANCE_REMINDER', 'DAILY_QUIZ_OPEN', 'FRIEND_POKE', 'FRIEND_REQUEST_ACCEPTED',
        'FRIEND_REQUEST_RECEIVED', 'LEARNING_REMINDER', 'LEVEL_UP', 'NEW_DEVICE_LOGIN', 'WEAKNESS_REPORT_READY')
        COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY COLUMN target_type ENUM('DAILY_QUIZ', 'FRIEND', 'MYPAGE', 'NONE', 'SETTING', 'WEAKNESS_REPORT')
        COLLATE utf8mb4_unicode_ci DEFAULT NULL;

ALTER TABLE notification_send_logs
    MODIFY COLUMN notification_type ENUM('ATTENDANCE_REMINDER', 'DAILY_QUIZ_OPEN', 'FRIEND_POKE', 'FRIEND_REQUEST_ACCEPTED',
        'FRIEND_REQUEST_RECEIVED', 'LEARNING_REMINDER', 'LEVEL_UP', 'NEW_DEVICE_LOGIN', 'WEAKNESS_REPORT_READY')
        COLLATE utf8mb4_unicode_ci NOT NULL;
