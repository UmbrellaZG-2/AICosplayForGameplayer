package com.aicosplay.service.impl;

import com.aicosplay.entity.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TextToSpeechServiceTest {

    @InjectMocks
    private TextToSpeechService textToSpeechService;

    private Map<String, byte[]> audioDataCache;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        // 获取audioDataCache字段并设置为可访问
        Field cacheField = TextToSpeechService.class.getDeclaredField("audioDataCache");
        cacheField.setAccessible(true);
        audioDataCache = (Map<String, byte[]>) cacheField.get(textToSpeechService);
        audioDataCache.clear(); // 清空缓存，确保测试独立性
    }

    @Test
    void convertTextToSpeech_Success() {
        // 创建测试消息
        Message message = new Message();
        message.setContent("Hello, this is a test message.");

        // 执行方法
        Message result = textToSpeechService.convertTextToSpeech(message);

        // 验证结果
        assertNotNull(result);
        assertEquals((byte) 1, result.getIsVoiceMessage());
        assertNotNull(result.getVoiceFilePath());
        assertTrue(StringUtils.hasText(result.getVoiceFilePath()));
        assertNotNull(result.getVoiceDuration());
        assertTrue(result.getVoiceDuration() > 0);
        assertEquals((byte) 1, result.getShowTextButton());
    }

    @Test
    void convertTextToSpeech_NullMessage() {
        // 测试空消息
        Message result = textToSpeechService.convertTextToSpeech(null);
        assertNull(result);
    }

    @Test
    void convertTextToSpeech_EmptyContent() {
        // 测试空内容
        Message message = new Message();
        message.setContent("");

        Message result = textToSpeechService.convertTextToSpeech(message);
        assertEquals(message, result);
        assertNull(result.getIsVoiceMessage());
    }

    @Test
    void storeAudioDataAndGetAudioData_Success() {
        // 准备测试数据
        byte[] testAudioData = new byte[]{1, 2, 3, 4, 5};

        // 存储音频数据
        String voiceId = textToSpeechService.storeAudioData(testAudioData);

        // 验证存储成功
        assertNotNull(voiceId);
        assertTrue(voiceId.startsWith("voice_"));

        // 获取音频数据
        byte[] retrievedData = textToSpeechService.getAudioData(voiceId);

        // 验证获取的数据与原始数据一致
        assertNotNull(retrievedData);
        assertArrayEquals(testAudioData, retrievedData);
    }

    @Test
    void getAudioData_NonExistentVoiceId() {
        // 测试获取不存在的语音ID
        String nonExistentVoiceId = "voice_non_existent";
        byte[] retrievedData = textToSpeechService.getAudioData(nonExistentVoiceId);
        assertNull(retrievedData);
    }

    @Test
    void getVoiceFileUrl_Success() {
        // 准备测试数据
        String voiceId = "test_voice_id";

        // 获取语音文件URL
        String url = textToSpeechService.getVoiceFileUrl(voiceId);

        // 验证URL格式正确
        assertNotNull(url);
        assertEquals("/api/speech/" + voiceId, url);
    }

    @Test
    void getVoiceFileUrl_NullOrEmptyVoiceId() {
        // 测试null语音ID
        assertNull(textToSpeechService.getVoiceFileUrl(null));

        // 测试空语音ID
        assertNull(textToSpeechService.getVoiceFileUrl(""));
    }

    @Test
    void convertTextToSpeech_InvalidText() {
        // 测试特殊字符
        Message message = new Message();
        message.setContent("!@#$%^&*()");

        Message result = textToSpeechService.convertTextToSpeech(message);

        // 验证结果
        assertNotNull(result);
        assertEquals((byte) 1, result.getIsVoiceMessage());
        assertNotNull(result.getVoiceFilePath());
    }

    @Test
    void convertTextToSpeech_LongText() {
        // 创建一个较长的文本消息
        StringBuilder longTextBuilder = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longTextBuilder.append("This is a long text test. ");
        }
        String longText = longTextBuilder.toString();

        Message message = new Message();
        message.setContent(longText);

        Message result = textToSpeechService.convertTextToSpeech(message);

        // 验证结果
        assertNotNull(result);
        assertEquals((byte) 1, result.getIsVoiceMessage());
        assertNotNull(result.getVoiceFilePath());
        assertNotNull(result.getVoiceDuration());
        assertTrue(result.getVoiceDuration() > 10); // 较长文本应该有较长的时长
    }

    @Test
    void audioDataCacheSizeLimit() {
        // 模拟添加多个音频数据到缓存
        for (int i = 0; i < 10; i++) {
            byte[] audioData = new byte[]{(byte) i};
            textToSpeechService.storeAudioData(audioData);
        }

        // 验证缓存大小不超过限制（当前没有明确限制，但应该能够存储多个条目）
        assertTrue(audioDataCache.size() >= 10);
    }

    @Test
    void storeAudioData_UniqueVoiceId() {
        // 测试存储多个音频数据，确保每个返回的voiceId都是唯一的
        Map<String, Boolean> voiceIds = new HashMap<>();
        for (int i = 0; i < 5; i++) {
            byte[] audioData = new byte[]{(byte) i};
            String voiceId = textToSpeechService.storeAudioData(audioData);
            assertFalse(voiceIds.containsKey(voiceId));
            voiceIds.put(voiceId, true);
        }
    }
}