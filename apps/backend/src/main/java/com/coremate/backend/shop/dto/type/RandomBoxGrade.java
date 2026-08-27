package com.coremate.backend.shop.dto.type;

public enum RandomBoxGrade {
    // MVP가 쓰는 유일한 등급. 박스는 1종이고 등급을 나누지 않는다.
    STANDARD,
    // 아래는 확장용으로 남겨 둔다.
    LOW,
    MID,
    HIGH,
    LEGEND,
    COMMON,
    RARE,
    EPIC,
    LEGENDARY;

    public static RandomBoxGrade from(String value) {
        if (value == null || value.isBlank()) {
            return STANDARD;
        }
        try {
            return RandomBoxGrade.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STANDARD;
        }
    }
}
