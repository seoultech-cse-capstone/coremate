package com.coremate.backend.avatar.catalog.type;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

// 아바타 장착 슬롯 12종. 이름은 AvatarItemType 과 1:1로 같고, FURNITURE 만 슬롯이 없다.
public enum AvatarSlot {

    // 착용 9부위. renderOrder 는 정면 기준으로 뒤에서 앞으로 겹쳐 그리는 순서다.
    // 마네킹 → 신발 → 하의 → 상의 → 가방 → 머리 → 눈 → 입 → 안경 → 모자.
    SHOES(false, 1),
    BOTTOM(true, 2),
    TOP(true, 3),
    BAG(false, 4),
    HAIR(true, 5),
    EYES(true, 6),
    MOUTH(true, 7),
    EYEWEAR(false, 8),
    HAT(false, 9),

    // 아바타 스프라이트 스택에 속하지 않는 3종. 펫은 방에서 아바타를 따라다니고,
    // 벽지와 바닥은 방 배경이라 겹침 순서가 아바타와 무관하다.
    PET(false, null),
    WALLPAPER(true, null),

    // 벽지·바닥을 따로 조합하지 않기로 하면서 WALLPAPER 테마 하나로 합쳤다(2026-08-30).
    // 값은 남기되 이 슬롯의 아이템은 만들지 않는다.
    FLOOR(false, null);

    // 비우면 렌더가 깨지는 필수 슬롯 6종(HAIR·EYES·MOUTH·TOP·BOTTOM·WALLPAPER).
    // 교체만 되고 해제는 안 된다. 상의·하의가 빠지면 옷을 안 입은 아바타가 되고,
    // 방 테마가 빠지면 방이 그려지지 않는다.
    private final boolean required;

    // 아바타 스프라이트를 겹쳐 그리는 순서. 스택에 속하지 않으면 null.
    private final Integer renderOrder;

    AvatarSlot(boolean required, Integer renderOrder) {
        this.required = required;
        this.renderOrder = renderOrder;
    }

    public boolean isRequired() {
        return required;
    }

    public Integer getRenderOrder() {
        return renderOrder;
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
