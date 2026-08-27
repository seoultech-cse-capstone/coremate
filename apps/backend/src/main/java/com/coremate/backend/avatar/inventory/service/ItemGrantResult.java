package com.coremate.backend.avatar.inventory.service;

// 아이템 지급 결과. 호출자는 outcome 만 보고 분기하면 되고
// item_type / rarity / is_default 를 알 필요가 없다.
public record ItemGrantResult(
        Outcome outcome,
        Long userItemId,
        int refundCoin
) {

    public enum Outcome {
        // 미보유였다. user_items 에 새 행이 생겼다.
        GRANTED,

        // FURNITURE 중복이다. 여러 개 배치할 수 있어 수량을 늘렸다.
        QUANTITY_INCREASED,

        // 그 밖의 중복이다. user_items 는 그대로고 코인을 환급해야 한다.
        DUPLICATE_REFUNDABLE
    }

    public static ItemGrantResult granted(Long userItemId) {
        return new ItemGrantResult(Outcome.GRANTED, userItemId, 0);
    }

    public static ItemGrantResult quantityIncreased(Long userItemId) {
        return new ItemGrantResult(Outcome.QUANTITY_INCREASED, userItemId, 0);
    }

    // 환급 금액은 등급에서만 나온다. 호출자가 다시 계산하지 않는다.
    public static ItemGrantResult duplicateRefundable(int refundCoin) {
        return new ItemGrantResult(Outcome.DUPLICATE_REFUNDABLE, null, refundCoin);
    }

    public boolean isRefundable() {
        return outcome == Outcome.DUPLICATE_REFUNDABLE;
    }
}
