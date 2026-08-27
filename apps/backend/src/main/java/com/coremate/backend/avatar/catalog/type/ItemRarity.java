package com.coremate.backend.avatar.catalog.type;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

// 아이템 등급. 종류와 무관하게 모든 아이템이 하나씩 갖는다.
// 중복 획득 시 환급 코인은 여기에만 적는다 — 이 숫자가 코드에 두 번 나오면 안 된다.
// 박스 등급별 확률은 random_box_items 시드가 정본이라 여기에 두지 않는다.
public enum ItemRarity {

    COMMON(20),
    RARE(50),
    EPIC(100),
    LEGENDARY(200);

    private final int refundCoin;

    ItemRarity(int refundCoin) {
        this.refundCoin = refundCoin;
    }

    public int getRefundCoin() {
        return refundCoin;
    }

    public static ItemRarity from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
        try {
            return ItemRarity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreMateException(ErrorCode.INVALID_INPUT);
        }
    }
}
