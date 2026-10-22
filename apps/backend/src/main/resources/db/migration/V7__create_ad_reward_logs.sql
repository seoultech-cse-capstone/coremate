-- =============================================================================
-- V7__create_ad_reward_logs.sql
--
-- 광고 시청 보상 기록(ad_reward_logs)을 만든다. DB 명세서에는 있었지만 광고 보상(FR-PAY-02)이
-- "추후" 우선순위라 MVP 에서 만들지 않았던 테이블이다(#363).
--
-- AdMob 보상형 광고는 시청이 끝나면 구글이 우리 서버로 서명된 콜백(서버 측 확인, SSV)을 보내고,
-- 콜백 한 건이 이 테이블 한 행이다. 구글은 200 을 못 받으면 같은 콜백을 다시 보내므로
-- provider_transaction_id 의 UNIQUE 로 중복 지급을 막는다.
--
-- 명세와 다른 점 두 가지.
-- - reward_status 는 다른 상태 컬럼처럼 native enum 이고, 실제로 쓰는 REWARDED·FAILED 만 둔다.
--   명세 예시의 CANCELED(시청 중단)는 구글이 콜백을 보내지 않아 알 수 없고, DUPLICATED 는
--   UNIQUE 때문에 행을 남길 수 없다.
-- - user_id·currency_log_id 는 다른 로그 테이블(reward_logs·currency_logs)처럼 물리 FK 없이 논리 참조다.
-- =============================================================================

CREATE TABLE `ad_reward_logs` (
  `ad_reward_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `ad_provider` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ad_unit_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider_transaction_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coin_amount` int NOT NULL,
  `currency_log_id` bigint DEFAULT NULL,
  `reward_status` enum('REWARDED','FAILED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REWARDED',
  `rewarded_at` datetime(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`ad_reward_id`),
  UNIQUE KEY `uk_ad_reward_logs_provider_transaction_id` (`provider_transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
