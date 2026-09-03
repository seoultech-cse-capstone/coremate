package com.coremate.backend.avatar.catalog.type;

// 보유 아이템을 어디서 얻었는지. user_items.acquired_source 에 이름 그대로 저장한다.
public enum AcquiredSource {

    // 랜덤박스 오픈 보상.
    RANDOM_BOX,

    // 상점에서 직접 구매. 꾸미기 아이템 직판이 폐기돼 지금은 변경권 2종만 이 경로로 들어온다.
    SHOP_PURCHASE,

    // 가입 시 자동 지급하는 기본 6종. 비매품이라 랜덤박스 후보에서도 빠진다.
    DEFAULT_GRANT
}
