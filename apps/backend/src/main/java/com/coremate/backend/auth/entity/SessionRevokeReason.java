package com.coremate.backend.auth.entity;

/**
 * 기기 세션이 해지된 이유. 쫓겨난 기기에 어떤 안내를 띄울지가 이 값으로 갈린다.
 *
 * <p>본인이 이유를 아는 해지(로그아웃·비밀번호 재설정·탈퇴)에까지 "다른 기기에서 로그인" 안내를
 * 띄우면 거짓 침해 경고가 되므로, 사유를 뭉개지 않고 나눠 둔다.
 */
public enum SessionRevokeReason {

    /** 사용자가 직접 로그아웃. 안내 없음. */
    USER_LOGOUT,

    /** 기기 관리 화면에서 원격 해지. */
    REMOTE_LOGOUT,

    /** 활성 기기 상한을 넘겨 밀려남. */
    MAX_SESSION_EXCEEDED,

    /** 비밀번호 재설정으로 전체 해지. 안내 없음. */
    PASSWORD_RESET,

    /** 회원 탈퇴로 전체 해지. 안내 없음. */
    WITHDRAWAL
}
