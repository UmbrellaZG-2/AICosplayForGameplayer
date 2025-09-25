package com.aicosplay.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.aicosplay.constant.ErrorCode;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.exception.EmailAlreadyExistsException;
import com.aicosplay.exception.ServiceException;
import com.aicosplay.exception.UnauthorizedException;
import com.aicosplay.exception.UserNotFoundException;
import com.aicosplay.exception.UsernameAlreadyExistsException;
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
                ApiResponse.error(ErrorCode.PARAM_VALIDATION_ERROR, ex.getErrorMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理服务层异常
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<?>> handleServiceException(ServiceException ex, WebRequest request) {
        // 根据具体的异常类型返回对应的错误代码
        if (ex instanceof UsernameAlreadyExistsException) {
            return new ResponseEntity<>(
                    ApiResponse.error(ErrorCode.USERNAME_ALREADY_EXISTS),
                    HttpStatus.BAD_REQUEST
            );
        } else if (ex instanceof EmailAlreadyExistsException) {
            return new ResponseEntity<>(
                    ApiResponse.error(ErrorCode.EMAIL_ALREADY_EXISTS),
                    HttpStatus.BAD_REQUEST
            );
        } else if (ex instanceof UserNotFoundException) {
            return new ResponseEntity<>(
                    ApiResponse.error(ErrorCode.USER_NOT_FOUND),
                    HttpStatus.NOT_FOUND
            );
        }
        
        // 通用服务异常
        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.SYSTEM_ERROR, ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    /**
     * 处理安全异常
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<?>> handleSecurityException(SecurityException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.PERMISSION_DENIED),
                HttpStatus.FORBIDDEN
        );
    }

    /**
     * 处理未授权异常（401错误）
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        return new ResponseEntity<>(
                ApiResponse.error(ErrorCode.AUTHENTICATION_FAILED),
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
                ApiResponse.error(ErrorCode.SYSTEM_ERROR),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}