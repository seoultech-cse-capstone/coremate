package com.coremate.backend.reward.entity.type;

// CoreMate에서 발생 가능한 보상/제한 정책 유형.
public enum RewardType {
    CONCEPT_PRACTICE_XP,
    CATEGORY_MOCK_XP,
    SUBJECT_MOCK_XP,
    DAILY_QUIZ_COMPLETE,
    DAILY_QUIZ_REWARD,
    DAILY_QUIZ_ATTENDANCE,
    ATTENDANCE_REWARD,
    CATEGORY_MOCK_FIRST_PASS_COIN,
    SUBJECT_MOCK_FIRST_PASS_COIN,
    // 최초 통과 보상을 이미 받은 범위를 다시 풀었을 때의 소액 보상.
    // 최초 통과와 별도 타입이라 일일 한도도 별도로 쌓인다.
    CATEGORY_MOCK_RETRY_COIN,
    SUBJECT_MOCK_RETRY_COIN,
    // 레벨업 보상은 구간마다 지급량이 달라 정책 행을 나눠 관리한다.
    // reward_policies.reward_type이 UNIQUE라 단일 값으로는 구간별 값을 담을 수 없다.
    LEVEL_UP_5,
    LEVEL_UP_10,
    LEVEL_UP_20,
    LEVEL_UP_30,
    LEVEL_UP_40,
    LEVEL_UP_50,
    STARTER_PACK,
    RANDOM_BOX_REWARD,
    AD_REWARD,
    PREMIUM_DAILY_COIN,
    PREMIUM_SIGNUP_BONUS,
    FEATURE_ACCESS_GRANT,
    PAYMENT_COIN_PACKAGE,
    WEAKNESS_ANALYSIS_CREATE
}
