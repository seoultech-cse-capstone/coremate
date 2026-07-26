package com.coremate.backend.reward.entity.type;

// 중복 보상 여부를 판단할 범위.
public enum RewardScope {
    PER_SESSION,
    PER_CONCEPT,
    PER_CATEGORY_LV1,
    PER_CATEGORY_LV2,
    PER_SUBJECT,
    PER_DAY,
    PER_LEVEL,
    PER_USER
}
