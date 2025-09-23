package com.aicosplay.security.filter;

import lombok.Data;

/**
 * 安全上下文类，用于在责任链过滤器之间传递数据
 */
@Data
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
}