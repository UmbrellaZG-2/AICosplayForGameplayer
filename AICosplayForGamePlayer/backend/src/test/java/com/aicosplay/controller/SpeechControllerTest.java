package com.aicosplay.controller;

import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.impl.SpeechRecognitionDispatcher;
import com.aicosplay.service.impl.TextToSpeechService;
import com.aicosplay.utils.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpeechControllerTest {

    @Mock
    private SpeechRecognitionDispatcher speechRecognitionDispatcher;

    @Mock
    private TextToSpeechService textToSpeechService;

    @InjectMocks
    private SpeechController speechController;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模拟Spring Security上下文
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void testRecognizeSpeech_Success() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        String expectedText = "测试语音识别结果";
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(expectedText);
        
        // 执行测试
        ResponseEntity<ApiResponse<?>> response = speechController.recognizeSpeech(audioFile);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals(expectedText, response.getBody().getData());
        
        // 验证调用
        verify(speechRecognitionDispatcher).recognizeSpeech(audioFile, "testuser", "");
    }

    @Test
    void testRecognizeSpeech_EmptyFile() {
        // 准备测试数据 - 空文件
        MockMultipartFile emptyFile = new MockMultipartFile(
                "audio", 
                "empty.wav", 
                "audio/wav", 
                new byte[0]
        );
        
        // 执行测试并验证异常
        try {
            speechController.recognizeSpeech(emptyFile);
        } catch (Exception e) {
            assertEquals("FILE_EMPTY", ((com.aicosplay.exception.BusinessException)e).getErrorCode());
        }
    }

    @Test
    void testRecognizeSpeech_FileTooLarge() {
        // 准备测试数据 - 超过10MB的文件
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "audio", 
                "large.wav", 
                "audio/wav", 
                largeContent
        );
        
        // 执行测试并验证异常
        try {
            speechController.recognizeSpeech(largeFile);
        } catch (Exception e) {
            assertEquals("FILE_TOO_LARGE", ((com.aicosplay.exception.BusinessException)e).getErrorCode());
        }
    }

    @Test
    void testRecognizeSpeech_RecognitionFailed() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 模拟识别失败返回null
        when(speechRecognitionDispatcher.recognizeSpeech(any(), anyString(), anyString())).thenReturn(null);
        
        // 执行测试
        ResponseEntity<ApiResponse<?>> response = speechController.recognizeSpeech(audioFile);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1001, response.getBody().getCode()); // AUDIO_ERROR代码
        assertEquals("语音识别失败，请重试", response.getBody().getMessage());
    }

    @Test
    void testHealthCheck() {
        // 模拟健康检查响应
        String expectedStatus = "{\"online\":\"AVAILABLE\",\"offline\":\"AVAILABLE\"}";
        when(speechRecognitionDispatcher.getServicesStatus()).thenReturn(expectedStatus);
        
        // 执行测试
        ResponseEntity<ApiResponse<?>> response = speechController.healthCheck();
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals(expectedStatus, response.getBody().getData());
        
        // 验证调用
        verify(speechRecognitionDispatcher).getServicesStatus();
    }

    @Test
    void testSwitchServiceType() {
        // 执行测试
        ResponseEntity<ApiResponse<?>> response = speechController.switchServiceType("offline");
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(200, response.getBody().getCode());
        assertEquals("服务类型切换成功", response.getBody().getData());
        
        // 验证调用
        verify(speechRecognitionDispatcher).switchServiceType("offline");
    }

    @Test
    void testGetSpeechData() {
        // 准备测试数据
        String voiceId = "test-voice-id";
        byte[] expectedAudioData = "test audio data".getBytes();
        
        // 模拟获取音频数据
        when(textToSpeechService.getAudioData(voiceId)).thenReturn(expectedAudioData);
        
        // 执行测试
        ResponseEntity<byte[]> response = speechController.getSpeechData(voiceId);
        
        // 验证结果
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expectedAudioData, response.getBody());
        assertEquals("application/octet-stream", response.getHeaders().getContentType().toString());
        
        // 验证调用
        verify(textToSpeechService).getAudioData(voiceId);
    }

    @Test
    void testGetSpeechData_VoiceNotFound() {
        // 准备测试数据
        String voiceId = "non-existent-voice-id";
        
        // 模拟音频数据不存在
        when(textToSpeechService.getAudioData(voiceId)).thenReturn(null);
        
        // 执行测试并验证异常
        try {
            speechController.getSpeechData(voiceId);
        } catch (Exception e) {
            assertEquals("VOICE_FILE_NOT_FOUND", ((com.aicosplay.exception.BusinessException)e).getErrorCode());
        }
    }
}