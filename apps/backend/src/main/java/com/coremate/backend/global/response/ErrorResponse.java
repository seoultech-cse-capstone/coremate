package com.coremate.backend.global.response;

import com.coremate.backend.global.exception.ErrorCode;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(String message) {
        return new ErrorResponse("INVALID_INPUT", message);
    }
}
