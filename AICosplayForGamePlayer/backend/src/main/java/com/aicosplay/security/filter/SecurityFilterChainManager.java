package com.aicosplay.security.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 安全过滤器链管理器，负责构建和管理责任链
 */
@Component
public class SecurityFilterChainManager {
    
    private final SecurityFilter promptInjectionFilter;
    private final SecurityFilter riskContentFilter;
    private final SecurityFilter modelHallucinationFilter;
    private final SecurityFilter answerAlignmentFilter;
    
    // 责任链的第一个过滤器
    private SecurityFilter firstFilter;
    
    @Autowired
    public SecurityFilterChainManager(PromptInjectionFilter promptInjectionFilter,
                                     RiskContentFilter riskContentFilter,
                                     ModelHallucinationFilter modelHallucinationFilter,
                                     AnswerAlignmentFilter answerAlignmentFilter) {
        this.promptInjectionFilter = promptInjectionFilter;
        this.riskContentFilter = riskContentFilter;
        this.modelHallucinationFilter = modelHallucinationFilter;
        this.answerAlignmentFilter = answerAlignmentFilter;
        
        // 初始化责任链
        buildFilterChain();
    }
    
    /**
     * 构建责任链
     * 按照：提示词注入检测 -> 风险内容检测 -> 模型幻觉检测 -> 回答对齐检测 的顺序构建
     */
    private void buildFilterChain() {
        // 构建责任链
        firstFilter = promptInjectionFilter;
        promptInjectionFilter.setNext(riskContentFilter);
        riskContentFilter.setNext(modelHallucinationFilter);
        modelHallucinationFilter.setNext(answerAlignmentFilter);
        // answerAlignmentFilter 是最后一个过滤器，不需要设置下一个
    }
    
    /**
     * 执行安全过滤链
     * @param context 安全上下文
     * @return 过滤结果
     */
    public boolean executeFilterChain(SecurityContext context) {
        if (firstFilter == null) {
            // 如果责任链为空，直接返回安全
            return true;
        }
        
        // 执行责任链
        return firstFilter.doFilter(context);
    }
    
    /**
     * 创建新的安全上下文
     * @param userPrompt 用户输入的提示词
     * @param context 对话上下文
     * @return 新创建的安全上下文
     */
    public SecurityContext createContext(String userPrompt, String context) {
        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserPrompt(userPrompt);
        securityContext.setContext(context);
        return securityContext;
    }
}