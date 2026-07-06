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

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다"),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "리프레시 토큰을 찾을 수 없습니다"),
    ACCOUNT_NOT_ACTIVE(HttpStatus.FORBIDDEN, "비활성화된 계정입니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

    // Content
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "과목을 찾을 수 없습니다"),
    CONCEPT_NOT_FOUND(HttpStatus.NOT_FOUND, "개념을 찾을 수 없습니다"),
    INVALID_CONTENT_DATA(HttpStatus.INTERNAL_SERVER_ERROR, "콘텐츠 데이터가 손상되었습니다"),

    // ProblemSet
    PROBLEM_SET_NOT_FOUND(HttpStatus.NOT_FOUND, "문제집을 찾을 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "문제를 찾을 수 없습니다"),

    // Bookmark
    BOOKMARK_NOT_FOUND(HttpStatus.NOT_FOUND, "북마크를 찾을 수 없습니다"),

    // DailyQuiz / Attendance
    DAILY_QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "오늘의 CS 퀴즈를 찾을 수 없습니다"),
    DAILY_QUIZ_INCOMPLETE(HttpStatus.BAD_REQUEST, "오늘의 CS 퀴즈 답안이 완성되지 않았습니다"),
    ATTENDANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "출석 기록을 찾을 수 없습니다"),

    // Solve
    SOLVE_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "풀이 세션을 찾을 수 없습니다"),
    SOLVE_SESSION_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 완료된 풀이 세션입니다"),

    // Weakness
    WEAKNESS_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "약점 분석 리포트를 찾을 수 없습니다"),
    WEAKNESS_ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "약점 분석 중 오류가 발생했습니다"),

    // Reward
    REWARD_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "보상 정책을 찾을 수 없습니다"),
    INSUFFICIENT_COIN(HttpStatus.BAD_REQUEST, "보유 코인이 부족합니다"),
    INVALID_REWARD_AMOUNT(HttpStatus.BAD_REQUEST, "보상 수량이 올바르지 않습니다"),
    DAILY_REWARD_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "일일 보상 한도를 초과했습니다");

    private final HttpStatus status;
    private final String message;
}
