package com.coremate.backend.shop.dto.type;

public enum ShopProductType {
    ITEM,
    RANDOM_BOX,
    ROOM_THEME,
    GENDER_CHANGE,
    NICKNAME_CHANGE,
    ACTION_CARD,
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
