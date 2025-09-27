package com.aicosplay.service.impl;

import com.aicosplay.entity.SpeechRecognitionHistory;
import com.aicosplay.service.SpeechRecognitionHistoryService;
import com.aicosplay.service.SpeechRecognitionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * 语音识别调度器
 * 实现识别模式智能调度机制，根据配置和服务状态实现无缝切换
 */
@Service
public class SpeechRecognitionDispatcher {

    private static final Logger logger = Logger.getLogger(SpeechRecognitionDispatcher.class.getName());

    // 在线语音识别服务
    private final SpeechRecognitionService onlineService;

    // 离线语音识别服务
    private final SpeechRecognitionService offlineService;

    private final SpeechRecognitionHistoryService speechRecognitionHistoryService;

    // 默认识别模式配置
    @Value("${speech.recognition.default-mode:offline-first}")
    private String defaultMode;

    // 当前使用的服务
    private final AtomicReference<SpeechRecognitionService> currentService = new AtomicReference<>();

    /**
     * 构造函数
     */
    @Autowired
    public SpeechRecognitionDispatcher(
            SpeechServiceImpl speechServiceImpl, 
            VoskOfflineSpeechService voskOfflineSpeechService,
            SpeechRecognitionHistoryService speechRecognitionHistoryService) {
        this.onlineService = speechServiceImpl;
        this.offlineService = voskOfflineSpeechService;
        this.speechRecognitionHistoryService = speechRecognitionHistoryService;
    }
    
