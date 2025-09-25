package com.aicosplay.service;

import com.aicosplay.exception.BusinessException;
import com.aicosplay.security.filter.SecurityContext;
import com.aicosplay.security.filter.SecurityFilterChainManager;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIService {

    private final ChatClient chatClient;
    private final SecurityFilterChainManager securityFilterChainManager;

    @Autowired
    public AIService(ChatClient chatClient, SecurityFilterChainManager securityFilterChainManager) {
        this.chatClient = chatClient;
        this.securityFilterChainManager = securityFilterChainManager;
    }

    /**
     * 使用大模型生成回复，并进行安全检查
     * @param prompt 用户输入的提示词
     * @return 大模型生成的回复内容
     * @throws SecurityException 如果安全检查未通过
     */
    public String generateResponse(String prompt) {
        // 创建安全上下文
        SecurityContext context = securityFilterChainManager.createContext(prompt, null);
        
        // 执行输入安全检查（责任链的前两个过滤器：提示词注入检测和风险内容检测）
        if (!securityFilterChainManager.executeFilterChain(context)) {
            throw new SecurityException("输入安全检查未通过: " + context.getErrorMessage());
        }
        
        // 使用经过处理的提示词调用大模型
        String safePrompt = context.getProcessedPrompt() != null ? context.getProcessedPrompt() : prompt;
        Message userMessage = new UserMessage(safePrompt);
        Prompt requestPrompt = new Prompt(List.of(userMessage));
        ChatResponse response = chatClient.call(requestPrompt);
        
        // 获取原始响应
        String rawResponse = response.getResult().getOutput().getContent();
        context.setRawResponse(rawResponse);
        
        // 执行输出安全检查（继续执行责任链的剩余过滤器：模型幻觉检测和回答对齐检测）
        if (!securityFilterChainManager.executeFilterChain(context)) {
            throw new SecurityException("输出安全检查未通过: " + context.getErrorMessage());
        }
        
        // 返回经过处理的安全响应
        return context.getProcessedResponse() != null ? context.getProcessedResponse() : rawResponse;
    }

    /**
     * 使用大模型生成回复，支持上下文，并进行安全检查
     * @param prompt 用户输入的提示词
     * @param context 对话上下文（历史消息）
     * @return 大模型生成的回复内容
     * @throws SecurityException 如果安全检查未通过
     */
    public String generateResponseWithContext(String prompt, String context) {
        // 创建安全上下文
        SecurityContext securityContext = securityFilterChainManager.createContext(prompt, context);
        
        // 执行输入安全检查
        if (!securityFilterChainManager.executeFilterChain(securityContext)) {
            throw new SecurityException("输入安全检查未通过: " + securityContext.getErrorMessage());
        }
        
        // 构建包含上下文的完整提示词
        String processedPrompt = securityContext.getProcessedPrompt() != null ? 
                                 securityContext.getProcessedPrompt() : prompt;
        String fullPrompt = buildPromptWithContext(processedPrompt, context);
        
        // 使用大模型生成回复
        Message userMessage = new UserMessage(fullPrompt);
        Prompt requestPrompt = new Prompt(List.of(userMessage));
        ChatResponse response = chatClient.call(requestPrompt);
        
        // 获取原始响应
        String rawResponse = response.getResult().getOutput().getContent();
        securityContext.setRawResponse(rawResponse);
        
        // 执行输出安全检查
        if (!securityFilterChainManager.executeFilterChain(securityContext)) {
            throw new SecurityException("输出安全检查未通过: " + securityContext.getErrorMessage());
        }
        
        // 返回经过处理的安全响应
        return securityContext.getProcessedResponse() != null ? 
               securityContext.getProcessedResponse() : rawResponse;
    }
    
    /**
     * 使用大模型生成回复，支持角色设定、用户信息和上下文
     * @param userPrompt 用户输入的提示词
     * @param context 对话上下文（历史消息）
     * @param characterPrompt 角色设定提示词
     * @param username 用户名
     * @param isFirstMessage 是否是首次对话
     * @return 大模型生成的回复内容
     * @throws SecurityException 如果安全检查未通过
     */
    public String generateResponseWithContext(String userPrompt, String context, 
                                             String characterPrompt, String username, 
                                             boolean isFirstMessage) {
        // 创建安全上下文
        SecurityContext securityContext = securityFilterChainManager.createContext(userPrompt, context);
        
        // 执行输入安全检查
        if (!securityFilterChainManager.executeFilterChain(securityContext)) {
            throw new SecurityException("输入安全检查未通过: " + securityContext.getErrorMessage());
        }
        
        // 构建包含角色设定和用户信息的完整提示词
        String processedPrompt = securityContext.getProcessedPrompt() != null ? 
                                 securityContext.getProcessedPrompt() : userPrompt;
        
        String fullPrompt;
        if (isFirstMessage) {
            // 首次对话，需要发送角色设定和用户信息
            fullPrompt = buildPromptWithCharacterAndUserInfo(
                    processedPrompt, 
                    context, 
                    characterPrompt, 
                    username
            );
        } else {
            // 非首次对话，只发送用户输入和上下文
            fullPrompt = buildPromptWithContext(processedPrompt, context);
        }
        
        // 使用大模型生成回复
        Message userMessage = new UserMessage(fullPrompt);
        Prompt requestPrompt = new Prompt(List.of(userMessage));
        ChatResponse response = chatClient.call(requestPrompt);
        
        // 获取原始响应
        String rawResponse = response.getResult().getOutput().getContent();
        securityContext.setRawResponse(rawResponse);
        
        // 执行输出安全检查
        if (!securityFilterChainManager.executeFilterChain(securityContext)) {
            throw new SecurityException("输出安全检查未通过: " + securityContext.getErrorMessage());
        }
        
        // 返回经过处理的安全响应
        return securityContext.getProcessedResponse() != null ? 
               securityContext.getProcessedResponse() : rawResponse;
    }

    /**
     * 构建包含上下文的完整提示词
     * @param userPrompt 用户输入的提示词
     * @param context 对话上下文
     * @return 构建好的完整提示词
     */
    private String buildPromptWithContext(String userPrompt, String context) {
        StringBuilder fullPromptBuilder = new StringBuilder();
        
        // 添加系统提示（角色设定）
        fullPromptBuilder.append("你是一个AI助手，专门为游戏玩家提供角色扮演服务。")
                        .append("请根据用户提供的信息，生成符合角色设定的回应。\n\n");
        
        // 添加对话上下文（如果有）
        if (context != null && !context.isEmpty()) {
            fullPromptBuilder.append("对话历史：\n")
                            .append(context)
                            .append("\n\n");
        }
        
        // 添加用户当前输入
        fullPromptBuilder.append("用户当前输入：\n")
                        .append(userPrompt);
        
        return fullPromptBuilder.toString();
    }
    
    /**
     * 构建包含角色设定和用户信息的完整提示词
     * @param userPrompt 用户输入的提示词
     * @param context 对话上下文
     * @param characterPrompt 角色设定提示词
     * @param username 用户名
     * @return 构建好的完整提示词
     */
    private String buildPromptWithCharacterAndUserInfo(String userPrompt, String context, 
                                                     String characterPrompt, String username) {
        StringBuilder fullPromptBuilder = new StringBuilder();
        
        // 添加系统提示
        fullPromptBuilder.append("你现在需要完全代入以下角色设定，与用户进行互动：\n\n");
        
        // 添加角色设定
        fullPromptBuilder.append("角色设定：\n")
                        .append(characterPrompt)
                        .append("\n\n");
        
        // 添加用户信息
        fullPromptBuilder.append("与你对话的用户信息：\n")
                        .append("用户名：").append(username)
                        .append("\n\n");
        
        // 添加对话上下文（如果有）
        if (context != null && !context.isEmpty()) {
            fullPromptBuilder.append("对话历史：\n")
                            .append(context)
                            .append("\n\n");
        }
        
        // 添加用户当前输入
        fullPromptBuilder.append("用户当前输入：\n")
                        .append(userPrompt);
        
        return fullPromptBuilder.toString();
    }
}