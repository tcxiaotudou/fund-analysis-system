package com.fund.analysis.exception;

import org.springframework.http.HttpStatus;

/**
 * 应用异常基类，用于把业务错误映射到明确的 HTTP 状态码。
 */
public class AppException extends RuntimeException {

    private final HttpStatus status;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
