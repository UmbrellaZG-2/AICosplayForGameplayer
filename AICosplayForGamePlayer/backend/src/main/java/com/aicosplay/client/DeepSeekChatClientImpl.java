package com.aicosplay.client;

import com.google.gson.Gson;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 自定义DeepSeek AI客户端实现
 * 根据DeepSeek官方API文档实现的聊天客户端
 * 参考文档: https://api-docs.deepseek.com/zh-cn/
 */
@Component
public class DeepSeekChatClientImpl implements ChatClient {

    private static final Logger logger = LoggerFactory.getLogger(DeepSeekChatClientImpl.class);
    
    private final OkHttpClient okHttpClient;
    private final Gson gson;
    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final String baseUrl = "https://api.deepseek.com/chat/completions";

    /**
     * 构造函数，通过Spring配置注入DeepSeek API的必要参数
     * 参数值从application.properties文件中的环境变量读取
     */
    @Autowired
    public DeepSeekChatClientImpl(
            @Autowired String apiKey,
            @Autowired String model,
            @Autowired Double temperature) {
        // 验证必要参数
        Objects.requireNonNull(apiKey, "DeepSeek API key cannot be null. Please set SPRING_AI_DEEPSEEK_API_KEY environment variable.");
        Objects.requireNonNull(model, "DeepSeek model name cannot be null. Please set SPRING_AI_DEEPSEEK_CHAT_MODEL environment variable.");
        
        this.apiKey = apiKey;
        this.model = model;
        // 如果未提供温度参数，使用默认值0.7
        this.temperature = temperature != null ? temperature : 0.7;
        
        logger.info("Initializing DeepSeekChatClientImpl with model: {}", model);
        
        // 初始化OkHttpClient，设置合适的超时和重试策略
        this.okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
        
        // 初始化Gson，用于JSON序列化和反序列化
        this.gson = new Gson();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            logger.debug("调用DeepSeek API，模型: {}", model);
            
            // 验证输入参数
            Objects.requireNonNull(prompt, "Prompt cannot be null");
            if (prompt.getContents() == null || prompt.getContents().isEmpty()) {
                throw new IllegalArgumentException("Prompt contents cannot be empty");
            }
            
            // 构建请求体
            DeepSeekRequest requestBody = buildRequestBody(prompt);
            String jsonBody = gson.toJson(requestBody);
            logger.trace("DeepSeek API请求体: {}", jsonBody);

            // 创建HTTP请求
            Request request = new Request.Builder()
                    .url(baseUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8")))
                    .build();

            // 执行请求
            try (Response response = okHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    logger.error("DeepSeek API请求失败: {}, 错误详情: {}", response.code(), errorBody);
                    throw new IOException("DeepSeek API请求失败，状态码: " + response.code() + ", 错误详情: " + errorBody);
                }

                // 解析响应
                String responseBody = response.body().string();
                logger.debug("DeepSeek API响应: {}", responseBody);
                
                DeepSeekResponse deepSeekResponse = gson.fromJson(responseBody, DeepSeekResponse.class);
                
                // 记录使用情况统计
                if (deepSeekResponse.getUsage() != null) {
                    logger.debug("DeepSeek API使用情况: 提示词tokens: {}, 完成tokens: {}, 总计tokens: {}", 
                            deepSeekResponse.getUsage().getPrompt_tokens() != null ? deepSeekResponse.getUsage().getPrompt_tokens() : "N/A",
                            deepSeekResponse.getUsage().getCompletion_tokens() != null ? deepSeekResponse.getUsage().getCompletion_tokens() : "N/A",
                            deepSeekResponse.getUsage().getTotal_tokens() != null ? deepSeekResponse.getUsage().getTotal_tokens() : "N/A");
                }
                
                // 转换为Spring AI的ChatResponse
                return convertToChatResponse(deepSeekResponse);
            }
        } catch (IOException e) {
            logger.error("DeepSeek API通信错误: {}", e.getMessage(), e);
            throw new RuntimeException("与DeepSeek API通信时发生错误", e);
        } catch (Exception e) {
            logger.error("调用DeepSeek API时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("调用DeepSeek API时发生错误", e);
        }
    }
    
    // 实现ChatClient接口中的call(String)方法
    @Override
    public String call(String message) {
        // 将简单字符串消息转换为Prompt对象
        UserMessage userMessage = new UserMessage(message);
        Prompt prompt = new Prompt(List.of(userMessage));
        
        // 调用call(Prompt)方法获取ChatResponse
        ChatResponse response = call(prompt);
        
        try {
            // 在Spring AI 0.8.0版本中，使用getResult().getOutput().getContent()获取生成内容
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            logger.error("从ChatResponse中提取内容时出错: {}", e.getMessage(), e);
            return "";
        }
    }

    // 由于Spring AI 0.8.0版本不支持这些方法的覆盖，我们不添加@Override注解
    public ChatClient withPromptTemplate(PromptTemplate promptTemplate) {
        // 由于当前实现不支持模板，我们可以记录日志并返回当前实例
        logger.info("withPromptTemplate方法被调用，但当前实现不支持模板功能");
        return this;
    }

