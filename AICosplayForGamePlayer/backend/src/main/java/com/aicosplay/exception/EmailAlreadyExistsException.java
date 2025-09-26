package com.aicosplay.exception;

/**
 * 邮箱已被使用异常
 */
public class EmailAlreadyExistsException extends ServiceException {
    private static final String ERROR_CODE = "EMAIL_EXISTS";
    
    public EmailAlreadyExistsException() {
        super("邮箱已被使用", ERROR_CODE);
    }
    
    public EmailAlreadyExistsException(String message) {
        super(message, ERROR_CODE);
    }
    
    public EmailAlreadyExistsException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}