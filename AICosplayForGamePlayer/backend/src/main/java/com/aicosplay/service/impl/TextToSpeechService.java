package com.aicosplay.service.impl;

import com.aicosplay.entity.Message;
import com.aicosplay.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

@Service
public class TextToSpeechService {

    private static final Logger logger = LoggerFactory.getLogger(TextToSpeechService.class);
    private static final String AUDIO_FORMAT = "wav";
    private static final String VOICE_NAME = "kevin16";
    
    @Value("${audio.storage.path}")
    private String audioStoragePath;
    
    public TextToSpeechService() {
        // 初始化FreeTTS
        System.setProperty("freetts.voices", "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");
    }
    
    /**
     * 将文本转换为语音并保存到文件
     * @param message 要转换的消息对象
     * @return 更新后的消息对象
     */
    public Message convertTextToSpeech(Message message) {
        if (message == null || !StringUtils.hasText(message.getContent())) {
            logger.warn("无法转换为空消息或空内容的文本到语音");
            return message;
        }
        
        try {
            // 创建音频存储目录（如果不存在）
            Path storagePath = Paths.get(audioStoragePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            // 生成唯一的文件名
            String filename = "ai_voice_" + UUID.randomUUID() + "." + AUDIO_FORMAT;
            String filePath = storagePath.resolve(filename).toString();
            
            // 执行文字转语音
            byte[] audioData = generateSpeechAudio(message.getContent());
            
            // 保存音频数据到文件
            saveAudioToFile(audioData, filePath);
            
            // 更新消息对象的语音相关字段
            message.setIsVoiceMessage((byte) 1);
            message.setVoiceFilePath(filePath);
            message.setVoiceDuration(calculateAudioDuration(audioData));
            message.setShowTextButton((byte) 1); // 默认显示文字按钮
            
            logger.info("成功将消息转换为语音并保存: {}", filePath);
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
            // 创建内存音频流
            com.sun.speech.freetts.audio.JavaStreamingAudioPlayer audioPlayer = 
                new com.sun.speech.freetts.audio.JavaStreamingAudioPlayer();
            
            // 设置音频播放器
            voice.setAudioPlayer(audioPlayer);
            
            // 朗读文本
            voice.speak(text);
            
            // 获取音频数据
            byte[] audioData = audioPlayer.getAudioData();
            return audioData;
        } finally {
            // 释放语音资源
            voice.deallocate();
        }
    }
    
    /**
     * 保存音频数据到文件
     */
    private void saveAudioToFile(byte[] audioData, String filePath) throws Exception {
        // 创建文件对象
        File audioFile = new File(filePath);
        
        // 设置音频格式
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        
        // 创建音频输入流
        AudioInputStream audioInputStream = new AudioInputStream(
                new ByteArrayInputStream(audioData), format, audioData.length / format.getFrameSize());
        
        // 保存音频文件
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, audioFile);
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
    public String getVoiceFileUrl(String voiceFilePath) {
        if (!StringUtils.hasText(voiceFilePath)) {
            return null;
        }
        
        // 提取相对路径作为URL
        File file = new File(voiceFilePath);
        return "/audio/" + file.getName();
    }
}