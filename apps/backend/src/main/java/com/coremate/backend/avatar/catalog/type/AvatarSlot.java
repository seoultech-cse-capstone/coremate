package com.coremate.backend.avatar.catalog.type;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

// 아바타 장착 슬롯 12종. 이름은 AvatarItemType 과 1:1로 같고, FURNITURE 만 슬롯이 없다.
public enum AvatarSlot {

    HAT(false),
    HAIR(true),
    EYES(true),
    EYEWEAR(false),
    MOUTH(true),
    TOP(false),
    BOTTOM(false),
    SHOES(false),
    BAG(false),
    PET(false),
    WALLPAPER(true),
    FLOOR(true);

    // 비우면 렌더가 깨지는 필수 슬롯 5종. 교체만 되고 해제는 안 된다.
    private final boolean required;

    AvatarSlot(boolean required) {
        this.required = required;
    }

    public boolean isRequired() {
        return required;
    }

    // 장착형 12종은 슬롯 이름이 곧 아이템 종류다. FURNITURE 는 슬롯이 없어 null 을 준다.
    public static AvatarSlot of(AvatarItemType itemType) {
        if (itemType == null || itemType.isFurniture()) {
            return null;
        }
        return AvatarSlot.valueOf(itemType.name());
    }

    public static AvatarSlot from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
        }
        try {
            return AvatarSlot.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreMateException(ErrorCode.INVALID_AVATAR_SLOT);
        }
    }
}
