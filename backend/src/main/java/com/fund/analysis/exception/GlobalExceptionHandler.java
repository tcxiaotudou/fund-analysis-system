package com.fund.analysis.exception;

import com.fund.analysis.dto.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Result<Void>> handleBadRequestException(BadRequestException e) {
        logger.warn("业务参数校验失败: {}", e.getMessage(), e);
        return build(e.getStatus(), e.getMessage());
    }

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<Result<Void>> handleExternalApiException(ExternalApiException e) {
        logger.warn("外部数据服务调用失败", e);
        return build(e.getStatus(), "第三方错误");
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Result<Void>> handleAppException(AppException e) {
        logger.warn("业务处理失败", e);
        return build(e.getStatus(), "服务暂时不可用，请稍后重试");
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        logger.warn("请求参数错误: {}", e.getMessage(), e);
        return build(HttpStatus.BAD_REQUEST, "请求参数有误，请检查后重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        logger.error("系统内部错误", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<Result<Void>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Result.error(status.value(), message));
    }
}
