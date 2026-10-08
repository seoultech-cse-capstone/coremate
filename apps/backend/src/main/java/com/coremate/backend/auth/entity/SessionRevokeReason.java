package com.coremate.backend.auth.entity;

/**
 * 기기 세션이 해지된 이유. 쫓겨난 기기에 어떤 안내를 띄울지가 이 값으로 갈린다.
 *
 * <p>본인이 이유를 아는 해지(로그아웃·비밀번호 재설정·비밀번호 변경·탈퇴)에까지 "다른 기기에서 로그인"
 * 안내를 띄우면 거짓 침해 경고가 되므로, 사유를 뭉개지 않고 나눠 둔다.
 *
 * <p>DB 의 revoke_reason 은 MySQL native enum 이라, 값을 추가하면 마이그레이션으로 컬럼 목록도 바꿔야 한다.
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
    WITHDRAWAL,

    /**
     * 로그인 상태 비밀번호 변경으로 지금 기기를 뺀 나머지 해지. 안내 없음.
     *
     * <p>PASSWORD_RESET 을 쓰지 않는 이유: 재설정은 전 기기, 변경은 다른 기기만 끊는 별개 사건이라
     * 기록이 섞이면 나중에 어느 쪽인지 가릴 수 없다. REMOTE_LOGOUT 을 쓰지 않는 이유: 쫓겨난 기기에
     * "기기 관리에서 해지되었습니다" 안내가 떠 본인이 하지 않은 일처럼 보인다.
     */
    PASSWORD_CHANGE
}
