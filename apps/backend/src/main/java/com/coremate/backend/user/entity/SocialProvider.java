package com.coremate.backend.user.entity;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;

/**
 * 소셜 로그인 제공자. path 파라미터(kakao 등)를 대소문자 무시로 파싱한다.
 */
public enum SocialProvider {
    GOOGLE, KAKAO, NAVER;

    public static SocialProvider from(String value) {
        if (value == null || value.isBlank()) {
            throw new CoreMateException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        try {
            return SocialProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CoreMateException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }
}
