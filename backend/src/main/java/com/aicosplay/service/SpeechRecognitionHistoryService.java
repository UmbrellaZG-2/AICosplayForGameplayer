package com.aicosplay.service;

import com.aicosplay.entity.SpeechRecognitionHistory;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别历史记录服务接口
 * 提供语音识别历史记录的保存、查询等功能
 */
public interface SpeechRecognitionHistoryService {
    
    /**
     * 保存语音识别历史记录
     * @param history 语音识别历史记录实体
     * @return 保存后的历史记录实体
     */
    SpeechRecognitionHistory saveHistory(SpeechRecognitionHistory history);
    
    /**
     * 根据用户ID查询语音识别历史记录
     * @param userId 用户ID
     * @return 语音识别历史记录列表
     */
    List<SpeechRecognitionHistory> getHistoriesByUserId(String userId);
    
    /**
     * 根据用户ID和时间段查询语音识别历史记录
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 语音识别历史记录列表
     */
    List<SpeechRecognitionHistory> getHistoriesByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据用户ID和识别方式查询语音识别历史记录
     * @param userId 用户ID
     * @param recognitionType 识别方式（OFFLINE或API）
     * @return 语音识别历史记录列表
     */
    List<SpeechRecognitionHistory> getHistoriesByUserIdAndType(String userId, SpeechRecognitionHistory.RecognitionType recognitionType);
    
    /**
     * 查询指定用户的最新N条语音识别历史记录
     * @param userId 用户ID
     * @param limit 限制数量
     * @return 语音识别历史记录列表
     */
    List<SpeechRecognitionHistory> getLatestHistoriesByUserId(String userId, int limit);
    
    /**
     * 统计用户的语音识别成功次数
     * @param userId 用户ID
     * @return 成功次数
     */
    long countSuccessRecognitionsByUserId(String userId);
    
    /**
     * 统计用户的语音识别失败次数
     * @param userId 用户ID
     * @return 失败次数
     */
    long countFailedRecognitionsByUserId(String userId);
}