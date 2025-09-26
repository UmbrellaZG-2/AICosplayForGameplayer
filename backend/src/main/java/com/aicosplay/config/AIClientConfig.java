package com.aicosplay.config;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Spring AI配置类，用于配置AI聊天客户端和相关组件
 */
@Configuration
public class AIClientConfig {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.model:llama3}")
    private String defaultModel;

    /**
     * 创建Ollama API客户端
     */
    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi(ollamaBaseUrl);
    }

    /**
     * 创建AI聊天客户端
     */
    @Bean
    public ChatClient chatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }



    /**
     * 创建系统提示模板，用于构建标准化的系统提示
     */
    @Bean
    public PromptTemplate systemPromptTemplate() {
        String systemPromptTemplate = """
        你是一个AI助手，专门为游戏玩家提供角色扮演服务。
        请根据用户提供的角色设定、对话历史和当前输入，生成符合角色性格和身份的回应。
        回应应该自然流畅，符合角色的说话风格和背景设定。
        """;
        return new PromptTemplate(systemPromptTemplate);
    }
}