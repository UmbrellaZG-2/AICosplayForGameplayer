package com.aicosplay.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final ChatClient chatClient;

    @Autowired
    public AIService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    /**
     * 使用大模型生成回复
     * @param prompt 用户输入的提示词
     * @return 大模型生成的回复内容
     */
    public String generateResponse(String prompt) {
        return generateResponseInternal(prompt, null, null, null, false);
    }

    /**
     * 使用大模型生成回复，支持上下文
     * @param prompt 用户输入的提示词
     * @param context 对话上下文（历史消息）
     * @return 大模型生成的回复内容
     */
    public String generateResponseWithContext(String prompt, String context) {
        return generateResponseInternal(prompt, context, null, null, false);
    }
    
    /**
     * 使用大模型生成回复，支持角色设定、用户信息和上下文
     * @param userPrompt 用户输入的提示词
     * @param context 对话上下文（历史消息）
     * @param characterPrompt 角色设定提示词
     * @param username 用户名
     * @param isFirstMessage 是否是首次对话
     * @return 大模型生成的回复内容
     */
    public String generateResponseWithContext(String userPrompt, String context, 
                                             String characterPrompt, String username, 
                                             boolean isFirstMessage) {
        return generateResponseInternal(userPrompt, context, characterPrompt, username, isFirstMessage);
    }
    
    /**
     * 核心方法：使用大模型生成回复，支持各种参数组合
     * @param prompt 用户输入的提示词
     * @param context 对话上下文
     * @param characterPrompt 角色设定提示词
     * @param username 用户名
     * @param isFirstMessage 是否首次对话
     * @return 大模型生成的回复内容
     */
    private String generateResponseInternal(String prompt, String context, 
                                           String characterPrompt, String username, 
                                           boolean isFirstMessage) {
        // 记录请求信息
        logger.debug("Generating AI response with context: {}", context != null);
        
        // 构建完整提示词
        String fullPrompt;
        if (isFirstMessage && characterPrompt != null && username != null) {
            // 首次对话，需要发送角色设定和用户信息
            fullPrompt = buildPromptWithCharacterAndUserInfo(
                    prompt, 
                    context, 
                    characterPrompt, 
                    username
            );
        } else if (context != null) {
            // 有上下文，发送用户输入和上下文
            fullPrompt = buildPromptWithContext(prompt, context);
        } else {
            // 无上下文，仅发送用户输入
            fullPrompt = prompt;
        }
        
        // 使用大模型生成回复
        Message userMessage = new UserMessage(fullPrompt);
        Prompt requestPrompt = new Prompt(List.of(userMessage));
        ChatResponse response = chatClient.call(requestPrompt);
        
        // 获取响应
        String finalResponse = response.getResult().getOutput().getContent();
        
        logger.debug("AI response generation completed successfully");
        return finalResponse;
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