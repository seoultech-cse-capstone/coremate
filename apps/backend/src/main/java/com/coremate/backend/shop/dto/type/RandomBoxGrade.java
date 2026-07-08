package com.coremate.backend.shop.dto.type;

public enum RandomBoxGrade {
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
            return COMMON;
        }
        try {
            return RandomBoxGrade.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
