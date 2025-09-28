package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechRecognitionService.RecognitionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Disabled("在线语音识别服务在测试环境中禁用")
class SpeechServiceImplTest {

    private SpeechServiceImpl speechServiceImpl;

    @Mock
    private RestTemplate restTemplate;

    @Value("${iflytek.appid}")
    private String appId;

    @Value("${iflytek.apiKey}")
    private String apiKey;

    @Value("${iflytek.apiSecret}")
    private String apiSecret;

    @Value("${iflytek.api.url}")
    private String apiUrl;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        speechServiceImpl = new SpeechServiceImpl();
        
        // 使用反射设置属性
        try {
            // 设置RestTemplate
            speechServiceImpl.getClass().getDeclaredField("restTemplate").setAccessible(true);
            speechServiceImpl.getClass().getDeclaredField("restTemplate").set(speechServiceImpl, restTemplate);
            
            // 设置配置参数
            speechServiceImpl.getClass().getDeclaredField("appId").setAccessible(true);
            speechServiceImpl.getClass().getDeclaredField("appId").set(speechServiceImpl, appId != null ? appId : "test-appid");
            
            speechServiceImpl.getClass().getDeclaredField("apiKey").setAccessible(true);
            speechServiceImpl.getClass().getDeclaredField("apiKey").set(speechServiceImpl, apiKey != null ? apiKey : "test-apikey");
            
            speechServiceImpl.getClass().getDeclaredField("apiSecret").setAccessible(true);
            speechServiceImpl.getClass().getDeclaredField("apiSecret").set(speechServiceImpl, apiSecret != null ? apiSecret : "test-apisecret");
            
            speechServiceImpl.getClass().getDeclaredField("apiUrl").setAccessible(true);
            speechServiceImpl.getClass().getDeclaredField("apiUrl").set(speechServiceImpl, apiUrl != null ? apiUrl : "https://api.xfyun.cn/v1/private/sdk/sf888c293?host=vtc.xfyun.cn");
            
            // 设置默认值，以防注入失败
            if (speechServiceImpl.getClass().getDeclaredField("appId").get(speechServiceImpl) == null) {
                speechServiceImpl.getClass().getDeclaredField("appId").set(speechServiceImpl, "test-appid");
            }
            if (speechServiceImpl.getClass().getDeclaredField("apiKey").get(speechServiceImpl) == null) {
                speechServiceImpl.getClass().getDeclaredField("apiKey").set(speechServiceImpl, "test-apikey");
            }
            if (speechServiceImpl.getClass().getDeclaredField("apiSecret").get(speechServiceImpl) == null) {
                speechServiceImpl.getClass().getDeclaredField("apiSecret").set(speechServiceImpl, "test-apisecret");
            }
            if (speechServiceImpl.getClass().getDeclaredField("apiUrl").get(speechServiceImpl) == null) {
                speechServiceImpl.getClass().getDeclaredField("apiUrl").set(speechServiceImpl, "https://api.xfyun.cn/v1/private/sdk/sf888c293?host=vtc.xfyun.cn");
            }
        } catch (Exception e) {
            fail("Failed to set up test environment: " + e.getMessage());
        }
    }

    @Test
    void testRecognizeSpeech_Success() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟API响应
        String base64Audio = Base64.getEncoder().encodeToString(testAudioData);
        String expectedResponse = "{\"code\":0,\"message\":\"success\",\"data\":{\"result\":\"测试识别结果\"}}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(expectedResponse);
        
        // 模拟RestTemplate行为
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenReturn(responseEntity);
        
        // 执行测试
        String result = speechServiceImpl.recognizeSpeech(audioFile);
        
        // 验证结果
        assertEquals("测试识别结果", result);
        verify(restTemplate, times(1)).exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        );
    }

    @Test
    void testRecognizeSpeech_APIError() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟API错误响应
        String errorResponse = "{\"code\":1001,\"message\":\"API Error\",\"data\":null}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(errorResponse);
        
        // 模拟RestTemplate行为
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenReturn(responseEntity);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("在线语音识别失败"));
    }

    @Test
    void testRecognizeSpeech_EmptyResult() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟API返回空结果
        String emptyResultResponse = "{\"code\":0,\"message\":\"success\",\"data\":{\"result\":\"\"}}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(emptyResultResponse);
        
        // 模拟RestTemplate行为
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenReturn(responseEntity);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertEquals("未识别到有效文本", exception.getMessage());
    }

    @Test
    void testRecognizeSpeech_NullData() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟API返回null data
        String nullDataResponse = "{\"code\":0,\"message\":\"success\",\"data\":null}";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(nullDataResponse);
        
        // 模拟RestTemplate行为
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenReturn(responseEntity);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("在线语音识别结果解析失败"));
    }

    @Test
    void testRecognizeSpeech_InvalidJSON() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟API返回无效JSON
        String invalidJSONResponse = "invalid json response";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(invalidJSONResponse);
        
        // 模拟RestTemplate行为
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenReturn(responseEntity);
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息包含JSON解析错误
        assertTrue(exception.getMessage().contains("在线语音识别结果解析失败"));
    }

    @Test
    void testRecognizeSpeech_RestTemplateException() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 模拟RestTemplate异常
        when(restTemplate.exchange(
                anyString(), 
                eq(HttpMethod.POST), 
                any(HttpEntity.class), 
                eq(String.class)
        )).thenThrow(new RuntimeException("网络连接超时"));
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("在线语音识别失败"));
    }

    @Test
    void testHealthCheck() {
        // 执行测试
        String status = speechServiceImpl.healthCheck();
        
        // 验证结果
        assertTrue(status.contains("运行正常"));
        assertTrue(status.contains("在线"));
        assertTrue(status.contains("讯飞"));
    }

    @Test
    void testIsAvailable() {
        // 在线服务总是可用的（假设网络正常）
        assertTrue(speechServiceImpl.isAvailable());
    }

    @Test
    void testGetServiceType() {
        // 执行测试
        String serviceType = speechServiceImpl.getServiceType();
        
        // 验证结果
        assertEquals("online", serviceType);
    }

    @Test
    void testGetRecognitionType() {
        // 执行测试
        RecognitionType recognitionType = speechServiceImpl.getRecognitionType();
        
        // 验证结果
        assertEquals(RecognitionType.API, recognitionType);
    }

    // 注意：这些测试方法在当前实现中不存在，已被注释掉
    /*
    @Test
    void testGenerateAuthToken() {
        // 这里我们只能测试token不为空，因为具体的token生成逻辑涉及时间戳和加密
        String token = speechServiceImpl.generateAuthToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testCreateRequestHeaders() {
        // 创建请求头
        HttpHeaders headers = speechServiceImpl.createRequestHeaders();
        
        // 验证请求头包含必要信息
        assertNotNull(headers);
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.containsKey("Content-Type"));
        assertEquals("application/json", headers.getFirst("Content-Type"));
    }

    @Test
    void testBuildRequestBody() throws IOException {
        // 准备测试数据
        byte[] testAudioData = {1, 2, 3, 4, 5}; // 模拟音频数据
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioData
        );
        
        // 构建请求体
        Map<String, Object> requestBody = speechServiceImpl.buildRequestBody(audioFile);
        
        // 验证请求体包含必要信息
        assertNotNull(requestBody);
        assertTrue(requestBody.containsKey("common"));
        assertTrue(requestBody.containsKey("business"));
        assertTrue(requestBody.containsKey("data"));
        
        // 验证common部分
        Map<String, Object> common = (Map<String, Object>) requestBody.get("common");
        assertNotNull(common);
        assertTrue(common.containsKey("app_id"));
        
        // 验证data部分
        Map<String, Object> data = (Map<String, Object>) requestBody.get("data");
        assertNotNull(data);
        assertTrue(data.containsKey("audio"));
        
        // 验证audio数据被正确编码
        String base64Audio = (String) data.get("audio");
        assertNotNull(base64Audio);
        assertFalse(base64Audio.isEmpty());
    }

    @Test
    void testProcessRecognitionResult_Success() {
        // 模拟成功响应
        String successResponse = "{\"code\":0,\"message\":\"success\",\"data\":{\"result\":\"处理结果测试\"}}";
        
        // 处理结果
        String result = speechServiceImpl.processRecognitionResult(successResponse);
        
        // 验证结果
        assertEquals("处理结果测试", result);
    }

    @Test
    void testProcessRecognitionResult_Failure() {
        // 模拟失败响应
        String failureResponse = "{\"code\":1001,\"message\":\"API Error\",\"data\":null}";
        
        // 处理结果并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            speechServiceImpl.processRecognitionResult(failureResponse);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("API Error"));
    }
    */
}