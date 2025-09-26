package com.aicosplay.security.filter;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 风险内容检测过滤器，用于识别和阻止包含不当、违法或有害内容的提示词和响应
 */
@Component
public class RiskContentFilter extends BaseSecurityFilter {
    
    // 风险内容类别
    private static final List<String> VIOLENCE_KEYWORDS = Arrays.asList(
            "暴力", "杀人", "打架", "伤害", "武器", "攻击", "血腥", "屠杀"
    );
    
    private static final List<String> SEXUAL_KEYWORDS = Arrays.asList(
            "色情", "黄色", "成人", "性爱", "裸露", "情色", "性感", "低俗"
    );
    
    private static final List<String> POLITICAL_KEYWORDS = Arrays.asList(
            "政治", "敏感", "领导人", "政权", "政变", "抗议", "游行", "示威"
    );
    
    private static final List<String> ILLEGAL_KEYWORDS = Arrays.asList(
            "违法", "犯罪", "毒品", "赌博", "诈骗", "盗窃", "抢劫", "黑客", "破解"
    );
    
    private static final List<String> DISCRIMINATION_KEYWORDS = Arrays.asList(
            "歧视", "种族", "民族", "宗教", "性别", "偏见", "仇恨", "排斥"
    );
    
    @Override
    protected boolean doFilterInternal(SecurityContext context) {
        // 检查用户输入
        String userPrompt = context.getUserPrompt();
        if (userPrompt != null && !userPrompt.isEmpty()) {
            if (containsRiskContent(userPrompt)) {
                context.markUnsafe("检测到风险内容，该请求无法处理");
                return false;
            }
        }
        
        // 检查模型响应（如果已经生成）
        String rawResponse = context.getRawResponse();
        if (rawResponse != null && !rawResponse.isEmpty()) {
            if (containsRiskContent(rawResponse)) {
                context.markUnsafe("检测到风险内容，该响应无法返回");
                return false;
            }
            
            // 如果通过检查，净化响应内容
            String processedResponse = sanitizeResponse(rawResponse);
            context.setProcessedResponse(processedResponse);
        }
        
        return true;
    }
    
    /**
     * 检查内容是否包含风险词汇
     * @param content 要检查的内容
     * @return 是否包含风险内容
     */
    private boolean containsRiskContent(String content) {
        content = content.toLowerCase();
        
        // 检查各类风险关键词
        if (containsAnyKeywords(content, VIOLENCE_KEYWORDS)) {
            return true;
        }
        
        if (containsAnyKeywords(content, SEXUAL_KEYWORDS)) {
            return true;
        }
        
        if (containsAnyKeywords(content, POLITICAL_KEYWORDS)) {
            return true;
        }
        
        if (containsAnyKeywords(content, ILLEGAL_KEYWORDS)) {
            return true;
        }
        
        if (containsAnyKeywords(content, DISCRIMINATION_KEYWORDS)) {
            return true;
        }
        
        // 检查特殊字符和模式
        if (containsSuspiciousPatterns(content)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查内容是否包含任何关键词列表中的词汇
     */
    private boolean containsAnyKeywords(String content, List<String> keywords) {
        for (String keyword : keywords) {
            if (content.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查内容是否包含可疑模式
     */
    private boolean containsSuspiciousPatterns(String content) {
        // 检查重复字符
        Pattern repeatedCharsPattern = Pattern.compile("(.)\\1{4,}");
        if (repeatedCharsPattern.matcher(content).find()) {
            return true;
        }
        
        // 检查特殊符号组合
        Pattern specialCharsPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\\\",.<>\\/?]{5,}");
        if (specialCharsPattern.matcher(content).find()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 净化响应内容
     */
    private String sanitizeResponse(String response) {
        // 在实际应用中，可以实现更复杂的内容净化逻辑
        // 这里简化处理，只做基本的过滤
        String sanitized = response;
        
        // 替换敏感词汇（示例）
        for (String keyword : VIOLENCE_KEYWORDS) {
            sanitized = sanitized.replaceAll("(?i)" + keyword, "***");
        }
        
        return sanitized;
    }
}