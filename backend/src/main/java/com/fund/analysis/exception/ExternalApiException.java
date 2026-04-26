package com.fund.analysis.exception;

import org.springframework.http.HttpStatus;

public class ExternalApiException extends AppException {

    public ExternalApiException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
