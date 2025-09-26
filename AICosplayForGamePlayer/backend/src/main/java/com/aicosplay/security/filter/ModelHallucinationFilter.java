package com.aicosplay.security.filter;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.List;

/**
 * 模型幻觉检测过滤器，用于识别和减少模型生成的幻觉内容
 */
@Component
public class ModelHallucinationFilter extends BaseSecurityFilter {
    
    // 常见的幻觉指示词
    private static final List<String> HALLUCINATION_INDICATORS = Arrays.asList(
            "据报道",
            "研究表明",
            "科学家发现",
            "专家表示",
            "根据数据",
            "统计显示",
            "研究发现",
            "最新研究",
            "有研究指出"
    );
    
    // 检测过于绝对的陈述
    private static final Pattern ABSOLUTE_STATEMENT_REGEX = Pattern.compile(
            "(?i)绝对|完全|必定|必然|100%|一定|毫无疑问",
            Pattern.CASE_INSENSITIVE
    );
    
    @Override
    protected boolean doFilterInternal(SecurityContext context) {
        // 这个过滤器主要处理模型的响应，而不是用户的提示词
        // 所以在生成响应后才会执行有效的检查
        String response = context.getRawResponse();
        
        if (response == null || response.trim().isEmpty()) {
            return true; // 空响应视为通过
        }
        
        int hallucinationScore = 0;
        
        // 检查幻觉指示词
        for (String indicator : HALLUCINATION_INDICATORS) {
            if (response.contains(indicator)) {
                hallucinationScore++;
            }
        }
        
        // 检查绝对陈述
        if (ABSOLUTE_STATEMENT_REGEX.matcher(response).find()) {
            hallucinationScore++;
        }
        
        // 检查响应长度是否异常（过短或过长可能表示问题）
        int responseLength = response.length();
        if (responseLength < 10 || responseLength > 1000) {
            hallucinationScore++;
        }
        
        // 如果幻觉分数超过阈值，标记为不安全
        if (hallucinationScore >= 3) {
            context.markUnsafe("检测到可能的模型幻觉内容，响应包含过多未经证实的陈述");
            return false;
        }
        
        // 如果通过检查，添加免责声明
        String processedResponse = addDisclaimer(response);
        context.setProcessedResponse(processedResponse);
        
        return true;
    }
    
    /**
     * 添加免责声明到响应中
     * @param response 原始响应
     * @return 添加免责声明后的响应
     */
    private String addDisclaimer(String response) {
        // 对于可能包含幻觉的内容，添加适当的免责声明
        return response + "\n\n（注：以上内容基于AI生成，可能不完全准确，请谨慎参考。）";
    }
}