package com.coremate.backend.reward.entity.type;

// currency_logs.reason에 저장되는 코인 증감 사유.
public enum CurrencyReason {
    DAILY_QUIZ_REWARD,
    ATTENDANCE_REWARD,
    CATEGORY_MOCK_FIRST_PASS_REWARD,
    SUBJECT_MOCK_FIRST_PASS_REWARD,
    CATEGORY_MOCK_RETRY_REWARD,
    SUBJECT_MOCK_RETRY_REWARD,
    LEVEL_UP_REWARD,
    STARTER_PACK,
    RANDOM_BOX_PURCHASE,
    // 폐기됨 - 방 테마 상품이 벽지/바닥으로 분리되고 직판도 없어졌다.
    // 값을 지우면 이미 쌓인 currency_logs 행을 못 읽으므로 남겨둔다.
    ROOM_THEME_PURCHASE,
    NICKNAME_CHANGE,
    GENDER_CHANGE,
    WEAKNESS_ANALYSIS_CREATE,
    AD_REWARD,
    PREMIUM_DAILY_COIN,
    PREMIUM_SIGNUP_BONUS,
    COIN_PACKAGE_PURCHASE,
    SHOP_ITEM_PURCHASE,
    RANDOM_BOX_REWARD,
    ADMIN_GRANT,
    REFUND,
    // 이미 가진 아이템이 랜덤박스에서 또 나왔을 때의 등급별 코인 환급.
    // 금액은 ItemRarity 가 갖고 있고 ItemGrantService 가 계산해 내려준다.
    DUPLICATE_ITEM_REFUND
}
