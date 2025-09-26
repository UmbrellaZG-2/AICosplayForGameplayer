package com.aicosplay.security.filter;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * Prompt注入检测过滤器，用于检测和防范提示词注入攻击
 */
@Component
public class PromptInjectionFilter extends BaseSecurityFilter {
    
    // 常见的提示词注入模式
    private static final List<String> INJECTION_PATTERNS = Arrays.asList(
            "ignore previous",
            "forget instructions",
            "system: ",
            "override instructions",
            "you are now",
            "new role:",
            "disregard prior",
            "change your behavior",
            "ignore all previous",
            "pretend to be"
    );
    
    // 正则表达式模式，用于检测潜在的注入尝试
    private static final Pattern INJECTION_REGEX = Pattern.compile(
            "(?i)(ignore|forget|override|disregard).*?(previous|instructions|prior)|system:\s*|",
            Pattern.CASE_INSENSITIVE
    );
    
    @Override
    protected boolean doFilterInternal(SecurityContext context) {
        String prompt = context.getUserPrompt();
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return true; // 空提示词视为通过
        }
        
        // 检查常见的注入模式
        for (String pattern : INJECTION_PATTERNS) {
            if (prompt.toLowerCase().contains(pattern.toLowerCase())) {
                context.markUnsafe("检测到潜在的提示词注入尝试: " + pattern);
                return false;
            }
        }
        
        // 使用正则表达式进一步检测
        if (INJECTION_REGEX.matcher(prompt).find()) {
            context.markUnsafe("检测到潜在的提示词注入模式");
            return false;
        }
        
        // 如果通过检查，复制用户提示词到处理后的提示词
        context.setProcessedPrompt(prompt);
        return true;
    }
}