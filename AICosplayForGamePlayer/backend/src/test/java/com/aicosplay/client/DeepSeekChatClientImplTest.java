package com.aicosplay.client;

import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeepSeekChatClientImplTest {

    @Mock
    private Call mockCall;
    
    @Mock
    private Response mockResponse;
    
    @Mock
    private ResponseBody mockResponseBody;
    
    private String apiKey = "test-api-key";
    private String model = "deepseek-chat";
    private Double temperature = 0.7;
    
    private DeepSeekChatClientImpl deepSeekChatClient;
    private OkHttpClient mockOkHttpClient;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建模拟的OkHttpClient
        mockOkHttpClient = mock(OkHttpClient.class);
        when(mockOkHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
        
        // 由于DeepSeekChatClientImpl在构造函数中创建OkHttpClient和Gson，
        // 我们需要直接创建一个客户端实例用于测试
        deepSeekChatClient = new DeepSeekChatClientImpl(apiKey, model, temperature);
        
        // 使用反射替换OkHttpClient实例
        try {
            java.lang.reflect.Field field = DeepSeekChatClientImpl.class.getDeclaredField("okHttpClient");
            field.setAccessible(true);
            field.set(deepSeekChatClient, mockOkHttpClient);
        } catch (Exception e) {
            fail("Failed to setup test: " + e.getMessage());
        }
    }

    @Test
    void constructor_shouldInitializeClientWithValidParams() {
        // 验证构造函数能正常初始化客户端
        DeepSeekChatClientImpl client = new DeepSeekChatClientImpl(apiKey, model, temperature);
        assertNotNull(client);
    }

    @Test
    void constructor_shouldThrowExceptionWhenApiKeyIsNull() {
        // 验证当API密钥为空时，构造函数抛出异常
        assertThrows(NullPointerException.class, () -> {
            new DeepSeekChatClientImpl(null, model, temperature);
        });
    }

    @Test
    void constructor_shouldThrowExceptionWhenModelIsNull() {
        // 验证当模型名为空时，构造函数抛出异常
        assertThrows(NullPointerException.class, () -> {
            new DeepSeekChatClientImpl(apiKey, null, temperature);
        });
    }

    @Test
    void constructor_shouldUseDefaultTemperatureWhenNull() {
        // 验证当温度参数为空时，构造函数使用默认值
        DeepSeekChatClientImpl client = new DeepSeekChatClientImpl(apiKey, model, null);
        assertNotNull(client);
    }

    @Test
    void callPrompt_shouldReturnChatResponseWhenApiCallSucceeds() throws IOException {
        // 准备测试数据
        String userMessageContent = "Hello, AI!";
        UserMessage userMessage = new UserMessage(userMessageContent);
        Prompt prompt = new Prompt(List.of(userMessage));
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        
        // 准备模拟响应数据
        String mockResponseJson = "{\"id\":\"test-id\",\"object\":\"chat.completion\",\"created\":1677649427,\"model\":\"deepseek-chat\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello! How can I help you today?\"},\"finish_reason\":\"stop\",\"index\":0}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":15,\"total_tokens\":25}}";
        when(mockResponseBody.string()).thenReturn(mockResponseJson);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        
        // 执行测试
        ChatResponse chatResponse = deepSeekChatClient.call(prompt);
        
        // 验证结果
        assertNotNull(chatResponse);
        assertFalse(chatResponse.getResults().isEmpty());
        assertEquals("Hello! How can I help you today?", chatResponse.getResult().getOutput().getContent());
    }

    @Test
    void callPrompt_shouldHandleEmptyChoicesGracefully() throws IOException {
        // 准备测试数据
        UserMessage userMessage = new UserMessage("Hello, AI!");
        Prompt prompt = new Prompt(List.of(userMessage));
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        
        // 准备模拟响应数据（空choices列表）
        String mockResponseJson = "{\"id\":\"test-id\",\"object\":\"chat.completion\",\"created\":1677649427,\"model\":\"deepseek-chat\",\"choices\":[],\"usage\":{}}";
        when(mockResponseBody.string()).thenReturn(mockResponseJson);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        
        // 执行测试
        ChatResponse chatResponse = deepSeekChatClient.call(prompt);
        
        // 验证结果
        assertNotNull(chatResponse);
        assertTrue(chatResponse.getResults().isEmpty());
    }

    @Test
    void callPrompt_shouldThrowExceptionWhenApiCallFails() throws IOException {
        // 准备测试数据
        UserMessage userMessage = new UserMessage("Hello, AI!");
        Prompt prompt = new Prompt(List.of(userMessage));
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(false);
        when(mockResponse.code()).thenReturn(401);
        when(mockResponseBody.string()).thenReturn("Unauthorized");
        when(mockResponse.body()).thenReturn(mockResponseBody);
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            deepSeekChatClient.call(prompt);
        });
    }

    @Test
    void callPrompt_shouldThrowExceptionWhenIOExceptionOccurs() throws IOException {
        // 准备测试数据
        UserMessage userMessage = new UserMessage("Hello, AI!");
        Prompt prompt = new Prompt(List.of(userMessage));
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenThrow(new IOException("Connection failed"));
        
        // 执行测试并验证异常
        assertThrows(RuntimeException.class, () -> {
            deepSeekChatClient.call(prompt);
        });
    }

    @Test
    void callString_shouldReturnContentWhenApiCallSucceeds() throws IOException {
        // 准备测试数据
        String message = "Hello, AI!";
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        
        // 准备模拟响应数据
        String mockResponseJson = "{\"id\":\"test-id\",\"object\":\"chat.completion\",\"created\":1677649427,\"model\":\"deepseek-chat\",\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Hello! How can I help you today?\"},\"finish_reason\":\"stop\",\"index\":0}],\"usage\":{}}";
        when(mockResponseBody.string()).thenReturn(mockResponseJson);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        
        // 执行测试
        String result = deepSeekChatClient.call(message);
        
        // 验证结果
        assertEquals("Hello! How can I help you today?", result);
    }

    @Test
    void callString_shouldReturnEmptyStringWhenExtractionFails() throws IOException {
        // 准备测试数据
        String message = "Hello, AI!";
        
        // 模拟OkHttpClient行为
        when(mockCall.execute()).thenReturn(mockResponse);
        when(mockResponse.isSuccessful()).thenReturn(true);
        
        // 准备模拟响应数据
        String mockResponseJson = "{\"id\":\"test-id\",\"object\":\"chat.completion\",\"created\":1677649427,\"model\":\"deepseek-chat\",\"choices\":[],\"usage\":{}}";
        when(mockResponseBody.string()).thenReturn(mockResponseJson);
        when(mockResponse.body()).thenReturn(mockResponseBody);
        
        // 执行测试
        String result = deepSeekChatClient.call(message);
        
        // 验证结果
        assertEquals("", result);
    }

    @Test
    void withPromptTemplate_shouldReturnSameInstance() {
        // 测试withPromptTemplate方法返回当前实例
        PromptTemplate promptTemplate = mock(PromptTemplate.class);
        ChatClient result = deepSeekChatClient.withPromptTemplate(promptTemplate);
        assertSame(deepSeekChatClient, result);
    }
}