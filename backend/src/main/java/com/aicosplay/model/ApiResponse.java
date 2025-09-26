package com.aicosplay.model;

import com.aicosplay.constant.ErrorCode;

/**
 * 通用API响应类，用于统一所有接口的响应格式
 */
public class ApiResponse<T> {
    
    /**
     * 响应状态码
     * 200：成功
     * 400：请求参数错误
     * 401：未授权
     * 403：禁止访问
     * 404：资源不存在
     * 500：服务器内部错误
     */
    private int code;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应数据
     */
    private T data;
    
    /**
     * 无参构造函数
     */
    public ApiResponse() {
    }
    
    /**
     * 全参构造函数
     * @param code 响应状态码
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    /**
     * 获取响应状态码
     * @return 状态码
     */
    public int getCode() {
        return code;
    }
    
    /**
     * 设置响应状态码
     * @param code 状态码
     */
    public void setCode(int code) {
        this.code = code;
    }
    
    /**
     * 获取响应消息
     * @return 消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 设置响应消息
     * @param message 消息
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * 获取响应数据
     * @return 数据
     */
    public T getData() {
        return data;
    }
    
    /**
     * 设置响应数据
     * @param data 数据
     */
    public void setData(T data) {
        this.data = data;
    }
    /**
     * 创建成功响应
     * @param data 响应数据
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }
    
    /**
     * 创建成功响应（无数据）
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(200, "操作成功", null);
    }
    
    /**
     * 创建错误响应
     * @param code 错误码
     * @param message 错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    /**
     * 使用ErrorCode枚举创建错误响应
     * @param errorCode 错误代码枚举
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }
    
    /**
     * 使用ErrorCode枚举创建错误响应（带自定义消息）
     * @param errorCode 错误代码枚举
     * @param customMessage 自定义错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String customMessage) {
        return new ApiResponse<>(errorCode.getCode(), customMessage, null);
    }
    
    /**
     * 创建未授权错误响应
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> unauthorized() {
        return new ApiResponse<>(401, "用户未登录或会话已过期", null);
    }
    
    /**
     * 创建禁止访问错误响应
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> forbidden() {
        return new ApiResponse<>(403, "您没有权限执行此操作", null);
    }
    
    /**
     * 创建参数错误响应
     * @param message 错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return new ApiResponse<>(400, message, null);
    }
    
    /**
     * 创建服务器内部错误响应
     * @param message 错误消息
     * @return ApiResponse实例
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return new ApiResponse<>(500, message, null);
    }
}