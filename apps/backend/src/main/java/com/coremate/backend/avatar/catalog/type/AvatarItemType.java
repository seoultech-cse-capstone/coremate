package com.coremate.backend.avatar.catalog.type;

public enum AvatarItemType {
    CLOTHES,
    HAT,
    SHOES,
    FURNITURE,
    ROOM_THEME,
    PET,
    ACTION_CARD,
    HEAD,
    FACE,
    BODY,
    ACCESSORY,
    BACKGROUND,
    OTHER;

    public static AvatarItemType from(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        try {
            return AvatarItemType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
