package com.coremate.backend.avatar.profile;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

// 방 안에서 아바타가 마지막에 취한 자세.
// ⚠️ 앱이 그리는 포즈는 4종(서있기 앞·뒤, 책상 타이핑, 침대 수면)이지만 저장하는 값은 3종이다.
// 서있기의 앞·뒤는 구분해 저장하지 않고 앱이 정한다.
public enum RoomPose {

    // 기본 자세. NULL 도 STAND 와 같게 취급한다.
    STAND,

    // interaction = "desk" 가구에서 재생한다.
    SIT_DESK,

    // interaction = "bed" 가구에서 재생한다.
    SLEEP_BED;

    // 가구를 특정해야 하는 포즈인지. STAND 는 어디서나 취할 수 있어 배치를 가리키지 않는다.
    public boolean requiresPlacement() {
        return this != STAND;
    }

    public static RoomPose from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        try {
            return RoomPose.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
    }
}
