package com.aicosplay.exception;

/**
 * 用户未找到异常
 */
public class UserNotFoundException extends ServiceException {
    private static final String ERROR_CODE = "USER_NOT_FOUND";
    
    public UserNotFoundException() {
        super("用户不存在", ERROR_CODE);
    }
    
    public UserNotFoundException(String message) {
        super(message, ERROR_CODE);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}