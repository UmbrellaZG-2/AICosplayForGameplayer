package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechRecognitionService.RecognitionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockMultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VoskOfflineSpeechServiceTest {

    private VoskOfflineSpeechService voskOfflineSpeechService;
    private AutoCloseable mockitoCloseable;

    private Model mockModel;
    private Recognizer mockRecognizer;
    private Encoder mockEncoder;

    private MockedStatic<Model> modelStatic;
    private MockedStatic<Recognizer> recognizerStatic;
    private MockedStatic<Encoder> encoderStatic;

    private String modelPath = "../../vosk-model-small-cn-0.22/";
    private float sampleRate = 16000.0f;

    @BeforeEach
    void setUp() {
        mockitoCloseable = MockitoAnnotations.openMocks(this);
        
        // 创建模拟对象
        mockModel = mock(Model.class);
        mockRecognizer = mock(Recognizer.class);
        mockEncoder = mock(Encoder.class);
        
        // 设置静态模拟 - 使用doReturn语法避免类型混淆
        modelStatic = mockStatic(Model.class);
        try {
            modelStatic.when(() -> new Model(modelPath)).thenReturn(mockModel);
        } catch (Exception e) {
            // 如果静态构造函数模拟失败，至少确保测试能继续
        }
        
        recognizerStatic = mockStatic(Recognizer.class);
        try {
            recognizerStatic.when(() -> new Recognizer(mockModel, sampleRate)).thenReturn(mockRecognizer);
        } catch (Exception e) {
            // 如果静态构造函数模拟失败，至少确保测试能继续
        }
        
        encoderStatic = mockStatic(Encoder.class);
        try {
            encoderStatic.when(Encoder::new).thenReturn(mockEncoder);
        } catch (Exception e) {
            // 如果静态构造函数模拟失败，至少确保测试能继续
        }
        
        // 创建服务实例
        voskOfflineSpeechService = new VoskOfflineSpeechService();
        
        // 使用反射设置属性（因为这些属性通常由Spring注入）
        try {
            voskOfflineSpeechService.getClass().getDeclaredField("modelPath").setAccessible(true);
            voskOfflineSpeechService.getClass().getDeclaredField("modelPath").set(voskOfflineSpeechService, modelPath);
            
            voskOfflineSpeechService.getClass().getDeclaredField("sampleRate").setAccessible(true);
            voskOfflineSpeechService.getClass().getDeclaredField("sampleRate").set(voskOfflineSpeechService, sampleRate);
        } catch (Exception e) {
            fail("Failed to set up test environment: " + e.getMessage());
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        mockitoCloseable.close();
        if (modelStatic != null) modelStatic.close();
        if (recognizerStatic != null) recognizerStatic.close();
        if (encoderStatic != null) encoderStatic.close();
    }

    @Test
    void testInitialize_Success() {
        // 执行测试
        voskOfflineSpeechService.initialize();
        
        // 验证结果
        assertTrue(voskOfflineSpeechService.isAvailable());
        modelStatic.verify(() -> new Model(modelPath));
    }

    @Test
    void testInitialize_Failure() throws IOException {
        // 模拟初始化失败
        modelStatic.when(() -> new Model(anyString())).thenThrow(new IOException("Model initialization failed"));
        
        // 执行测试
        voskOfflineSpeechService.initialize();
        
        // 验证结果
        assertFalse(voskOfflineSpeechService.isAvailable());
    }

    @Test
    void testShutdown() throws Exception {
        // 初始化服务
        voskOfflineSpeechService.initialize();
        
        // 执行测试
        voskOfflineSpeechService.shutdown();
        
        // 验证结果
        verify(mockModel).close();
    }

    @Test
    void testRecognizeSpeech_Success() throws Exception {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 创建临时WAV文件用于测试
        Path tempWavPath = Files.createTempFile("test", ".wav");
        File tempWavFile = tempWavPath.toFile();
        
        try {
            // 写入基本的WAV头信息和内容
            try (FileOutputStream fos = new FileOutputStream(tempWavFile)) {
                byte[] wavHeader = new byte[44]; // 基本的WAV头
                fos.write(wavHeader);
                fos.write(testAudioContent.getBytes());
            }
            
            // 模拟音频转换
            doNothing().when(mockEncoder).encode(any(MultimediaObject.class), any(File.class), any(EncodingAttributes.class));
            
            // 模拟识别器行为
            when(mockRecognizer.acceptWaveForm(any(byte[].class), anyInt())).thenReturn(true, false);
            when(mockRecognizer.getResult()).thenReturn("{\"text\":\"识别结果1\"}");
            when(mockRecognizer.getFinalResult()).thenReturn("{\"text\":\"识别结果2\"}");
            
            // 初始化服务
            voskOfflineSpeechService.initialize();
            
            // 执行测试
            String result = voskOfflineSpeechService.recognizeSpeech(audioFile);
            
            // 验证结果
            assertEquals("识别结果1识别结果2", result);
        } finally {
            // 清理临时文件
            if (tempWavFile.exists()) {
                tempWavFile.delete();
            }
        }
    }

    @Test
    void testRecognizeSpeech_ServiceUnavailable() {
        // 不初始化服务，确保它不可用
        
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voskOfflineSpeechService.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertEquals("Vosk离线语音识别服务不可用", exception.getMessage());
    }

    @Test
    void testRecognizeSpeech_RecognitionFailed() throws Exception {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 创建临时WAV文件用于测试
        Path tempWavPath = Files.createTempFile("test", ".wav");
        File tempWavFile = tempWavPath.toFile();
        
        try {
            // 写入基本的WAV头信息和内容
            try (FileOutputStream fos = new FileOutputStream(tempWavFile)) {
                byte[] wavHeader = new byte[44]; // 基本的WAV头
                fos.write(wavHeader);
                fos.write(testAudioContent.getBytes());
            }
            
            // 模拟音频转换
            doNothing().when(mockEncoder).encode(any(MultimediaObject.class), any(File.class), any(EncodingAttributes.class));
            
            // 模拟识别器行为 - 返回空结果
            when(mockRecognizer.acceptWaveForm(any(byte[].class), anyInt())).thenReturn(false);
            when(mockRecognizer.getFinalResult()).thenReturn("{\"text\":\"\"}");
            
            // 初始化服务
            voskOfflineSpeechService.initialize();
            
            // 执行测试并验证异常
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                voskOfflineSpeechService.recognizeSpeech(audioFile);
            });
            
            // 验证异常消息
            assertEquals("未识别到有效文本", exception.getMessage());
        } finally {
            // 清理临时文件
            if (tempWavFile.exists()) {
                tempWavFile.delete();
            }
        }
    }

    @Test
    void testHealthCheck_Available() {
        // 初始化服务
        voskOfflineSpeechService.initialize();
        
        // 执行测试
        String status = voskOfflineSpeechService.healthCheck();
        
        // 验证结果
        assertTrue(status.contains("运行正常"));
        assertTrue(status.contains(modelPath));
    }

    @Test
    void testHealthCheck_Unavailable() {
        // 不初始化服务，确保它不可用
        
        // 执行测试
        String status = voskOfflineSpeechService.healthCheck();
        
        // 验证结果
        assertTrue(status.contains("不可用"));
        assertTrue(status.contains(modelPath));
    }

    @Test
    void testGetServiceType() {
        // 执行测试
        String serviceType = voskOfflineSpeechService.getServiceType();
        
        // 验证结果
        assertEquals("offline", serviceType);
    }

    @Test
    void testGetRecognitionType() {
        // 执行测试
        RecognitionType recognitionType = voskOfflineSpeechService.getRecognitionType();
        
        // 验证结果
        assertEquals(RecognitionType.OFFLINE, recognitionType);
    }

    @Test
    void testIsAvailable() {
        // 未初始化时应该不可用
        assertFalse(voskOfflineSpeechService.isAvailable());
        
        // 初始化后应该可用
        voskOfflineSpeechService.initialize();
        assertTrue(voskOfflineSpeechService.isAvailable());
    }

    @Test
    void testConvertToWav_EncoderException() throws Exception {
        // 准备测试数据
        String testAudioContent = "test audio content";
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", 
                "test.wav", 
                "audio/wav", 
                testAudioContent.getBytes()
        );
        
        // 模拟编码器异常
        doThrow(mock(EncoderException.class)).when(mockEncoder).encode(
                any(MultimediaObject.class), any(File.class), any(EncodingAttributes.class)
        );
        
        // 初始化服务
        voskOfflineSpeechService.initialize();
        
        // 执行测试并验证异常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            voskOfflineSpeechService.recognizeSpeech(audioFile);
        });
        
        // 验证异常消息
        assertTrue(exception.getMessage().contains("离线语音识别失败"));
    }
}