    /**
     * 初始化方法，在所有依赖注入完成后执行
     */
    @PostConstruct
    public void init() {
        try {
            // 短暂延迟，确保所有依赖服务都有足够时间完成初始化
            Thread.sleep(100);
            logger.info("开始初始化语音识别调度器");
            initializeCurrentService();
        } catch (Exception e) {
            logger.severe("初始化语音识别调度器时发生异常: " + e.getMessage());
            // 设置默认使用在线服务作为后备方案
            try {
                if (onlineService != null && onlineService.isAvailable()) {
                    currentService.set(onlineService);
                    logger.warning("调度器初始化异常，已回退到在线服务");
                } else {
                    logger.warning("调度器初始化异常，且在线服务不可用");
                }
            } catch (Exception ex) {
                logger.severe("回退到在线服务也失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 初始化当前使用的服务
     */
    private void initializeCurrentService() {
        if ("offline-first".equalsIgnoreCase(defaultMode)) {
            if (offlineService.isAvailable()) {
                currentService.set(offlineService);
                logger.info("当前使用离线语音识别服务");
            } else {
                currentService.set(onlineService);
                logger.warning("离线语音识别服务不可用，已切换到在线服务");
            }
        } else {
            currentService.set(onlineService);
            logger.info("当前使用在线语音识别服务");
        }
    }

    /**
     * 执行语音识别，实现智能切换逻辑
     * @param audioFile 音频文件
     * @param userId 用户ID
     * @param characterInfo 角色信息（可选）
     * @return 识别结果
     */
    public String recognizeSpeech(MultipartFile audioFile, String userId, String characterInfo) {
        // 创建历史记录对象
        SpeechRecognitionHistory history = new SpeechRecognitionHistory();
        history.setUserId(userId);
        history.setCharacterInfo(characterInfo != null ? characterInfo : "");
        history.setAudioDuration((int) Math.round(calculateAudioDuration(audioFile.getSize())));
        history.setSuccess(false);
        
        // 记录开始时间
        LocalDateTime startTime = LocalDateTime.now();
        String result = null;
        String errorMessage = null;
        SpeechRecognitionService.RecognitionType recognitionType = null;
        
        try {
            // 默认优先使用离线服务
            SpeechRecognitionService primaryService = offlineService;
            SpeechRecognitionService fallbackService = onlineService;

            // 带退避的重试机制配置
            int maxRetries = 5;
            long initialDelayMs = 1000; // 初始延迟1秒
            double backoffMultiplier = 1.5; // 退避倍数

            try {
                // 首先检查离线服务是否可用
                if (!primaryService.isAvailable()) {
                    logger.warning("离线语音识别服务不可用，直接使用在线服务");
                    result = fallbackService.recognizeSpeech(audioFile);
                    recognitionType = fallbackService.getRecognitionType();
                } else {
                    // 尝试使用离线服务进行识别，带退避重试
                    for (int attempt = 0; attempt < maxRetries; attempt++) {
                        try {
                            result = primaryService.recognizeSpeech(audioFile);
                            recognitionType = primaryService.getRecognitionType();
                            logger.info("离线语音识别成功，使用服务类型: " + primaryService.getServiceType() + ", 尝试次数: " + (attempt + 1));
                            break;
                        } catch (Exception e) {
                            logger.warning("离线语音识别第" + (attempt + 1) + "次失败: " + e.getMessage());

                            // 如果不是最后一次尝试，则等待后重试
                            if (attempt < maxRetries - 1) {
                                long delayMs = (long) (initialDelayMs * Math.pow(backoffMultiplier, attempt));
                                logger.info("将在" + delayMs + "毫秒后重试离线识别");
                                try {
                                    Thread.sleep(delayMs);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    logger.warning("重试等待被中断，将进行下一次尝试");
                                }
                            } else {
                                // 最后一次尝试失败，切换到在线服务
                                logger.warning("离线语音识别服务重试" + maxRetries + "次均失败，切换到在线服务");
                                if (fallbackService.isAvailable()) {
                                    try {
                                        result = fallbackService.recognizeSpeech(audioFile);
                                        recognitionType = fallbackService.getRecognitionType();
                                        logger.info("在线语音识别成功");
                                    } catch (Exception fallbackEx) {
                                        logger.severe("在线语音识别也失败: " + fallbackEx.getMessage());
                                        errorMessage = "所有可用语音识别服务均失败: " + fallbackEx.getMessage();
                                        throw new RuntimeException(errorMessage, fallbackEx);
                                    }
                                } else {
                                    logger.severe("在线语音识别服务不可用");
                                    errorMessage = "离线服务重试多次失败且在线服务不可用";
                                    throw new RuntimeException(errorMessage);
                                }
                            }
                        }
                    }
                }
                
                // 设置识别成功信息
                history.setSuccess(true);
                history.setRecognizedText(result);
                history.setRecognitionType(SpeechRecognitionHistory.RecognitionType.valueOf(recognitionType.name()));
                
                return result;
            } catch (Exception e) {
                logger.severe("语音识别处理过程中发生错误: " + e.getMessage());
                errorMessage = e.getMessage();
                // 不直接抛出异常，而是设置为识别结果为null
                // throw e;
            }
        } finally {
            // 记录结束时间
            LocalDateTime endTime = LocalDateTime.now();
            
            // 计算识别耗时
            Duration duration = Duration.between(startTime, endTime);
            logger.info("语音识别总耗时: " + duration.toMillis() + "毫秒");
            
            // 设置错误信息（如果有）
            if (errorMessage != null) {
                history.setErrorMessage(errorMessage);
            }
            
            // 保存历史记录
            try {
                speechRecognitionHistoryService.saveHistory(history);
                logger.info("语音识别历史记录保存成功");
            } catch (Exception e) {
                // 保存历史记录失败不应影响主流程
                logger.warning("保存语音识别历史记录失败: " + e.getMessage());
            }
        }
        
        // 如果执行到这里，说明识别失败，返回错误信息而不是抛出异常
        return null;
    }
    
    /**
     * 计算音频文件时长（基于文件大小估算）
     * 假设音频格式为16kHz采样率，16bit位深，单声道
     * @param fileSize 文件大小（字节）
     * @return 音频时长（秒）
     */
    private double calculateAudioDuration(long fileSize) {
        // 16kHz, 16bit, 单声道的音频每秒约32KB
        double bytesPerSecond = 16000 * 2; // 16kHz * 2字节/采样点
        return fileSize / bytesPerSecond;
    }

    /**
     * 获取备用服务
     */
    private SpeechRecognitionService getFallbackService(SpeechRecognitionService currentService) {
        if (currentService == offlineService) {
            return onlineService;
        } else if (currentService == onlineService && offlineService.isAvailable()) {
            return offlineService;
        }
        return null;
    }

    /**
     * 获取当前使用的服务类型
     */
    public String getCurrentServiceType() {
        return currentService.get().getServiceType();
    }

    /**
     * 手动切换服务类型
     */
    public void switchServiceType(String serviceType) {
        if ("offline".equalsIgnoreCase(serviceType) && offlineService.isAvailable()) {
            currentService.set(offlineService);
            logger.info("手动切换到离线语音识别服务");
        } else if ("online".equalsIgnoreCase(serviceType)) {
            currentService.set(onlineService);
            logger.info("手动切换到在线语音识别服务");
        } else {
            logger.warning("无效的服务类型或服务不可用: " + serviceType);
        }
    }

    /**
     * 获取所有可用服务的状态
     */
    public String getServicesStatus() {
        StringBuilder statusBuilder = new StringBuilder();
        statusBuilder.append("当前使用服务: ").append(currentService.get().getServiceType()).append("\n");
        statusBuilder.append("在线服务状态: ").append(onlineService.healthCheck()).append("\n");
        statusBuilder.append("离线服务状态: ").append(offlineService.healthCheck());
        return statusBuilder.toString();
    }

    /**
     * 重新初始化服务选择
     */
    public void reinitialize() {
        initializeCurrentService();
    }
}