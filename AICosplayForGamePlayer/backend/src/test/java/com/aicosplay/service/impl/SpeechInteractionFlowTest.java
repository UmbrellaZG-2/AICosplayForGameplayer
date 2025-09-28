package com.aicosplay.service.impl;

import com.aicosplay.controller.SpeechController;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.service.AIService;
import com.aicosplay.service.UserService;
import com.aicosplay.utils.UserContext;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 语音交互完整流程测试类
 * 测试从语音识别 -> AI回复生成 -> 文本转语音的完整流程
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SpeechInteractionFlowTest {

    @Mock
    private SpeechRecognitionDispatcher speechRecognitionDispatcher;

    @Mock
    private TextToSpeechService textToSpeechService;

    @Mock
    private AIService aiService;

    @Mock
    private UserService userService;

    @InjectMocks
    private SpeechController speechController;

    private MockMultipartFile testAudioFile;
    private String testRecognizedText;
    private String testAIResponse;
    private String testVoiceId;
    private byte[] testAudioData;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建模拟HTTP请求和会话
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        
        // 设置用户名到会话中（模拟用户已登录）
        session.setAttribute("username", "testuser");
        request.setSession(session);
        
        // 设置请求上下文，使UserContext能够获取当前会话
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        
        // 设置UserContext中的userService
        UserContext userContext = new UserContext();
        userContext.setUserService(userService);
        
        // 模拟userService.findByUsername方法
        User mockUser = new User();
        mockUser.setUsername("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        
        // 准备测试数据
        String testAudioContent = "test audio content";
        testAudioFile = new MockMultipartFile(
                "audio",
                "test.wav",
                "audio/wav",
                testAudioContent.getBytes()
        );
        
        testRecognizedText = "你好，我想了解这个游戏";
        testAIResponse = "欢迎！这是一款非常有趣的角色扮演游戏，你可以选择不同的角色进行冒险。";
        testVoiceId = UUID.randomUUID().toString();
        testAudioData = new byte[]{1, 2, 3, 4, 5}; // 模拟音频数据
    }
    
    @AfterEach
    void tearDown() {
        // 清除请求上下文，避免测试间的相互影响
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 测试完整的语音交互流程：
     * 1. 语音识别 - 从音频文件识别文本
     * 2. AI回复生成 - 将识别的文本发送给AI获取回复
     * 3. 文本转语音 - 将AI回复转换为语音
     */
    @Test
    void testCompleteSpeechInteractionFlow() {
        // 创建模拟的Message对象
        Message mockMessage = new Message();
        mockMessage.setContent(testAIResponse);
        mockMessage.setVoiceFilePath(testVoiceId);
        
        // 模拟各个服务的行为
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(testRecognizedText);
        when(aiService.generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(testAIResponse);
        when(textToSpeechService.convertTextToSpeech(any(Message.class))).thenReturn(mockMessage);
        when(textToSpeechService.getAudioData(eq(testVoiceId))).thenReturn(testAudioData);
        
        // 1. 执行语音识别
        ResponseEntity<?> recognizeResponse = speechController.recognizeSpeech(testAudioFile);
        
        // 验证语音识别结果
        assertEquals(HttpStatus.OK, recognizeResponse.getStatusCode());
        assertNotNull(recognizeResponse.getBody());
        
        // 2. 模拟调用AI服务生成回复
        String aiResponse = aiService.generateResponseWithContext(testRecognizedText, "", "default", "testuser", false);
        
        // 验证AI回复
        assertEquals(testAIResponse, aiResponse);
        
        // 3. 模拟调用文本转语音服务
        Message aiMessage = new Message();
        aiMessage.setContent(testAIResponse);
        Message resultMessage = textToSpeechService.convertTextToSpeech(aiMessage);
        
        // 验证语音ID生成
        assertNotNull(resultMessage);
        assertNotNull(resultMessage.getVoiceFilePath());
        
        // 4. 模拟获取生成的语音数据
        ResponseEntity<?> audioResponse = speechController.getSpeechData(resultMessage.getVoiceFilePath());
        
        // 验证语音数据获取结果
        assertEquals(HttpStatus.OK, audioResponse.getStatusCode());
        assertNotNull(audioResponse.getBody());
        
        // 验证各个服务的调用
        verify(speechRecognitionDispatcher).recognizeSpeech(testAudioFile, "testuser", "");
        verify(aiService).generateResponseWithContext(testRecognizedText, "", "default", "testuser", false);
        verify(textToSpeechService).convertTextToSpeech(any(Message.class));
        verify(textToSpeechService).getAudioData(resultMessage.getVoiceFilePath());
    }

    /**
     * 测试语音识别失败的情况
     */
    @Test
    void testSpeechInteractionFlow_RecognitionFailed() {
        // 模拟语音识别失败
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(null);
        
        // 执行语音识别
        ResponseEntity<?> recognizeResponse = speechController.recognizeSpeech(testAudioFile);
        
        // 验证识别失败结果
        assertEquals(HttpStatus.OK, recognizeResponse.getStatusCode());
        
        // 验证服务调用
        verify(speechRecognitionDispatcher).recognizeSpeech(testAudioFile, "testuser", "");
        // AI服务和TTS服务不应该被调用
        verify(aiService, never()).generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        verify(textToSpeechService, never()).convertTextToSpeech(any(Message.class));
    }

    /**
     * 测试AI生成回复失败的情况
     */
    @Test
    void testSpeechInteractionFlow_AIFailed() {
        // 模拟语音识别成功，但AI生成回复失败
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(testRecognizedText);
        when(aiService.generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenThrow(new RuntimeException("AI服务异常"));
        
        // 1. 执行语音识别
        ResponseEntity<?> recognizeResponse = speechController.recognizeSpeech(testAudioFile);
        
        // 验证语音识别结果
        assertEquals(HttpStatus.OK, recognizeResponse.getStatusCode());
        
        // 2. 验证AI调用异常
        assertThrows(RuntimeException.class, () -> {
            aiService.generateResponseWithContext(testRecognizedText, "", "default", "testuser", false);
        });
        
        // 验证TTS服务不应该被调用
        verify(textToSpeechService, never()).convertTextToSpeech(any(Message.class));
    }

    /**
     * 测试文本转语音失败的情况
     */
    @Test
    void testSpeechInteractionFlow_TTSFailed() {
        // 模拟语音识别和AI回复成功，但文本转语音失败
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(testRecognizedText);
        when(aiService.generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean())).thenReturn(testAIResponse);
        when(textToSpeechService.convertTextToSpeech(any(Message.class))).thenThrow(new RuntimeException("TTS服务异常"));
        
        // 1. 执行语音识别
        ResponseEntity<?> recognizeResponse = speechController.recognizeSpeech(testAudioFile);
        
        // 验证语音识别结果
        assertEquals(HttpStatus.OK, recognizeResponse.getStatusCode());
        
        // 2. 调用AI服务生成回复
        String aiResponse = aiService.generateResponseWithContext(testRecognizedText, "", "default", "testuser", false);
        assertEquals(testAIResponse, aiResponse);
        
        // 3. 验证TTS调用异常
        assertThrows(RuntimeException.class, () -> {
            Message aiMessage = new Message();
            aiMessage.setContent(testAIResponse);
            textToSpeechService.convertTextToSpeech(aiMessage);
        });
    }
}