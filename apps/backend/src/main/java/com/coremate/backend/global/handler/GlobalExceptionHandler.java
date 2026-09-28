package com.coremate.backend.global.handler;

import com.coremate.backend.global.exception.CoreMateException;
import com.coremate.backend.global.exception.ErrorCode;
import com.coremate.backend.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CoreMateException.class)
    public ResponseEntity<ErrorResponse> handleCoreMateException(CoreMateException e) {
        return ResponseEntity.status(e.getErrorCode().getStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(message));
    }

    // 요청 본문이 깨졌거나(잘못된 JSON), 비어 있거나, 필드 타입이 안 맞는 경우.
    // 핸들러가 없으면 아래 Exception 핸들러로 떨어져 사용자 입력 오류가 500 으로 나간다.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableRequestBody(HttpMessageNotReadableException e) {
        // 파싱 실패 원인은 로그로만 남긴다. 그대로 내려보내면 내부 필드명·타입이 노출된다.
        log.warn("Malformed request body: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }

    // 매핑이 없는 경로. 핸들러가 없으면 아래 catch-all 로 떨어져 404 가 500 "서버 오류"로 나간다.
    // 폐쇄된 API 를 부르는 앱 화면이 서버 장애처럼 보이므로, 어떤 경로였는지 한 줄만 남기고 404 로 돌려준다.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("No handler for {} /{}", e.getHttpMethod(), e.getResourcePath());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getStatus())
                .body(ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
