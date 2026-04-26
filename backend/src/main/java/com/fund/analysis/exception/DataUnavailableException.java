package com.fund.analysis.exception;

import org.springframework.http.HttpStatus;

public class DataUnavailableException extends AppException {

    public DataUnavailableException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public DataUnavailableException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message, cause);
    }
}
