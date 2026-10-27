-- =============================================================================
-- V8__create_support_tables.sql
--
-- 고객지원 테이블 두 개를 만든다(#문의하기·버그 신고).
-- - inquiry     : 제목 + 카테고리(하단 네비 탭 5개) + 본문(300자). 관리자 웹에서 답변한다.
-- - bug_report  : 오류 증상 본문만 받는다.
--
-- 두 테이블 모두 작성자(user_id)로 users 를 물리 FK 로 참조한다. 카테고리·상태는 다른 상태
-- 컬럼과 같이 native enum 이다(상수를 추가하면 README 대로 MODIFY COLUMN 을 함께 써야 한다).
-- =============================================================================

CREATE TABLE `inquiry` (
  `inquiry_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `category` enum('HOME','FRIEND','PROBLEM_SET','REVIEW','MYPAGE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `body` varchar(300) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PENDING','ANSWERED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING',
  `answer` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `answered_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`inquiry_id`),
  KEY `idx_inquiry_user_id` (`user_id`),
  KEY `idx_inquiry_status` (`status`),
  CONSTRAINT `fk_inquiry_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bug_report` (
  `bug_report_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `body` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('RECEIVED','RESOLVED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'RECEIVED',
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`bug_report_id`),
  KEY `idx_bug_report_user_id` (`user_id`),
  KEY `idx_bug_report_status` (`status`),
  CONSTRAINT `fk_bug_report_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
