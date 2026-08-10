package com.coremate.backend.auth.entity;

/**
 * 기기 목록 표시용 플랫폼 구분. 클라이언트가 보낸 값이므로 인가 판단에는 쓰지 않는다.
 */
public enum DevicePlatform {

    IOS,
    ANDROID,
    UNKNOWN;

    /**
     * 클라이언트가 보낸 문자열을 enum 으로 바꾼다. 모르는 값이나 null 은 UNKNOWN 으로 흡수한다
     * (표시용 값 하나 때문에 로그인이 실패하면 안 된다).
     */
    public static DevicePlatform from(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
