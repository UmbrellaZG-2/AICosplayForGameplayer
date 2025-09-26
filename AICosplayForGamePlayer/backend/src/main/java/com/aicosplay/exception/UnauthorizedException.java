package com.aicosplay.exception;

/**
 * 未授权异常类，表示用户未登录或会话已过期
 */
public class UnauthorizedException extends RuntimeException {
    
    private static final String DEFAULT_MESSAGE = "用户未登录或会话已过期";
    
    public UnauthorizedException() {
        super(DEFAULT_MESSAGE);
    }
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}