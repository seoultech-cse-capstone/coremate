package com.coremate.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "아직 구현되지 않은 기능입니다"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key 헤더가 필요합니다"),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "이미 처리된 요청입니다"),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "비활성화된 계정입니다"),
    INVALID_VERIFICATION_CODE(HttpStatus.UNAUTHORIZED, "인증번호가 올바르지 않습니다"),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "인증번호가 만료되었거나 발급되지 않았습니다"),
    TOO_MANY_VERIFICATION_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해 주세요"),
    INVALID_RESET_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않거나 만료된 재설정 토큰입니다"),
    SESSION_REVOKED_BY_OTHER_DEVICE(HttpStatus.UNAUTHORIZED, "다른 기기에서 로그인하여 로그아웃되었습니다"),
    SESSION_REVOKED_BY_USER(HttpStatus.UNAUTHORIZED, "기기 관리에서 이 기기의 로그인이 해지되었습니다"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요"),
    CANNOT_REVOKE_CURRENT_SESSION(HttpStatus.BAD_REQUEST, "현재 사용 중인 기기입니다. 로그아웃을 이용해 주세요"),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았거나 만료되었습니다"),
    INVALID_OAUTH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 소셜 로그인 토큰입니다"),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "이미 탈퇴한 계정입니다"),

    // Content
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "과목을 찾을 수 없습니다"),
    CATEGORY_LV1_NOT_FOUND(HttpStatus.NOT_FOUND, "대분류를 찾을 수 없습니다"),
    CATEGORY_LV2_NOT_FOUND(HttpStatus.NOT_FOUND, "중분류를 찾을 수 없습니다"),
    CONCEPT_NOT_FOUND(HttpStatus.NOT_FOUND, "개념을 찾을 수 없습니다"),
    INVALID_CONTENT_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 데이터가 손상되었습니다"),

    // ProblemSet
    PROBLEM_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "문제집을 찾을 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다"),
    QUESTION_HINT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 문제에 사용 가능한 힌트가 없습니다"),

    // Bookmark
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다"),

    // Friend
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "친구 관계를 찾을 수 없습니다"),
    FRIEND_POKE_DAILY_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "오늘 보낼 수 있는 콕 찌르기를 모두 사용했습니다"),
    FRIEND_POKE_FRIEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "같은 친구에게는 하루 3번까지 보낼 수 있습니다"),
    FRIEND_SELF_REQUEST(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 요청을 보낼 수 없습니다"),
    FRIEND_REQUEST_DUPLICATED(HttpStatus.CONFLICT, "이미 친구이거나 요청을 보낸 사용자입니다"),
    FRIEND_REQUEST_NOT_RECEIVED(HttpStatus.FORBIDDEN, "본인에게 온 친구 요청만 처리할 수 있습니다"),
    FRIEND_RELATION_REQUIRED(HttpStatus.FORBIDDEN, "친구 관계인 사용자만 확인할 수 있습니다"),
    INVALID_FRIEND_STATUS(HttpStatus.BAD_REQUEST, "친구 요청 응답 상태가 올바르지 않습니다"),
    FRIEND_BLOCKED_BY_ME(HttpStatus.CONFLICT, "차단한 사용자입니다. 차단을 해제한 뒤 요청해 주세요"),
    FRIEND_BLOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "차단한 사용자가 아닙니다"),
    FRIEND_LEARNING_PRIVATE(HttpStatus.FORBIDDEN, "친구가 학습 기록을 공개하지 않았습니다"),

    // Notification
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다"),
    NOTIFICATION_CATEGORY_NOT_CONFIGURABLE(HttpStatus.BAD_REQUEST, "계정 보안 알림은 수신 설정을 변경할 수 없습니다"),

    // DailyQuiz / Attendance
    DAILY_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "오늘의 CS 퀴즈를 찾을 수 없습니다"),
    DAILY_QUIZ_INCOMPLETE(HttpStatus.BAD_REQUEST, "오늘의 CS 퀴즈 답안이 완성되지 않았습니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출석 기록을 찾을 수 없습니다"),

    // Avatar / Inventory
    AVATAR_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "아바타 아이템을 찾을 수 없습니다"),
    USER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "보유 아이템을 찾을 수 없습니다"),
    AVATAR_ITEM_NOT_EQUIPPABLE(HttpStatus.BAD_REQUEST, "장착할 수 없는 아이템입니다"),
    INVALID_AVATAR_SLOT(HttpStatus.BAD_REQUEST, "아바타 장착 슬롯이 올바르지 않습니다"),
    // EX-ITEM-007. 머리·눈·입·벽지·바닥은 비우면 렌더가 깨져 교체만 허용한다.
    REQUIRED_AVATAR_SLOT(HttpStatus.BAD_REQUEST, "이 부위는 비워둘 수 없습니다. 다른 아이템으로 바꿔주세요"),
    // 방 가구 배치 검증 4가지. 하나라도 걸리면 배치 저장 전체를 롤백한다.
    FURNITURE_NOT_OWNED(HttpStatus.BAD_REQUEST, "보유하지 않은 가구는 방에 놓을 수 없습니다"),
    ROOM_PLACEMENT_OUT_OF_BOUNDS(HttpStatus.BAD_REQUEST, "방 밖에는 가구를 놓을 수 없습니다"),
    ROOM_PLACEMENT_OVERLAP(HttpStatus.BAD_REQUEST, "가구끼리 겹쳐 놓을 수 없습니다. 자리를 옮겨주세요"),
    ROOM_PLACEMENT_EXCEEDS_QUANTITY(HttpStatus.BAD_REQUEST, "보유한 개수보다 많이 놓을 수 없습니다"),
    // EX-ITEM-008. 앱이 자동으로 보내는 값이라 사용자에게 보여줄 안내는 없다.
    INVALID_ROOM_POSE(HttpStatus.BAD_REQUEST, "저장할 수 없는 아바타 포즈입니다"),

    // Shop / RandomBox
    SHOP_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상점 상품을 찾을 수 없습니다"),
    SHOP_LEVEL_REQUIRED(HttpStatus.FORBIDDEN, "상품 구매에 필요한 레벨이 부족합니다"),
    UNSUPPORTED_SHOP_PRODUCT(HttpStatus.BAD_REQUEST, "아직 지원하지 않는 상점 상품입니다"),
    RANDOM_BOX_NOT_FOUND(HttpStatus.NOT_FOUND, "랜덤박스를 찾을 수 없습니다"),
    RANDOM_BOX_REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "랜덤박스 보상 후보를 찾을 수 없습니다"),
    INVALID_RANDOM_BOX_POLICY(HttpStatus.INTERNAL_SERVER_ERROR, "랜덤박스 보상 정책이 올바르지 않습니다"),

    // Solve
    SOLVE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "풀이 세션을 찾을 수 없습니다"),
    SOLVE_SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 풀이 세션입니다"),
    ANSWER_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출된 문제입니다"),
    SOLVE_NOT_ALL_ANSWERED(HttpStatus.BAD_REQUEST, "아직 제출하지 않은 문제가 있습니다"),
    SOLVE_SESSION_EXPIRED(HttpStatus.CONFLICT, "제한 시간이 지나 종료된 모의고사입니다. 새로 시작해주세요"),
    SOLVE_SESSION_ABANDONED(HttpStatus.CONFLICT, "이미 중단된 풀이 세션입니다"),
    UNSUPPORTED_SESSION_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 풀이 세션 유형입니다"),

    // Weakness
    WEAKNESS_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "약점 분석 리포트를 찾을 수 없습니다"),
    WEAKNESS_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "약점 분석 중 오류가 발생했습니다"),
    WEAKNESS_SELECTION_EMPTY(HttpStatus.BAD_REQUEST, "분석 대상 회차를 1개 이상 선택해야 합니다"),
    WEAKNESS_SELECTION_TOO_MANY(HttpStatus.BAD_REQUEST, "분석 대상은 최대 5개까지 선택할 수 있습니다"),
    WEAKNESS_DUPLICATE_SESSION(HttpStatus.BAD_REQUEST, "중복된 풀이 회차를 선택했습니다"),
    WEAKNESS_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "선택한 풀이 회차를 찾을 수 없습니다"),
    WEAKNESS_SESSION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료되지 않은 풀이 회차는 분석할 수 없습니다"),
    WEAKNESS_SESSION_NOT_MOCK(HttpStatus.BAD_REQUEST, "모의고사 회차만 분석 대상으로 선택할 수 있습니다"),
    WEAKNESS_MIXED_SUBJECT(HttpStatus.BAD_REQUEST, "서로 다른 과목의 회차를 함께 분석할 수 없습니다"),
    WEAKNESS_INSUFFICIENT_EVIDENCE(HttpStatus.BAD_REQUEST, "아직 반복적으로 확인된 약점이 없습니다. 같은 범위의 문제집을 조금 더 완료한 뒤 다시 분석해 주세요"),

    // Reward
    REWARD_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "보상 정책을 찾을 수 없습니다"),
    INSUFFICIENT_COIN(HttpStatus.BAD_REQUEST, "보유 코인이 부족합니다"),
    INVALID_REWARD_AMOUNT(HttpStatus.BAD_REQUEST, "보상 수량이 올바르지 않습니다"),
    DAILY_REWARD_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "일일 보상 한도를 초과했습니다"),
    // 광고 시청 완료 콜백의 서명이 구글 공개키로 확인되지 않았다. 구글이 아닌 곳에서 온 요청이다.
    INVALID_AD_REWARD_SIGNATURE(HttpStatus.BAD_REQUEST, "광고 보상 확인 서명이 올바르지 않습니다"),

    // Support
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다"),
    BUG_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "버그 신고를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
