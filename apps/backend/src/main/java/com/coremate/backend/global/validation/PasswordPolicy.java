package com.coremate.backend.global.validation;

/**
 * 비밀번호 형식 규칙(FR-AUTH-01). 회원가입·비밀번호 변경·비밀번호 재설정이 같은 값을 쓴다.
 *
 * <p>대문자·소문자·숫자·특수문자 {@code !@#$%^&*} 를 각 1개 이상 포함한 8자 이상이며,
 * 허용 문자는 영문·숫자·이 8개 특수문자로 제한한다. 명세 자체는 "포함"만 요구해 문자 집합을
 * 열어 둘 수도 있지만, 앱(shared/constants/passwordRule.ts)이 이미 제한하고 있어 서버만
 * 느슨하면 앱에서는 못 만드는 비밀번호가 API 직접 호출로는 만들어진다. 둘을 같은 규칙으로 둔다.
 *
 * <p>로그인·탈퇴처럼 기존 비밀번호를 대조하기만 하는 요청에는 적용하지 않는다.
 * 규칙 도입 전에 가입한 계정이 로그인조차 못 하게 되기 때문이다.
 */
public final class PasswordPolicy {

    public static final String REGEXP =
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$";

    public static final String MESSAGE =
            "비밀번호는 대문자·소문자·숫자·특수문자(!@#$%^&*)를 각각 1개 이상 포함해 8자 이상이어야 합니다";

    private PasswordPolicy() {
    }
}
