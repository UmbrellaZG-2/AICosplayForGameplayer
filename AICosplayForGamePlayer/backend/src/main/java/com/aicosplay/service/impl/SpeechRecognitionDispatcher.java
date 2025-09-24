package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechRecognitionService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
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
            VoskOfflineSpeechService voskOfflineSpeechService) {
        this.onlineService = speechServiceImpl;
        this.offlineService = voskOfflineSpeechService;
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
     * @return 识别结果
     */
    public String recognizeSpeech(MultipartFile audioFile) {
        // 默认优先使用离线服务
        SpeechRecognitionService primaryService = offlineService;
        SpeechRecognitionService fallbackService = onlineService;
        String result;

        // 带退避的重试机制配置
        int maxRetries = 5;
        long initialDelayMs = 1000; // 初始延迟1秒
        double backoffMultiplier = 1.5; // 退避倍数

        try {
            // 首先检查离线服务是否可用
            if (!primaryService.isAvailable()) {
                logger.warning("离线语音识别服务不可用，直接使用在线服务");
                return fallbackService.recognizeSpeech(audioFile);
            }

            // 尝试使用离线服务进行识别，带退避重试
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    result = primaryService.recognizeSpeech(audioFile);
                    logger.info("离线语音识别成功，使用服务类型: " + primaryService.getServiceType() + ", 尝试次数: " + (attempt + 1));
                    return result;
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
                    }
                }
            }

            // 离线服务重试超过五次，切换到科大讯飞在线服务
            logger.warning("离线语音识别服务重试" + maxRetries + "次均失败，切换到在线服务");
            if (fallbackService.isAvailable()) {
                try {
                    result = fallbackService.recognizeSpeech(audioFile);
                    logger.info("在线语音识别成功");
                    return result;
                } catch (Exception fallbackEx) {
                    logger.severe("在线语音识别也失败: " + fallbackEx.getMessage());
                    throw new RuntimeException("所有可用语音识别服务均失败", fallbackEx);
                }
            } else {
                logger.severe("在线语音识别服务不可用");
                throw new RuntimeException("离线服务重试多次失败且在线服务不可用");
            }
        } catch (Exception e) {
            logger.severe("语音识别处理过程中发生错误: " + e.getMessage());
            throw e;
        }
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