package com.aicosplay.exception;

/**
 * 用户名已存在异常
 */
public class UsernameAlreadyExistsException extends ServiceException {
    private static final String ERROR_CODE = "USERNAME_EXISTS";
    
    public UsernameAlreadyExistsException() {
        super("用户名已被使用", ERROR_CODE);
    }
    
    public UsernameAlreadyExistsException(String message) {
        super(message, ERROR_CODE);
    }
    
    public UsernameAlreadyExistsException(String message, Throwable cause) {
        super(message, ERROR_CODE, cause);
    }
}