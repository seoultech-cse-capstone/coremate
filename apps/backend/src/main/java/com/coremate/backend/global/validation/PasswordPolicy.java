package com.coremate.backend.global.validation;

/**
 * 비밀번호 형식 규칙(FR-AUTH-01). 회원가입·비밀번호 변경·비밀번호 재설정이 같은 값을 쓴다.
 *
 * <p>명세는 대문자·소문자·숫자·특수문자 {@code !@#$%^&*} 를 각 1개 이상 "포함"할 것만 요구하고
 * 그 밖의 문자를 금지하지 않으므로, 포함 여부만 선행 탐색으로 확인하고 문자 집합은 제한하지 않는다.
 *
 * <p>로그인·탈퇴처럼 기존 비밀번호를 대조하기만 하는 요청에는 적용하지 않는다.
 * 규칙 도입 전에 가입한 계정이 로그인조차 못 하게 되기 때문이다.
 */
public final class PasswordPolicy {

    public static final String REGEXP = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,}$";

    public static final String MESSAGE =
            "비밀번호는 대문자·소문자·숫자·특수문자(!@#$%^&*)를 각각 1개 이상 포함해 8자 이상이어야 합니다";

    private PasswordPolicy() {
    }
}
