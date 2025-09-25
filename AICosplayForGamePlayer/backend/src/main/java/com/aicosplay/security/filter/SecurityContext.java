package com.aicosplay.security.filter;

/**
 * 安全上下文类，用于在责任链过滤器之间传递数据
 */
public class SecurityContext {
    
    // 用户输入的原始提示词
    private String userPrompt;
    
    // 处理后的提示词（可能经过净化或修改）
    private String processedPrompt;
    
    // 大模型生成的原始回复
    private String rawResponse;
    
    // 处理后的回复（可能经过净化或修改）
    private String processedResponse;
    
    // 错误消息，如果检查未通过
    private String errorMessage;
    
    // 上下文信息
    private String context;
    
    // 安全检查结果
    private boolean isSafe = true;
    
    /**
     * 标记安全检查失败
     * @param errorMessage 失败原因
     */
    public void markUnsafe(String errorMessage) {
        this.isSafe = false;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public String getProcessedPrompt() {
        return processedPrompt;
    }

    public void setProcessedPrompt(String processedPrompt) {
        this.processedPrompt = processedPrompt;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getProcessedResponse() {
        return processedResponse;
    }

    public void setProcessedResponse(String processedResponse) {
        this.processedResponse = processedResponse;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public boolean isSafe() {
        return isSafe;
    }

    public void setSafe(boolean safe) {
        isSafe = safe;
    }
}