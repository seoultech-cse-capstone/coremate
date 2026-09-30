-- =============================================================================
-- V1__baseline.sql
--
-- Flyway 도입 시점(2026-09-30)의 스키마 전체다. 그때까지 스키마를 만들던 JPA 엔티티
-- (ddl-auto)가 빈 MySQL 8.4 에 만든 스키마를 mysqldump --no-data 로 떠서, 클라이언트
-- 문자셋 주석과 AUTO_INCREMENT 값만 지웠다.
--
-- 빈 DB 에서만 실행된다. 이미 테이블이 있던 DB(운영·기존 로컬)는 이 파일을 실행하지 않고
-- "적용됨"으로 기록만 한다(spring.flyway.baseline-on-migrate).
--
-- ⚠️ 적용된 마이그레이션은 고치지 않는다. 고치면 체크섬이 달라져 백엔드가 뜨지 않는다.
--    스키마를 바꿀 때는 V2 부터 새 파일을 만든다 — 규칙은 같은 폴더의 README.md.
--
-- 테이블이 이름순이라 FK 가 아직 없는 테이블을 가리킬 수 있어 FK 검사를 잠시 끈다.
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `attendance` (
  `attendance_id` bigint NOT NULL AUTO_INCREMENT,
  `attendance_date` date NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `daily_quiz_session_id` bigint DEFAULT NULL,
  `reward_claimed` bit(1) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`attendance_id`),
  UNIQUE KEY `uq_attendance_user_date` (`user_id`,`attendance_date`),
  KEY `idx_attendance_user_id` (`user_id`),
  KEY `idx_attendance_daily_quiz_session_id` (`daily_quiz_session_id`),
  CONSTRAINT `FKjcaqd29v2qy723owsdah2t8vx` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `avatar_items` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_default` bit(1) NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `image_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `metadata` json DEFAULT NULL,
  `rarity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `slot` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uq_avatar_items_name` (`item_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `bookmarks` (
  `bookmark_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `memo` text COLLATE utf8mb4_unicode_ci,
  `source_solve_session_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `question_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`bookmark_id`),
  UNIQUE KEY `uq_bookmarks_user_question` (`user_id`,`question_id`),
  KEY `FKmge2txjhd0hq1ji859u0ylwyw` (`question_id`),
  CONSTRAINT `FKdbsho2e05w5r13fkjqfjmge5f` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKmge2txjhd0hq1ji859u0ylwyw` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `category_lv1` (
  `category_lv1_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `category_lv1_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_lv1_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_lv1_name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `description` text COLLATE utf8mb4_unicode_ci,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`category_lv1_id`),
  UNIQUE KEY `uq_category_lv1_code` (`subject_id`,`category_lv1_code`),
  UNIQUE KEY `uq_category_lv1_name` (`subject_id`,`category_lv1_name`),
  CONSTRAINT `FKcud3ldvvu9gg81760i7iokuba` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `category_lv2` (
  `category_lv2_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `category_lv2_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_lv2_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category_lv2_name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `description` text COLLATE utf8mb4_unicode_ci,
  `sort_order` int NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `category_lv1_id` bigint NOT NULL,
  PRIMARY KEY (`category_lv2_id`),
  UNIQUE KEY `uq_category_lv2_code` (`category_lv1_id`,`category_lv2_code`),
  UNIQUE KEY `uq_category_lv2_name` (`category_lv1_id`,`category_lv2_name`),
  CONSTRAINT `FK582ab6usf1mbuw02hfa9luuwr` FOREIGN KEY (`category_lv1_id`) REFERENCES `category_lv1` (`category_lv1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `common_mistake` (
  `common_mistake_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `correction_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `mistake_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mistake_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `mistake_title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_note` text COLLATE utf8mb4_unicode_ci,
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `concept_id` bigint NOT NULL,
  PRIMARY KEY (`common_mistake_id`),
  UNIQUE KEY `uq_common_mistake_code` (`mistake_code`),
  KEY `idx_common_mistake_concept_title` (`concept_id`,`mistake_title`),
  CONSTRAINT `FKm1n5rl04oskcgf5cf6rh4asj3` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `concept` (
  `concept_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `aliases_json` json DEFAULT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `concept_code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `concept_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `concept_name_en` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `core_keywords_json` json NOT NULL,
  `core_summary` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `key_properties_json` json NOT NULL,
  `one_line_definition` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_note` text COLLATE utf8mb4_unicode_ci,
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `source_refs` json DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version_no` int NOT NULL,
  `category_lv2_id` bigint NOT NULL,
  PRIMARY KEY (`concept_id`),
  UNIQUE KEY `uq_concept_name_in_lv2` (`category_lv2_id`,`concept_name`),
  UNIQUE KEY `UKjl1c9qv2a8gkkj6w9mxgbgfyf` (`concept_code`),
  CONSTRAINT `FKasr5de11iq1fhuewsh4l0n74t` FOREIGN KEY (`category_lv2_id`) REFERENCES `category_lv2` (`category_lv2_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `concept_diagnostic_profile` (
  `concept_diagnostic_profile_id` bigint NOT NULL AUTO_INCREMENT,
  `basic_review_points` text COLLATE utf8mb4_unicode_ci,
  `concept_core` text COLLATE utf8mb4_unicode_ci,
  `concept_explanation` text COLLATE utf8mb4_unicode_ci,
  `concept_id` bigint NOT NULL,
  `confusion_comparisons` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) NOT NULL,
  `diagnostic_profile_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `key_points` text COLLATE utf8mb4_unicode_ci,
  `learning_example` text COLLATE utf8mb4_unicode_ci,
  `misconception_mapping_coverage` decimal(4,3) DEFAULT NULL,
  `misconception_notes` text COLLATE utf8mb4_unicode_ci,
  `prerequisite_concepts` text COLLATE utf8mb4_unicode_ci,
  `review_status` enum('APPROVED','ARCHIVED','DRAFT','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`concept_diagnostic_profile_id`),
  UNIQUE KEY `uq_concept_diagnostic_profile_concept_id` (`concept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `concept_relation` (
  `relation_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `relation_note` text COLLATE utf8mb4_unicode_ci,
  `relation_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `source_concept_id` bigint NOT NULL,
  `target_concept_id` bigint NOT NULL,
  PRIMARY KEY (`relation_id`),
  UNIQUE KEY `uq_concept_relation` (`source_concept_id`,`target_concept_id`,`relation_type`),
  KEY `FKjr3eqifcs61phq44tmdth99uu` (`target_concept_id`),
  CONSTRAINT `FKi3nf46pxm9611y2q33jxso05m` FOREIGN KEY (`source_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `FKjr3eqifcs61phq44tmdth99uu` FOREIGN KEY (`target_concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `ck_concept_relation_not_self` CHECK ((`source_concept_id` <> `target_concept_id`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `currency_logs` (
  `currency_log_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` int NOT NULL,
  `balance_after` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` enum('ADMIN_GRANT','AD_REWARD','ATTENDANCE_REWARD','CATEGORY_MOCK_FIRST_PASS_REWARD','CATEGORY_MOCK_RETRY_REWARD','COIN_PACKAGE_PURCHASE','DAILY_QUIZ_REWARD','DUPLICATE_ITEM_REFUND','GENDER_CHANGE','LEVEL_UP_REWARD','NICKNAME_CHANGE','PREMIUM_DAILY_COIN','PREMIUM_SIGNUP_BONUS','RANDOM_BOX_PURCHASE','RANDOM_BOX_REWARD','REFUND','ROOM_THEME_PURCHASE','SHOP_ITEM_PURCHASE','STARTER_PACK','SUBJECT_MOCK_FIRST_PASS_REWARD','SUBJECT_MOCK_RETRY_REWARD','WEAKNESS_ANALYSIS_CREATE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_type` enum('ADMIN_ADJUST','EARN','REFUND','SPEND') COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`currency_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_quiz_answer_logs` (
  `daily_quiz_answer_log_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `quiz_date` date NOT NULL,
  `selected_answer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  PRIMARY KEY (`daily_quiz_answer_log_id`),
  UNIQUE KEY `uq_daily_quiz_answer_logs_user_date_question` (`user_id`,`quiz_date`,`question_id`),
  KEY `idx_daily_quiz_answer_logs_user_date` (`user_id`,`quiz_date`),
  KEY `FK45xgfqfcg38nf1l5g235no95s` (`question_id`),
  CONSTRAINT `FK45xgfqfcg38nf1l5g235no95s` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_quiz_sets` (
  `daily_quiz_set_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `quiz_date` date NOT NULL,
  `sort_order` int NOT NULL,
  `question_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`daily_quiz_set_id`),
  UNIQUE KEY `uq_daily_quiz_sets_date_question` (`quiz_date`,`question_id`),
  UNIQUE KEY `uq_daily_quiz_sets_date_order` (`quiz_date`,`sort_order`),
  KEY `idx_daily_quiz_sets_question_id` (`question_id`),
  KEY `idx_daily_quiz_sets_subject_id` (`subject_id`),
  CONSTRAINT `FK2f35gcdjkyiegn3usrla174n1` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`),
  CONSTRAINT `FKgj7f3qmqri9x920ref08a7vvn` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `daily_reward_limits` (
  `daily_reward_limit_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `daily_limit` int DEFAULT NULL,
  `reward_count` int NOT NULL,
  `reward_date` date NOT NULL,
  `reward_type` enum('AD_REWARD','ATTENDANCE_REWARD','CATEGORY_MOCK_FIRST_PASS_COIN','CATEGORY_MOCK_RETRY_COIN','CATEGORY_MOCK_XP','CONCEPT_PRACTICE_XP','DAILY_QUIZ_ATTENDANCE','DAILY_QUIZ_COMPLETE','DAILY_QUIZ_REWARD','FEATURE_ACCESS_GRANT','LEVEL_UP_10','LEVEL_UP_20','LEVEL_UP_30','LEVEL_UP_40','LEVEL_UP_5','LEVEL_UP_50','PAYMENT_COIN_PACKAGE','PREMIUM_DAILY_COIN','PREMIUM_SIGNUP_BONUS','RANDOM_BOX_REWARD','STARTER_PACK','SUBJECT_MOCK_FIRST_PASS_COIN','SUBJECT_MOCK_RETRY_COIN','SUBJECT_MOCK_XP','WEAKNESS_ANALYSIS_CREATE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`daily_reward_limit_id`),
  UNIQUE KEY `uq_daily_reward_limits` (`user_id`,`reward_date`,`reward_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `device_tokens` (
  `device_token_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `device_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `platform` enum('ANDROID','IOS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `token` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`device_token_id`),
  UNIQUE KEY `uk_device_tokens_token` (`token`),
  KEY `idx_device_tokens_user_active` (`user_id`,`is_active`),
  CONSTRAINT `FKhc7d11bnr8x9gs5biohdhnx1c` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `feature_access_products` (
  `feature_product_id` bigint NOT NULL AUTO_INCREMENT,
  `access_policy` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `feature_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `premium_price_coin` int DEFAULT NULL,
  `price_coin` int NOT NULL,
  `product_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`feature_product_id`),
  UNIQUE KEY `UKirm0ycqaw6eepld8h80hcq5i` (`feature_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friend_pokes` (
  `poke_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `message_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  PRIMARY KEY (`poke_id`),
  KEY `idx_friend_pokes_from_user_id` (`from_user_id`),
  KEY `idx_friend_pokes_to_user_id` (`to_user_id`),
  CONSTRAINT `FKae5o85sivgb8egqh2m8dbsr1g` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKpigp25lvtaceeuyvbylqq9lws` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `friends` (
  `friend_id` bigint NOT NULL AUTO_INCREMENT,
  `accepted_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `status` enum('ACCEPTED','BLOCKED','REJECTED','REQUESTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_high_id` bigint NOT NULL,
  `user_low_id` bigint NOT NULL,
  `receiver_id` bigint NOT NULL,
  `requester_id` bigint NOT NULL,
  PRIMARY KEY (`friend_id`),
  UNIQUE KEY `uq_friends_user_pair` (`user_low_id`,`user_high_id`),
  KEY `idx_friends_requester_id` (`requester_id`),
  KEY `idx_friends_receiver_id` (`receiver_id`),
  KEY `idx_friends_user_pair_status` (`user_low_id`,`user_high_id`,`status`),
  CONSTRAINT `FK5c5mwkpdeyhhbhulwujqhv4al` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FK5mwylmmw7ot92jog0004x3gn1` FOREIGN KEY (`requester_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `idempotency_keys` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `idempotency_key` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `operation` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_idempotency_user_key` (`user_id`,`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notification_send_logs` (
  `notification_send_log_id` bigint NOT NULL AUTO_INCREMENT,
  `device_token_id` bigint DEFAULT NULL,
  `fail_reason` enum('FCM_ERROR','NO_TOKEN','OPTED_OUT','PERMISSION_DENIED','TOKEN_EXPIRED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `notification_id` bigint DEFAULT NULL,
  `notification_type` enum('ATTENDANCE_REMINDER','DAILY_QUIZ_OPEN','FRIEND_POKE','FRIEND_REQUEST_ACCEPTED','FRIEND_REQUEST_RECEIVED','LEARNING_REMINDER','LEVEL_UP','NEW_DEVICE_LOGIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `send_status` enum('FAILED','SKIPPED','SUCCESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `sent_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`notification_send_log_id`),
  KEY `idx_send_logs_user_type_sent` (`user_id`,`notification_type`,`sent_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `notifications` (
  `notification_id` bigint NOT NULL AUTO_INCREMENT,
  `body` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `category` enum('DAILY','FRIEND','LEARNING','REWARD','SYSTEM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `notification_type` enum('ATTENDANCE_REMINDER','DAILY_QUIZ_OPEN','FRIEND_POKE','FRIEND_REQUEST_ACCEPTED','FRIEND_REQUEST_RECEIVED','LEARNING_REMINDER','LEVEL_UP','NEW_DEVICE_LOGIN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_read` bit(1) NOT NULL,
  `read_at` datetime(6) DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` enum('DAILY_QUIZ','FRIEND','MYPAGE','NONE','SETTING') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `title` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`notification_id`),
  KEY `idx_notifications_user_read` (`user_id`,`is_read`),
  KEY `idx_notifications_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FK9y21adhxn0ayjhfocscqox7bh` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `question` (
  `question_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `approved_at` datetime(6) DEFAULT NULL,
  `choices_json` json DEFAULT NULL,
  `is_concept_practice_eligible` bit(1) NOT NULL,
  `content_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `correct_answer` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `is_daily_quiz_eligible` bit(1) NOT NULL,
  `difficulty_level` smallint NOT NULL,
  `difficulty_source` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `expected_solve_sec` int DEFAULT NULL,
  `explanation_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `is_mock_eligible` bit(1) NOT NULL,
  `question_format` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_text` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `question_type` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `review_note` text COLLATE utf8mb4_unicode_ci,
  `review_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `source_refs` json DEFAULT NULL,
  `source_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  `version_no` int NOT NULL,
  `concept_id` bigint NOT NULL,
  `hint_common_mistake_id` bigint DEFAULT NULL,
  PRIMARY KEY (`question_id`),
  UNIQUE KEY `UKsfybl80d79seaobeixnwjytyt` (`content_hash`),
  KEY `FKi7935mdhma0m6wx0o2s2rr2yg` (`concept_id`),
  KEY `FKh30uleydg95tfwevrjd5buisq` (`hint_common_mistake_id`),
  CONSTRAINT `FKh30uleydg95tfwevrjd5buisq` FOREIGN KEY (`hint_common_mistake_id`) REFERENCES `common_mistake` (`common_mistake_id`),
  CONSTRAINT `FKi7935mdhma0m6wx0o2s2rr2yg` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `ck_question_difficulty` CHECK ((`difficulty_level` between 1 and 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `question_diagnostic_profile` (
  `question_diagnostic_profile_id` bigint NOT NULL AUTO_INCREMENT,
  `answer_misconception_map` text COLLATE utf8mb4_unicode_ci,
  `concept_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `diagnostic_profile_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `diagnostic_summary` text COLLATE utf8mb4_unicode_ci,
  `difficulty` int DEFAULT NULL,
  `expected_solve_sec` int DEFAULT NULL,
  `question_id` bigint NOT NULL,
  `question_skill_type` varchar(40) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `review_status` enum('APPROVED','ARCHIVED','DRAFT','REJECTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  `reviewed_by` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`question_diagnostic_profile_id`),
  UNIQUE KEY `uq_question_diagnostic_profile_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `random_box_items` (
  `random_box_item_id` bigint NOT NULL AUTO_INCREMENT,
  `coin_amount` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `probability` decimal(8,6) NOT NULL,
  `reward_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `item_id` bigint DEFAULT NULL,
  `random_box_id` bigint NOT NULL,
  PRIMARY KEY (`random_box_item_id`),
  KEY `FKsrnlpfw7ihf2fv6mhu7u9fbyg` (`item_id`),
  KEY `FK9x4egcs9cw44n6qncvm7l7ac6` (`random_box_id`),
  CONSTRAINT `FK9x4egcs9cw44n6qncvm7l7ac6` FOREIGN KEY (`random_box_id`) REFERENCES `random_boxes` (`random_box_id`),
  CONSTRAINT `FKsrnlpfw7ihf2fv6mhu7u9fbyg` FOREIGN KEY (`item_id`) REFERENCES `avatar_items` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `random_box_open_logs` (
  `random_box_open_log_id` bigint NOT NULL AUTO_INCREMENT,
  `currency_log_id` bigint DEFAULT NULL,
  `opened_at` datetime(6) NOT NULL,
  `reward_coin_amount` int DEFAULT NULL,
  `reward_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `random_box_id` bigint NOT NULL,
  `reward_item_id` bigint DEFAULT NULL,
  `reward_user_item_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`random_box_open_log_id`),
  KEY `FKmsgr4qtlt68aad4vfhmppl20j` (`random_box_id`),
  KEY `FKqfawu37owkj6rm7wli6uamom6` (`reward_item_id`),
  KEY `FKmfmxxoxd5uassn4oqqt8rcam9` (`reward_user_item_id`),
  KEY `FKlc2ipkup1rfw5qk6y8iw04jke` (`user_id`),
  CONSTRAINT `FKlc2ipkup1rfw5qk6y8iw04jke` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `FKmfmxxoxd5uassn4oqqt8rcam9` FOREIGN KEY (`reward_user_item_id`) REFERENCES `user_items` (`user_item_id`),
  CONSTRAINT `FKmsgr4qtlt68aad4vfhmppl20j` FOREIGN KEY (`random_box_id`) REFERENCES `random_boxes` (`random_box_id`),
  CONSTRAINT `FKqfawu37owkj6rm7wli6uamom6` FOREIGN KEY (`reward_item_id`) REFERENCES `avatar_items` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `random_boxes` (
  `random_box_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `box_grade` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `box_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `required_level` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`random_box_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reward_logs` (
  `reward_log_id` bigint NOT NULL AUTO_INCREMENT,
  `coin_amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `item_id` bigint DEFAULT NULL,
  `random_box_id` bigint DEFAULT NULL,
  `reward_policy_id` bigint DEFAULT NULL,
  `reward_source` enum('ADMIN','AD_REWARD','ATTENDANCE','DAILY_QUIZ','LEVEL_UP','PAYMENT','PREMIUM','RANDOM_BOX','SOLVE_SESSION','STARTER_PACK') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_status` enum('FAILED','GRANTED','SKIPPED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_type` enum('AD_REWARD','ATTENDANCE_REWARD','CATEGORY_MOCK_FIRST_PASS_COIN','CATEGORY_MOCK_RETRY_COIN','CATEGORY_MOCK_XP','CONCEPT_PRACTICE_XP','DAILY_QUIZ_ATTENDANCE','DAILY_QUIZ_COMPLETE','DAILY_QUIZ_REWARD','FEATURE_ACCESS_GRANT','LEVEL_UP_10','LEVEL_UP_20','LEVEL_UP_30','LEVEL_UP_40','LEVEL_UP_5','LEVEL_UP_50','PAYMENT_COIN_PACKAGE','PREMIUM_DAILY_COIN','PREMIUM_SIGNUP_BONUS','RANDOM_BOX_REWARD','STARTER_PACK','SUBJECT_MOCK_FIRST_PASS_COIN','SUBJECT_MOCK_RETRY_COIN','SUBJECT_MOCK_XP','WEAKNESS_ANALYSIS_CREATE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `skip_reason` enum('ALREADY_REWARDED','ALREADY_REWARDED_TODAY','DAILY_LIMIT_EXCEEDED','INSUFFICIENT_COIN','INVALID_REWARD_AMOUNT','LOW_CORRECT_RATE','POLICY_NOT_FOUND') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `solve_session_id` bigint DEFAULT NULL,
  `source_id` bigint DEFAULT NULL,
  `target_id` bigint DEFAULT NULL,
  `target_type` enum('CATEGORY_LV1','CATEGORY_LV2','CONCEPT','DAILY','LEVEL','SUBJECT','USER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `xp_amount` int NOT NULL,
  PRIMARY KEY (`reward_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `reward_policies` (
  `reward_policy_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `coin_amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `daily_limit` int DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `first_reward_only` bit(1) NOT NULL,
  `min_correct_rate` decimal(5,2) DEFAULT NULL,
  `reward_kind` enum('COIN','ITEM','MIXED','RANDOM_BOX','XP') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reward_scope` enum('PER_CATEGORY_LV1','PER_CATEGORY_LV2','PER_CONCEPT','PER_DAY','PER_LEVEL','PER_SESSION','PER_SUBJECT','PER_USER') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reward_type` enum('AD_REWARD','ATTENDANCE_REWARD','CATEGORY_MOCK_FIRST_PASS_COIN','CATEGORY_MOCK_RETRY_COIN','CATEGORY_MOCK_XP','CONCEPT_PRACTICE_XP','DAILY_QUIZ_ATTENDANCE','DAILY_QUIZ_COMPLETE','DAILY_QUIZ_REWARD','FEATURE_ACCESS_GRANT','LEVEL_UP_10','LEVEL_UP_20','LEVEL_UP_30','LEVEL_UP_40','LEVEL_UP_5','LEVEL_UP_50','PAYMENT_COIN_PACKAGE','PREMIUM_DAILY_COIN','PREMIUM_SIGNUP_BONUS','RANDOM_BOX_REWARD','STARTER_PACK','SUBJECT_MOCK_FIRST_PASS_COIN','SUBJECT_MOCK_RETRY_COIN','SUBJECT_MOCK_XP','WEAKNESS_ANALYSIS_CREATE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `scope_daily_once` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `xp_amount` int NOT NULL,
  PRIMARY KEY (`reward_policy_id`),
  UNIQUE KEY `UKg3m8035mq1r2xh7dhqk4cnx2g` (`reward_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shop_products` (
  `shop_product_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `price_coin` int NOT NULL,
  `product_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `product_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `required_level` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `item_id` bigint DEFAULT NULL,
  `random_box_id` bigint DEFAULT NULL,
  PRIMARY KEY (`shop_product_id`),
  KEY `FK79nncq6cq0d9g92cv0n8cuw36` (`item_id`),
  KEY `FKbe1uax3e9q5drfh8vm0pkji3v` (`random_box_id`),
  CONSTRAINT `FK79nncq6cq0d9g92cv0n8cuw36` FOREIGN KEY (`item_id`) REFERENCES `avatar_items` (`item_id`),
  CONSTRAINT `FKbe1uax3e9q5drfh8vm0pkji3v` FOREIGN KEY (`random_box_id`) REFERENCES `random_boxes` (`random_box_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `shop_purchase_logs` (
  `shop_purchase_id` bigint NOT NULL AUTO_INCREMENT,
  `currency_log_id` bigint DEFAULT NULL,
  `purchased_at` datetime(6) NOT NULL,
  `shop_product_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`shop_purchase_id`),
  KEY `FKeddefw2kdgu1hc40uu1jbqv6e` (`shop_product_id`),
  KEY `FKsjc9egqcoh3ebqx6jw79ul8vd` (`user_id`),
  CONSTRAINT `FKeddefw2kdgu1hc40uu1jbqv6e` FOREIGN KEY (`shop_product_id`) REFERENCES `shop_products` (`shop_product_id`),
  CONSTRAINT `FKsjc9egqcoh3ebqx6jw79ul8vd` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `solve_logs` (
  `solve_log_id` bigint NOT NULL AUTO_INCREMENT,
  `is_answer_changed` bit(1) NOT NULL,
  `is_bookmarked` bit(1) NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `correct_answer_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expected_solve_sec_snapshot` int DEFAULT NULL,
  `hint_count` int NOT NULL,
  `hint_used` bit(1) NOT NULL,
  `option_change_count` int NOT NULL,
  `question_order` int NOT NULL,
  `selected_answer` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `solved_at` datetime(6) NOT NULL,
  `time_spent_sec` int NOT NULL,
  `user_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `solve_session_id` bigint NOT NULL,
  PRIMARY KEY (`solve_log_id`),
  UNIQUE KEY `UK7tbgqfeg8i1xe8tfx0dy4gn2` (`solve_session_id`,`question_id`),
  UNIQUE KEY `UKigwd36c9pkfhpq2vvlt8abik7` (`solve_session_id`,`question_order`),
  KEY `FKfoa3qikse3e1yt279ley1j7ga` (`question_id`),
  CONSTRAINT `FKfoa3qikse3e1yt279ley1j7ga` FOREIGN KEY (`question_id`) REFERENCES `question` (`question_id`),
  CONSTRAINT `FKib7i68nwlifbs5r1jke309s8p` FOREIGN KEY (`solve_session_id`) REFERENCES `solve_sessions` (`solve_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `solve_sessions` (
  `solve_session_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) DEFAULT NULL,
  `correct_count` int NOT NULL,
  `correct_rate` decimal(5,2) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `question_ids_json` json DEFAULT NULL,
  `session_type` enum('CATEGORY_LV1_MOCK','CATEGORY_LV2_MOCK','CONCEPT_PRACTICE','DAILY_QUIZ','SUBJECT_MOCK') COLLATE utf8mb4_unicode_ci NOT NULL,
  `started_at` datetime(6) NOT NULL,
  `status` enum('ABANDONED','COMPLETED','IN_PROGRESS') COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_question_count` int NOT NULL,
  `total_time_spent_sec` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `wrong_count` int NOT NULL,
  `category_lv1_id` bigint DEFAULT NULL,
  `category_lv2_id` bigint DEFAULT NULL,
  `concept_id` bigint DEFAULT NULL,
  `subject_id` bigint DEFAULT NULL,
  PRIMARY KEY (`solve_session_id`),
  KEY `FKlatl6gj741t7ft3psrqgjia1y` (`category_lv1_id`),
  KEY `FK460fhovodms8wu9p5smg5868u` (`category_lv2_id`),
  KEY `FK9lhiwku9scwobjg8erjix38g9` (`concept_id`),
  KEY `FKexkp2s1tcvexn3ths9wjou303` (`subject_id`),
  CONSTRAINT `FK460fhovodms8wu9p5smg5868u` FOREIGN KEY (`category_lv2_id`) REFERENCES `category_lv2` (`category_lv2_id`),
  CONSTRAINT `FK9lhiwku9scwobjg8erjix38g9` FOREIGN KEY (`concept_id`) REFERENCES `concept` (`concept_id`),
  CONSTRAINT `FKexkp2s1tcvexn3ths9wjou303` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`subject_id`),
  CONSTRAINT `FKlatl6gj741t7ft3psrqgjia1y` FOREIGN KEY (`category_lv1_id`) REFERENCES `category_lv1` (`category_lv1_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subject` (
  `subject_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `sort_order` int NOT NULL,
  `subject_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_name_en` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`subject_id`),
  UNIQUE KEY `UKbqn0dl9ld0wcq9na8amhhramm` (`subject_code`),
  UNIQUE KEY `UK9x44p8j4xvkb6vhaccexngaao` (`subject_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `subscription_plans` (
  `subscription_plan_id` bigint NOT NULL AUTO_INCREMENT,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `daily_coin_amount` int NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `monthly_bonus_coin` int NOT NULL,
  `plan_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `price_krw` int DEFAULT NULL,
  `random_box_discount_rate` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`subscription_plan_id`),
  UNIQUE KEY `UKrsiqsy651mlytqpby2qlh7orb` (`plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_avatar_equipped_items` (
  `equipped_item_id` bigint NOT NULL AUTO_INCREMENT,
  `slot` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_item_id` bigint NOT NULL,
  PRIMARY KEY (`equipped_item_id`),
  UNIQUE KEY `uq_user_avatar_equipped_slot` (`user_id`,`slot`),
  KEY `idx_user_avatar_equipped_user_id` (`user_id`),
  KEY `idx_user_avatar_equipped_user_item_id` (`user_item_id`),
  CONSTRAINT `FK1wubxf4sycaf4cjc2joyhbvqp` FOREIGN KEY (`user_item_id`) REFERENCES `user_items` (`user_item_id`),
  CONSTRAINT `FK6e2bv6rvjubng06agkocnqxrh` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_avatar_profiles` (
  `user_avatar_profile_id` bigint NOT NULL AUTO_INCREMENT,
  `base_avatar_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `room_pose` enum('SIT_DESK','SLEEP_BED','STAND') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `room_pose_updated_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `room_pose_placement_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_avatar_profile_id`),
  UNIQUE KEY `uq_user_avatar_profiles_user` (`user_id`),
  KEY `FKl8mulytuo8sf89jte04agkfvt` (`room_pose_placement_id`),
  CONSTRAINT `FKl8mulytuo8sf89jte04agkfvt` FOREIGN KEY (`room_pose_placement_id`) REFERENCES `user_room_placements` (`placement_id`),
  CONSTRAINT `FKr5w3pigv4ewn06iaiydigksi` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_concept_progress` (
  `user_concept_progress_id` bigint NOT NULL AUTO_INCREMENT,
  `is_completed` bit(1) NOT NULL,
  `completed_at` datetime(6) DEFAULT NULL,
  `completed_solve_session_id` bigint DEFAULT NULL,
  `concept_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_concept_progress_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_currency` (
  `user_id` bigint NOT NULL,
  `coin` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_items` (
  `user_item_id` bigint NOT NULL AUTO_INCREMENT,
  `acquired_at` datetime(6) NOT NULL,
  `acquired_source` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `quantity` int NOT NULL,
  `source_id` bigint DEFAULT NULL,
  `item_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_item_id`),
  UNIQUE KEY `uq_user_items_user_item` (`user_id`,`item_id`),
  KEY `idx_user_items_user_id` (`user_id`),
  KEY `idx_user_items_item_id` (`item_id`),
  CONSTRAINT `FK3diwnrm9e3pq8xq4tncj42rsg` FOREIGN KEY (`item_id`) REFERENCES `avatar_items` (`item_id`),
  CONSTRAINT `FK55mpmb46vtbmw6xljldr2wvvf` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_learning_history_stats` (
  `user_learning_history_stat_id` bigint NOT NULL AUTO_INCREMENT,
  `completed_concept_count` int NOT NULL,
  `concept_correct_question_count` int NOT NULL,
  `total_mock_solve_time_sec` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_learning_history_stat_id`),
  UNIQUE KEY `UKpg0f5s1byo4sp02ce7gal0hp5` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_notification_settings` (
  `notification_setting_id` bigint NOT NULL AUTO_INCREMENT,
  `category` enum('DAILY','FRIEND','LEARNING','REWARD','SYSTEM') COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `is_enabled` bit(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`notification_setting_id`),
  UNIQUE KEY `uk_user_notification_settings_user_category` (`user_id`,`category`),
  CONSTRAINT `FKs9tjvxu8ko31ivjlq9l9duh9y` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_room_placements` (
  `placement_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `grid_x` tinyint NOT NULL,
  `grid_y` tinyint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `user_item_id` bigint NOT NULL,
  PRIMARY KEY (`placement_id`),
  UNIQUE KEY `uq_user_room_placements_cell` (`user_id`,`grid_x`,`grid_y`),
  KEY `idx_user_room_placements_user_id` (`user_id`),
  KEY `FK6gk499cs78id86j1naunm0n03` (`user_item_id`),
  CONSTRAINT `FK6gk499cs78id86j1naunm0n03` FOREIGN KEY (`user_item_id`) REFERENCES `user_items` (`user_item_id`),
  CONSTRAINT `FKhy38gq137l6ohwe0kgmbfm250` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_sessions` (
  `user_session_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `device_id` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `last_seen_at` datetime(6) NOT NULL,
  `os_version` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `platform` enum('ANDROID','IOS','UNKNOWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `refresh_expires_at` datetime(6) DEFAULT NULL,
  `refresh_token_hash` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revoke_reason` enum('MAX_SESSION_EXCEEDED','PASSWORD_RESET','REMOTE_LOGOUT','USER_LOGOUT','WITHDRAWAL') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `session_id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`user_session_id`),
  UNIQUE KEY `uq_user_sessions_user_id_device_id` (`user_id`,`device_id`),
  UNIQUE KEY `uq_user_sessions_session_id` (`session_id`),
  KEY `idx_user_sessions_user_id_revoked_at` (`user_id`,`revoked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_social_accounts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `provider` enum('GOOGLE','KAKAO','NAVER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `provider_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_provider_provider_id` (`provider`,`provider_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `users` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `last_login_at` datetime(6) DEFAULT NULL,
  `level` int NOT NULL,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `nickname` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone_number` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `role` enum('ADMIN','USER') COLLATE utf8mb4_unicode_ci NOT NULL,
  `school_email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `school_verified` bit(1) NOT NULL,
  `status` enum('ACTIVE','BANNED','WITHDRAWN') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_xp` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `UK2ty1xmrrgtn89xt7kyxx6ta7h` (`nickname`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_ai_call_logs` (
  `ai_call_log_id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_report_id` bigint NOT NULL,
  `cache_write_tokens` int DEFAULT NULL,
  `cached_tokens` int DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `diagnostic_profile_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `escalation_count` int NOT NULL,
  `input_tokens` int DEFAULT NULL,
  `latency_ms` int DEFAULT NULL,
  `model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `output_tokens` int DEFAULT NULL,
  `prompt_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reasoning_tokens` int DEFAULT NULL,
  `request_fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `retry_count` int NOT NULL,
  `schema_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scoring_rule_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`ai_call_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_concepts` (
  `analysis_concept_id` bigint NOT NULL AUTO_INCREMENT,
  `ai_analysis_status` enum('AMBIGUOUS','INSUFFICIENT','SUPPORTED') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_primary_type` enum('APPLICATION_GAP','CONCEPT_CONFUSION','EXECUTION_ERROR','SPEED_FLUENCY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ai_secondary_signals` text COLLATE utf8mb4_unicode_ci,
  `backend_candidate_type` enum('APPLICATION_GAP','CONCEPT_CONFUSION','EXECUTION_ERROR','SPEED_FLUENCY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `cause_code` enum('APPLICATION_GAP','CARELESS_EXECUTION','PROCEDURE_FLUENCY_GAP','RELATIONSHIP_MAPPING_GAP','REPEATED_MISCONCEPTION','TERM_CONFUSION') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `concept_id` bigint NOT NULL,
  `concept_tip` text COLLATE utf8mb4_unicode_ci,
  `confused_relation` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `diagnosis` text COLLATE utf8mb4_unicode_ci,
  `evidence_strength` decimal(4,3) NOT NULL,
  `final_type` enum('APPLICATION_GAP','CONCEPT_CONFUSION','EXECUTION_ERROR','SPEED_FLUENCY') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `final_type_source` enum('AI','BOTH','NONE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hint_used_count` int NOT NULL,
  `likely_cause` text COLLATE utf8mb4_unicode_ci,
  `long_time_count` int NOT NULL,
  `question_count` int NOT NULL,
  `reconciliation_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sort_order` int NOT NULL,
  `type_agreement` bit(1) DEFAULT NULL,
  `type_separation` decimal(4,3) DEFAULT NULL,
  `weakness_score` int NOT NULL,
  `wrong_count` int NOT NULL,
  `analysis_report_id` bigint NOT NULL,
  PRIMARY KEY (`analysis_concept_id`),
  KEY `FKbvos09p0g1iurfmnca1ncp7f8` (`analysis_report_id`),
  CONSTRAINT `FKbvos09p0g1iurfmnca1ncp7f8` FOREIGN KEY (`analysis_report_id`) REFERENCES `weakness_analysis_reports` (`analysis_report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_evidence_logs` (
  `evidence_log_id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_concept_id` bigint DEFAULT NULL,
  `analysis_report_id` bigint NOT NULL,
  `cited_by_ai` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `evidence_type` enum('ANSWER_CHANGED','HINT_USED','LONG_TIME','WRONG') COLLATE utf8mb4_unicode_ci NOT NULL,
  `solve_log_id` bigint NOT NULL,
  PRIMARY KEY (`evidence_log_id`),
  UNIQUE KEY `uq_weakness_evidence_report_solve_type` (`analysis_report_id`,`solve_log_id`,`evidence_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_report_view_logs` (
  `report_view_log_id` bigint NOT NULL AUTO_INCREMENT,
  `analysis_report_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `view_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `viewed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`report_view_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_reports` (
  `analysis_report_id` bigint NOT NULL AUTO_INCREMENT,
  `ai_model` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `analysis_scope_name_snapshot` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `analysis_scope_type` enum('CATEGORY_LV1','CATEGORY_LV2','CONCEPT','SUBJECT') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `analysis_text` text COLLATE utf8mb4_unicode_ci,
  `category_lv1_id` bigint DEFAULT NULL,
  `category_lv2_id` bigint DEFAULT NULL,
  `coin_cost` int NOT NULL,
  `concept_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `currency_log_id` bigint DEFAULT NULL,
  `diagnostic_profile_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `failed_reason` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `feature_product_id` bigint DEFAULT NULL,
  `first_viewed_at` datetime(6) DEFAULT NULL,
  `generated_at` datetime(6) DEFAULT NULL,
  `last_viewed_at` datetime(6) DEFAULT NULL,
  `prompt_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `report_format` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `request_fingerprint` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `requested_at` datetime(6) DEFAULT NULL,
  `schema_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `scoring_rule_version` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `selected_session_count` int NOT NULL,
  `status` enum('COMPLETED','DELETED','FAILED','GENERATING','REQUESTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `subject_id` bigint NOT NULL,
  `summary_text` text COLLATE utf8mb4_unicode_ci,
  `title` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `total_hint_used_count` int NOT NULL,
  `total_long_time_count` int NOT NULL,
  `total_question_count` int NOT NULL,
  `total_wrong_count` int NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `user_id` bigint NOT NULL,
  `view_count` int NOT NULL,
  `viewer_content_type` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`analysis_report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `weakness_analysis_sessions` (
  `analysis_report_session_id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `selected_order` int NOT NULL,
  `solve_session_id` bigint NOT NULL,
  `solve_session_title_snapshot` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `wrong_count_snapshot` int NOT NULL,
  `analysis_report_id` bigint NOT NULL,
  PRIMARY KEY (`analysis_report_session_id`),
  UNIQUE KEY `uq_weakness_analysis_sessions_report_session` (`analysis_report_id`,`solve_session_id`),
  CONSTRAINT `FKrgapy6131tp1slron5o4lfs1b` FOREIGN KEY (`analysis_report_id`) REFERENCES `weakness_analysis_reports` (`analysis_report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `xp_logs` (
  `xp_log_id` bigint NOT NULL AUTO_INCREMENT,
  `amount` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` enum('ATTENDANCE_XP','CATEGORY_MOCK_XP','CONCEPT_PRACTICE_XP','DAILY_QUIZ_XP','LEVEL_UP_BONUS_XP','SUBJECT_MOCK_XP') COLLATE utf8mb4_unicode_ci NOT NULL,
  `reference_id` bigint DEFAULT NULL,
  `reference_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reward_policy_id` bigint DEFAULT NULL,
  `total_xp_after` int NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`xp_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
