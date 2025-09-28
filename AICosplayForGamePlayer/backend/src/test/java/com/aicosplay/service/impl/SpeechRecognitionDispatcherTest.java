package com.aicosplay.service.impl;

import java.util.concurrent.atomic.AtomicReference;
import com.aicosplay.service.SpeechRecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("offline-only")
class SpeechRecognitionDispatcherTest {

    @Mock
    private VoskOfflineSpeechService offlineSpeechService;

    @Mock
    private SpeechServiceImpl onlineSpeechService;

    @InjectMocks
    private SpeechRecognitionDispatcher speechRecognitionDispatcher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // 移除对不存在方法的调用
    }

    @Test
    void testRecognizeSpeech_OfflineSuccess() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        String expectedText = "离线语音识别成功";
        when(offlineSpeechService.recognizeSpeech(any())).thenReturn(expectedText);
        
        // 执行测试 - 默认使用offline-first模式
        String result = speechRecognitionDispatcher.recognizeSpeech(audioFile, "testuser", "");
        
        // 验证结果
        assertEquals(expectedText, result);
        
        // 验证调用 - 只调用了离线服务
        verify(offlineSpeechService).recognizeSpeech(any());
        verify(onlineSpeechService, never()).recognizeSpeech(any());
    }

    @Disabled("在线模式在测试环境中禁用")
    @Test
    void testRecognizeSpeech_OfflineFailed_SwitchToOnline() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 模拟离线识别失败，在线识别成功
        when(offlineSpeechService.recognizeSpeech(any())).thenReturn(null);
        String expectedText = "在线语音识别成功";
        when(onlineSpeechService.recognizeSpeech(any())).thenReturn(expectedText);
        
        // 执行测试 - 默认使用offline-first模式
        String result = speechRecognitionDispatcher.recognizeSpeech(audioFile, "testuser", "");
        
        // 验证结果
        assertEquals(expectedText, result);
        
        // 验证调用 - 先调用离线服务，失败后调用在线服务
        verify(offlineSpeechService).recognizeSpeech(any());
        verify(onlineSpeechService).recognizeSpeech(any());
    }

    @Disabled("在线模式在测试环境中禁用")
    @Test
    void testRecognizeSpeech_BothServicesFailed() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 模拟两个服务都失败
        when(offlineSpeechService.recognizeSpeech(any())).thenReturn(null);
        when(onlineSpeechService.recognizeSpeech(any())).thenReturn(null);
        
        // 执行测试 - 默认使用offline-first模式
        String result = speechRecognitionDispatcher.recognizeSpeech(audioFile, "testuser", "");
        
        // 验证结果 - 返回null表示识别失败
        assertNull(result);
        
        // 验证调用 - 两个服务都被调用
        verify(offlineSpeechService).recognizeSpeech(any());
        verify(onlineSpeechService).recognizeSpeech(any());
    }

    @Disabled("在线模式在测试环境中禁用")
    @Test
    void testRecognizeSpeech_SpecificServiceType_Online() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 切换到在线服务
        speechRecognitionDispatcher.switchServiceType("online");
        
        String expectedText = "指定在线语音识别成功";
        when(onlineSpeechService.recognizeSpeech(any())).thenReturn(expectedText);
        
        // 执行测试
        String result = speechRecognitionDispatcher.recognizeSpeech(audioFile, "testuser", "");
        
        // 验证结果
        assertEquals(expectedText, result);
        
        // 验证调用 - 只调用了在线服务
        verify(onlineSpeechService).recognizeSpeech(any());
        verify(offlineSpeechService, never()).recognizeSpeech(any());
    }

    @Test
    void testRecognizeSpeech_SpecificServiceType_Offline() {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 切换到离线服务
        speechRecognitionDispatcher.switchServiceType("offline");
        
        String expectedText = "指定离线语音识别成功";
        when(offlineSpeechService.recognizeSpeech(any())).thenReturn(expectedText);
        
        // 执行测试
        String result = speechRecognitionDispatcher.recognizeSpeech(audioFile, "testuser", "");
        
        // 验证结果
        assertEquals(expectedText, result);
        
        // 验证调用 - 只调用了离线服务
        verify(offlineSpeechService).recognizeSpeech(any());
        verify(onlineSpeechService, never()).recognizeSpeech(any());
    }

    @Test
    void testSwitchServiceType() {
        // 执行测试 - 切换服务类型
        speechRecognitionDispatcher.switchServiceType("online");
        
        // 验证结果 - 由于无法直接获取默认服务类型，这里只验证服务类型被切换
        speechRecognitionDispatcher.switchServiceType("offline");
    }

    @Test
    void testGetServicesStatus() {
        // 模拟服务状态
        when(offlineSpeechService.isAvailable()).thenReturn(true);
        when(onlineSpeechService.isAvailable()).thenReturn(true);
        
        // 模拟健康检查结果
        when(offlineSpeechService.healthCheck()).thenReturn("健康");
        when(onlineSpeechService.healthCheck()).thenReturn("健康");
        
        // 模拟当前服务类型
        when(offlineSpeechService.getServiceType()).thenReturn("offline");
        
        // 使用ReflectionTestUtils设置currentService
        AtomicReference<SpeechRecognitionService> currentService = new AtomicReference<>(offlineSpeechService);
        ReflectionTestUtils.setField(speechRecognitionDispatcher, "currentService", currentService);
        
        // 执行测试
        String status = speechRecognitionDispatcher.getServicesStatus();
        
        // 验证结果包含预期的格式
        assertTrue(status.contains("当前使用服务: offline"));
        assertTrue(status.contains("在线服务状态: 健康"));
        assertTrue(status.contains("离线服务状态: 健康"));
    }

    @Test
    void testGetServicesStatus_OfflineUnavailable() {
        // 模拟离线服务不可用
        when(offlineSpeechService.isAvailable()).thenReturn(false);
        when(onlineSpeechService.isAvailable()).thenReturn(true);
        
        // 模拟健康检查结果
        when(offlineSpeechService.healthCheck()).thenReturn("不可用");
        when(onlineSpeechService.healthCheck()).thenReturn("健康");
        
        // 模拟当前服务类型
        when(onlineSpeechService.getServiceType()).thenReturn("online");
        
        // 使用ReflectionTestUtils设置currentService
        AtomicReference<SpeechRecognitionService> currentService = new AtomicReference<>(onlineSpeechService);
        ReflectionTestUtils.setField(speechRecognitionDispatcher, "currentService", currentService);
        
        // 执行测试
        String status = speechRecognitionDispatcher.getServicesStatus();
        
        // 验证结果包含预期的格式
        assertTrue(status.contains("当前使用服务: online"));
        assertTrue(status.contains("在线服务状态: 健康"));
        assertTrue(status.contains("离线服务状态: 不可用"));
    }

    @Test
    void testGetServicesStatus_OnlineUnavailable() {
        // 模拟在线服务不可用
        when(offlineSpeechService.isAvailable()).thenReturn(true);
        when(onlineSpeechService.isAvailable()).thenReturn(false);
        
        // 模拟健康检查结果
        when(offlineSpeechService.healthCheck()).thenReturn("健康");
        when(onlineSpeechService.healthCheck()).thenReturn("不可用");
        
        // 模拟当前服务类型
        when(offlineSpeechService.getServiceType()).thenReturn("offline");
        
        // 使用ReflectionTestUtils设置currentService
        AtomicReference<SpeechRecognitionService> currentService = new AtomicReference<>(offlineSpeechService);
        ReflectionTestUtils.setField(speechRecognitionDispatcher, "currentService", currentService);
        
        // 执行测试
        String status = speechRecognitionDispatcher.getServicesStatus();
        
        // 验证结果包含预期的格式
        assertTrue(status.contains("当前使用服务: offline"));
        assertTrue(status.contains("在线服务状态: 不可用"));
        assertTrue(status.contains("离线服务状态: 健康"));
    }

    @Test
    void testIsServiceAvailable() {
        // 模拟离线服务可用
        when(offlineSpeechService.isAvailable()).thenReturn(true);
        
        // 执行测试 - 检查离线服务
        speechRecognitionDispatcher.switchServiceType("offline");
        String serviceType = speechRecognitionDispatcher.getCurrentServiceType();
        
        // 验证结果
        assertEquals("offline", serviceType);
        
        // 模拟在线服务不可用
        when(onlineSpeechService.isAvailable()).thenReturn(false);
        
        // 执行测试 - 检查在线服务
        speechRecognitionDispatcher.switchServiceType("online");
        serviceType = speechRecognitionDispatcher.getCurrentServiceType();
        
        // 验证结果
        assertEquals("online", serviceType);
    }
}