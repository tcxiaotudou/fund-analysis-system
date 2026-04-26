package com.fund.analysis.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public BusinessException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
