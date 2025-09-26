package com.aicosplay.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别服务统一接口
 * 规范语音识别服务需实现的核心方法，确保在线与离线服务的调用一致性
 */
public interface SpeechRecognitionService {
    
    /**
     * 识别类型枚举
     */
    enum RecognitionType {
        API,    // 使用在线API识别
        OFFLINE // 使用离线识别
    }

    /**
     * 开始识别语音文件
     * @param audioFile 音频文件
     * @return 识别出的文本内容
     */
    String recognizeSpeech(MultipartFile audioFile);

    /**
     * 健康检查接口
     * @return 服务状态信息
     */
    String healthCheck();

    /**
     * 获取服务类型标识
     * @return 服务类型名称（如"online"或"offline"）
     */
    String getServiceType();
    
    /**
     * 获取识别类型
     * @return 识别类型枚举值
     */
    RecognitionType getRecognitionType();

    /**
     * 检查服务是否可用
     * @return 服务是否可用的布尔值
     */
    boolean isAvailable();

    /**
     * 初始化服务
     */
    void initialize();

    /**
     * 关闭服务资源
     */
    void shutdown();
}