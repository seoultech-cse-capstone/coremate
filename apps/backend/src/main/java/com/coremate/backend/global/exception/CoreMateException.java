package com.coremate.backend.global.exception;

import lombok.Getter;

@Getter
public class CoreMateException extends RuntimeException {

    private final ErrorCode errorCode;

    public CoreMateException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