    /**
     * 构建DeepSeek API请求体
     * 根据DeepSeek API文档构建请求对象，包含所有必要的参数
     */
    private DeepSeekRequest buildRequestBody(Prompt prompt) {
        Objects.requireNonNull(prompt, "Prompt cannot be null");
        
        DeepSeekRequest request = new DeepSeekRequest();
        
        // 必需参数
        request.setModel(model);
        
        // 处理prompt内容，确保转换为List<Message>
        Object contents = prompt.getContents();
        List<Message> messages;
        if (contents instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<Message> castMessages = (List<Message>) contents;
            messages = castMessages;
        } else {
            // 如果不是List，则创建一个包含单个UserMessage的列表
            logger.warn("Prompt contents is not a List, creating single message");
            messages = List.of(new UserMessage(String.valueOf(contents)));
        }
        
        request.setMessages(convertToDeepSeekMessages(messages));
        
        // 设置温度参数，控制生成内容的随机性
        // 0表示确定性输出，1表示最大随机性
        request.setTemperature(temperature);
        
        // 设置为非流式响应
        request.setStream(false);
        
        // 添加其他可选参数（根据DeepSeek API文档）
        // request.setTopP(0.9); // 控制多样性，与temperature配合使用
        // request.setMaxTokens(4096); // 最大生成长度
        // request.setPresencePenalty(0.0); // 控制新主题出现的可能性
        // request.setFrequencyPenalty(0.0); // 控制重复内容的频率
        
        logger.debug("构建DeepSeek API请求体: 模型={}, 温度={}", model, temperature);
        
        return request;
    }

    /**
     * 将Spring AI消息转换为DeepSeek消息格式
     * 参考DeepSeek API文档: https://api-docs.deepseek.com/zh-cn/api/chat-completion
     */
    private List<DeepSeekMessage> convertToDeepSeekMessages(List<Message> messages) {
        Objects.requireNonNull(messages, "Messages list cannot be null");
        
        logger.debug("转换 {} 条消息到DeepSeek格式", messages.size());
        
        return messages.stream()
                .filter(Objects::nonNull) // 过滤掉空消息
                .map(message -> {
                    DeepSeekMessage deepSeekMessage = new DeepSeekMessage();
                    
                    // 根据消息类型设置角色
            String role;
            if (message instanceof SystemMessage) {
                role = "system";
            } else if (message instanceof UserMessage) {
                role = "user";
            } else if (message instanceof AssistantMessage) {
                role = "assistant";
            } else {
                logger.warn("未知消息类型: {}, 默认为user角色", message.getClass().getSimpleName());
                role = "user"; // 默认使用user角色
            }
                    
                    // 验证消息内容
                    String content = message.getContent();
                    if (content == null || content.trim().isEmpty()) {
                        logger.warn("消息内容为空，使用默认占位符");
                        content = "[Empty message]";
                    }
                    
                    deepSeekMessage.setRole(role);
                    deepSeekMessage.setContent(content);
                    
                    logger.trace("转换消息: 角色={}, 内容长度={}", role, content.length());
                    
                    return deepSeekMessage;
                })
                .toList();
    }

    /**
     * 将DeepSeek API响应转换为Spring AI的ChatResponse
     * 根据DeepSeek API文档格式进行解析和转换
     */
    private ChatResponse convertToChatResponse(DeepSeekResponse response) {
        Objects.requireNonNull(response, "DeepSeek API响应不能为空");
        
        logger.debug("开始转换DeepSeek API响应，响应ID: {}, 模型: {}", response.getId(), response.getModel());

        List<Generation> generations = new ArrayList<>();
        
        // 检查并处理choices数组
        if (response.getChoices() == null || response.getChoices().isEmpty()) {
            logger.warn("DeepSeek API返回空choices列表");
            return new ChatResponse(generations);
        }

        // 处理每个生成结果
        for (DeepSeekResponse.Choice choice : response.getChoices()) {
            if (choice == null) {
                logger.warn("发现空的choice对象");
                continue;
            }

            if (choice.getMessage() == null) {
                logger.warn("Choice中消息为空，索引: {}", choice.getIndex());
                continue;
            }

            String content = choice.getMessage().getContent();
            if (content == null || content.trim().isEmpty()) {
                logger.warn("DeepSeek API返回空消息内容，索引: {}", choice.getIndex());
                // 即使内容为空，也添加一个空的生成结果，以便调用方知道有一个结果
                content = "";
            }

            // 创建AssistantMessage，包含消息内容和元数据
            Map<String, Object> messageMetadata = new HashMap<>();
            messageMetadata.put("finish_reason", choice.getFinish_reason());
            messageMetadata.put("index", choice.getIndex());

            AssistantMessage assistantMessage = new AssistantMessage(
                    content,
                    messageMetadata
            );

            // 创建Generation对象
            Generation generation = new Generation(assistantMessage.getContent());
            generations.add(generation);

            logger.debug("成功创建生成结果，索引: {}, 完成原因: {}", 
                    choice.getIndex(), choice.getFinish_reason());
        }

        // 收集整体响应的元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", response.getId());
        metadata.put("object", response.getObject());
        metadata.put("model", response.getModel());
        metadata.put("created", response.getCreated());
        
        // 添加使用统计信息
        if (response.getUsage() != null) {
            metadata.put("usage", response.getUsage());
            // 单独提取使用统计中的关键信息，便于直接访问
            if (response.getUsage().getPrompt_tokens() != null) {
                metadata.put("prompt_tokens", response.getUsage().getPrompt_tokens());
            }
            if (response.getUsage().getCompletion_tokens() != null) {
                metadata.put("completion_tokens", response.getUsage().getCompletion_tokens());
            }
            if (response.getUsage().getTotal_tokens() != null) {
                metadata.put("total_tokens", response.getUsage().getTotal_tokens());
            }
        }

        // 创建并返回ChatResponse
        ChatResponse chatResponse = new ChatResponse(generations);
        
        logger.debug("DeepSeek API响应转换完成，生成结果数量: {}, 总token数: {}", 
                generations.size(), 
                response.getUsage() != null ? response.getUsage().getTotal_tokens() : "未知");
        
        return chatResponse;
    }

