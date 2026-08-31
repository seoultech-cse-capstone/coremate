package com.coremate.backend.shop.dto.type;

public enum ShopProductType {
    // 꾸미기 아이템 직판 폐기(ITEM·ROOM_THEME·ACTION_CARD 제거) — 랜덤박스가 유일한 획득 경로다.
    // 성별 구분 폐기(08-28)로 GENDER_CHANGE 도 판매 목록에서 제외한다.
    // (ShopProduct.PRODUCT_TYPE_GENDER_CHANGE 상수와 CurrencyReason.GENDER_CHANGE 는 과거 로그를 읽어야 하므로 남겨 둔다.)
    RANDOM_BOX,
    NICKNAME_CHANGE,
    // 코인 패키지는 아직 보류 상태로 남겨 둔다.
    COIN_PACKAGE,
    UNKNOWN;

    public static ShopProductType from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        try {
            return ShopProductType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
