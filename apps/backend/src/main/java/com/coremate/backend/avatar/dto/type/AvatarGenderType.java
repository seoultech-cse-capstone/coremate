package com.coremate.backend.avatar.dto.type;

public enum AvatarGenderType {
    MALE,
    FEMALE,
    OTHER;

    public static AvatarGenderType from(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        try {
            return AvatarGenderType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