    /**
     * DeepSeek API请求体模型
     * 根据DeepSeek API文档定义请求参数结构
     * 参考: https://api-docs.deepseek.com/zh-cn/api/chat-completion
     */
    public static class DeepSeekRequest {
        // 必需参数
        private String model; // 模型名称，如 deepseek-chat
        private List<DeepSeekMessage> messages; // 消息列表
        
        // 可选参数
        private Double temperature; // 控制生成的随机性，范围0-1
        private Double topP; // 控制多样性，与temperature配合使用，范围0-1
        private Integer maxTokens; // 最大生成长度
        private Double presencePenalty; // 控制新主题出现的可能性，范围-2-2
        private Double frequencyPenalty; // 控制重复内容的频率，范围-2-2
        private Boolean stream; // 是否使用流式响应

        // getter和setter
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<DeepSeekMessage> getMessages() { return messages; }
        public void setMessages(List<DeepSeekMessage> messages) { this.messages = messages; }
        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Double getTopP() { return topP; }
        public void setTopP(Double topP) { this.topP = topP; }
        public Integer getMaxTokens() { return maxTokens; }
        public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
        public Double getPresencePenalty() { return presencePenalty; }
        public void setPresencePenalty(Double presencePenalty) { this.presencePenalty = presencePenalty; }
        public Double getFrequencyPenalty() { return frequencyPenalty; }
        public void setFrequencyPenalty(Double frequencyPenalty) { this.frequencyPenalty = frequencyPenalty; }
        public Boolean getStream() { return stream; }
        public void setStream(Boolean stream) { this.stream = stream; }
    }

    /**
     * DeepSeek消息模型
     * 定义API请求中的消息格式
     */
    public static class DeepSeekMessage {
        private String role; // 角色，如 system, user, assistant
        private String content; // 消息内容

        // getter和setter
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    /**
     * DeepSeek API响应模型
     * 定义API返回的响应格式
     */
    public static class DeepSeekResponse {
        private String id; // 响应ID
        private String object; // 对象类型
        private Long created; // 创建时间戳
        private String model; // 使用的模型
        private List<Choice> choices; // 生成的结果列表
        private Usage usage; // token使用统计

        // getter和setter
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getObject() { return object; }
        public void setObject(String object) { this.object = object; }
        public Long getCreated() { return created; }
        public void setCreated(Long created) { this.created = created; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public List<Choice> getChoices() { return choices; }
        public void setChoices(List<Choice> choices) { this.choices = choices; }
        public Usage getUsage() { return usage; }
        public void setUsage(Usage usage) { this.usage = usage; }

        /**
         * 生成结果选项
         */
        public static class Choice {
            private DeepSeekMessage message; // 生成的消息
            private String finish_reason; // 完成原因
            private Integer index; // 索引

            // getter和setter
            public DeepSeekMessage getMessage() { return message; }
            public void setMessage(DeepSeekMessage message) { this.message = message; }
            public String getFinish_reason() { return finish_reason; }
            public void setFinish_reason(String finish_reason) { this.finish_reason = finish_reason; }
            public Integer getIndex() { return index; }
            public void setIndex(Integer index) { this.index = index; }
        }

        /**
         * Token使用统计
         */
        public static class Usage {
            private Integer prompt_tokens; // 输入提示使用的token数
            private Integer completion_tokens; // 生成响应使用的token数
            private Integer total_tokens; // 总共使用的token数

            // getter和setter
            public Integer getPrompt_tokens() { return prompt_tokens; }
            public void setPrompt_tokens(Integer prompt_tokens) { this.prompt_tokens = prompt_tokens; }
            public Integer getCompletion_tokens() { return completion_tokens; }
            public void setCompletion_tokens(Integer completion_tokens) { this.completion_tokens = completion_tokens; }
            public Integer getTotal_tokens() { return total_tokens; }
            public void setTotal_tokens(Integer total_tokens) { this.total_tokens = total_tokens; }
        }
    }


}