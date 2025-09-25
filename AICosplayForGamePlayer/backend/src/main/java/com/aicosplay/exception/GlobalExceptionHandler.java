package com.aicosplay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.aicosplay.model.ApiResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器，用于统一处理项目中的异常
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getErrorMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理服务层异常
     */
    @ExceptionHandler(ServiceException.class) // 包括EmailAlreadyExistsException, UsernameAlreadyExistsException等子类
    public ResponseEntity<ApiResponse<?>> handleServiceException(ServiceException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理安全异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityException(SecurityException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.forbidden(),
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * 处理未授权异常（401错误）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.unauthorized(),
                HttpStatus.UNAUTHORIZED
        );
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAllExceptions(Exception ex, WebRequest request) {
        // 记录详细错误信息到日志
        ex.printStackTrace();

        return new ResponseEntity<>(
                ApiResponse.internalError("服务器内部错误，请稍后再试"),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}