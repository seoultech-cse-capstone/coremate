package com.coremate.backend.avatar.catalog.type;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

// 아이템 마스터의 종류. 장착형 12종은 같은 이름의 AvatarSlot 을 갖고, FURNITURE 만 슬롯이 없다.
public enum AvatarItemType {

    // 착용 9부위. 렌더 순서는 마네킹 → 신발 → 하의 → 상의 → 가방 → 머리 → 눈 → 입 → 안경 → 모자.
    HAT,
    HAIR,
    EYES,
    EYEWEAR,
    MOUTH,
    TOP,
    BOTTOM,
    SHOES,
    BAG,

    // 방에서 아바타를 따라다니는 활성 아이템.
    PET,

    // 방 배경. 벽지와 바닥은 분리해서 장착한다.
    WALLPAPER,
    FLOOR,

    // 슬롯이 아니라 좌표로 배치한다. 같은 가구를 여러 개 놓을 수 있다.
    FURNITURE;

    public static AvatarItemType from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        try {
            return AvatarItemType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
    }

    public boolean isFurniture() {
        return this == FURNITURE;
    }
}
