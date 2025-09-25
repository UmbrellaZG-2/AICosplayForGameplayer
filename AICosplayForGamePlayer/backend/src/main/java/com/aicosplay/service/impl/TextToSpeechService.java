package com.aicosplay.service.impl;

import com.aicosplay.entity.Message;
import com.aicosplay.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import javax.sound.sampled.AudioFileFormat;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

@Service
public class TextToSpeechService {

    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);
    private static final String VOICE_NAME = "kevin16";
    
    public TextToSpeechService() {
        // 初始化FreeTTS
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
    }
    
    /**
     * 将文本转换为语音数据
     * @param message 要转换的消息对象
     * @return 更新后的消息对象
     */
    public Message convertTextToSpeech(Message message) {
        if (message == null || !StringUtils.hasText(message.getContent())) {
            logger.warn("无法转换为空消息或空内容的文本到语音");
            return message;
        }
        
        try {
            // 执行文字转语音
            byte[] audioData = generateSpeechAudio(message.getContent());
            
            // 存储音频数据并获取语音ID
            String voiceId = storeAudioData(audioData);
            
            // 更新消息对象的语音相关字段
            message.setIsVoiceMessage((byte) 1);
            message.setVoiceFilePath(voiceId); // 使用voiceId替代文件路径
            message.setVoiceDuration(calculateAudioDuration(audioData));
            message.setShowTextButton((byte) 1); // 默认显示文字按钮
            
            logger.info("成功将消息转换为语音数据");
            return message;
        } catch (Exception e) {
            logger.error("文字转语音失败: {}", e.getMessage(), e);
            throw new BusinessException("TEXT_TO_SPEECH_ERROR", "文字转语音处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用FreeTTS生成语音音频数据
     */
    private byte[] generateSpeechAudio(String text) throws Exception {
        // 获取VoiceManager实例
        VoiceManager voiceManager = VoiceManager.getInstance();
        
        // 获取指定的语音
        Voice voice = voiceManager.getVoice(VOICE_NAME);
        if (voice == null) {
            throw new RuntimeException("无法找到语音: " + VOICE_NAME);
        }
        
        // 分配语音资源
        voice.allocate();
        
        try {
            // 创建临时文件
        File tempFile = File.createTempFile("tts_", ".wav");
        tempFile.deleteOnExit(); // 程序退出时删除临时文件
        String tempFilePath = tempFile.getAbsolutePath();
        
        // 创建文件音频播放器
        com.sun.speech.freetts.audio.SingleFileAudioPlayer audioPlayer = 
            new com.sun.speech.freetts.audio.SingleFileAudioPlayer(tempFilePath.replace(".wav", ""), AudioFileFormat.Type.WAVE);
        
        // 设置音频播放器
        voice.setAudioPlayer(audioPlayer);
        
        // 朗读文本
        voice.speak(text);
        
        // 关闭音频播放器，确保数据写入文件
        audioPlayer.close();
        
        // 读取临时文件中的音频数据
        byte[] audioData = new byte[(int) tempFile.length()];
        try (FileInputStream fis = new FileInputStream(tempFile)) {
            fis.read(audioData);
        } catch (IOException e) {
            logger.error("Failed to read audio file: {}", e.getMessage());
            throw new RuntimeException("Failed to read audio file", e);
        }
        
        // 删除临时文件
        tempFile.delete();
            
            return audioData;
        } finally {
            // 释放语音资源
            voice.deallocate();
        }
    }
    
    /**
     * 计算音频时长（秒）
     */
    private Integer calculateAudioDuration(byte[] audioData) {
        // 假设采样率为16kHz，16位，单声道
        // 计算音频时长（秒）：数据长度 / (采样率 * 位深度/8 * 声道数)
        float duration = (float) audioData.length / (16000 * 2 * 1);
        return Math.round(duration);
    }
    
    /**
     * 获取语音文件的URL路径
     */
    public String getVoiceFileUrl(String voiceId) {
        if (!StringUtils.hasText(voiceId)) {
            return null;
        }
        
        // 返回音频数据获取的URL路径
        return "/api/speech/" + voiceId;
    }
    
    // 音频数据存储，用于临时保存生成的音频数据
    private final java.util.Map<String, byte[]> audioDataCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 存储音频数据并返回语音ID
     */
    public String storeAudioData(byte[] audioData) {
        String voiceId = "voice_" + UUID.randomUUID().toString();
        audioDataCache.put(voiceId, audioData);
        
        // 设置自动过期，5分钟后移除缓存的数据
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                audioDataCache.remove(voiceId);
            }
        }, 5 * 60 * 1000);
        
        return voiceId;
    }
    
    /**
     * 根据语音ID获取音频数据
     */
    public byte[] getAudioData(String voiceId) {
        return audioDataCache.get(voiceId);
    }
}