-- =============================================================================
-- V12__add_apns_error_fail_reason.sql
--
-- notification_send_logs.fail_reason 에 APNS_ERROR 를 추가한다.
--
-- 아이폰 알림을 FCM 이 아니라 APNs 로 직접 보내게 되면서, 아이폰 발송 실패를 FCM 실패와 구분해 남긴다.
-- 이 컬럼은 MySQL native enum 이라 SendFailReason 에 상수만 추가하면 저장할 때 "Data truncated" 로
-- 실패한다(validate 는 enum 목록을 비교하지 않는다).
--
-- 기존 값과 정렬·기본값은 V1 정의 그대로 두고 새 값만 끝에 붙인다. 끝에 붙이면 테이블을 다시 쓰지 않는다.
-- =============================================================================

ALTER TABLE notification_send_logs
    MODIFY COLUMN fail_reason ENUM('FCM_ERROR', 'NO_TOKEN', 'OPTED_OUT', 'PERMISSION_DENIED', 'TOKEN_EXPIRED', 'APNS_ERROR')
        COLLATE utf8mb4_unicode_ci DEFAULT NULL;
