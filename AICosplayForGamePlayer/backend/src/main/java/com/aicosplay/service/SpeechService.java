package com.aicosplay.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别服务接口
 * 提供语音转文字的功能
 */
public interface SpeechService {
    
    /**
     * 将语音文件转换为文本
     * @param audioFile 音频文件
     * @return 转换后的文本内容
     */
    String recognizeSpeech(MultipartFile audioFile);
    
    /**
     * 健康检查接口，用于测试服务是否正常运行
     * @return 健康状态信息
     */
    String healthCheck();
}