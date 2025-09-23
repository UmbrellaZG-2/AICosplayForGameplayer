package com.aicosplay.security.filter;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 回答对齐过滤器，用于确保模型的回答与预设的价值观和使用政策保持一致
 */
@Component
public class AnswerAlignmentFilter extends BaseSecurityFilter {
    
    // 价值观对齐关键词
    private static final List<String> POSITIVE_VALUES = Arrays.asList(
            "尊重", "友善", "包容", "理解", "互助", "合作", "和平", "正义"
    );
    
    // 违反对齐的指示词
    private static final List<String> MISALIGNMENT_INDICATORS = Arrays.asList(
            "这是个坏主意",
            "你不应该",
            "最好不要",
            "我不能帮助你",
            "无法提供",
            "不适合",
            "违反规定",
            "政策不允许"
    );
    
    // 检测负面情绪的正则表达式
    private static final Pattern NEGATIVE_EMOTION_REGEX = Pattern.compile(
            "(?i)生气|愤怒|讨厌|憎恨|厌恶|烦躁|沮丧|失望",
            Pattern.CASE_INSENSITIVE
    );
    
    @Override
    protected boolean doFilterInternal(SecurityContext context) {
        String response = context.getRawResponse();
        
        if (response == null || response.trim().isEmpty()) {
            return true; // 空响应视为通过
        }
        
        // 检查是否包含违反对齐的指示词
        for (String indicator : MISALIGNMENT_INDICATORS) {
            if (response.contains(indicator)) {
                // 这些指示词通常表示模型拒绝回答，这是正常的对齐行为
                // 但如果是在不适当的上下文中出现，可能需要进一步处理
                // 这里简化处理，仅记录但不标记为不安全
                continue;
            }
        }
        
        // 检查是否包含负面情绪词汇
        if (NEGATIVE_EMOTION_REGEX.matcher(response).find()) {
            // 模型回答中包含过多负面情绪可能表明对齐问题
            context.setProcessedResponse(modifyNegativeEmotions(response));
        }
        
        // 检查是否符合角色扮演的场景要求
        if (context.getContext() != null && context.getContext().contains("角色扮演")) {
            // 对于角色扮演场景，允许更多的角色化表达，但仍需确保基本对齐
            String processedResponse = ensureRoleplayAlignment(response);
            context.setProcessedResponse(processedResponse);
        }
        
        // 确保回答包含适当的提示以鼓励积极行为
        String finalResponse = addPositiveGuidance(response);
        context.setProcessedResponse(finalResponse);
        
        return true;
    }
    
    /**
     * 修改回答中的负面情绪表达
     */
    private String modifyNegativeEmotions(String response) {
        // 替换负面情绪词汇为更中性或积极的表达
        String modified = response;
        
        modified = modified.replaceAll("(?i)生气", "感到不悦");
        modified = modified.replaceAll("(?i)愤怒", "感到不满");
        modified = modified.replaceAll("(?i)讨厌", "不太喜欢");
        modified = modified.replaceAll("(?i)憎恨", "不太认同");
        
        return modified;
    }
    
    /**
     * 确保角色扮演场景中的回答对齐
     */
    private String ensureRoleplayAlignment(String response) {
        // 在角色扮演场景中，保留角色特点的同时确保内容合规
        // 这里简化处理，仅添加适当的提示
        return response + "\n\n（游戏角色扮演对话，请注意辨别虚拟与现实。）";
    }
    
    /**
     * 添加积极引导
     */
    private String addPositiveGuidance(String response) {
        // 对于较长的回答，添加积极引导
        if (response.length() > 200) {
            return response + "\n\n让我们保持友好和尊重的交流氛围！";
        }
        return response;
    }
}