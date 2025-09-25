package com.aicosplay.service.impl;

import com.aicosplay.entity.SpeechRecognitionHistory;
import com.aicosplay.repository.SpeechRecognitionHistoryRepository;
import com.aicosplay.service.SpeechRecognitionHistoryService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 语音识别历史记录服务实现类
 * 实现语音识别历史记录的保存、查询等功能
 */
@Service
public class SpeechRecognitionHistoryServiceImpl implements SpeechRecognitionHistoryService {
    
    private final SpeechRecognitionHistoryRepository speechRecognitionHistoryRepository;
    
    public SpeechRecognitionHistoryServiceImpl(SpeechRecognitionHistoryRepository speechRecognitionHistoryRepository) {
        this.speechRecognitionHistoryRepository = speechRecognitionHistoryRepository;
    }
    
    @Override
    public SpeechRecognitionHistory saveHistory(SpeechRecognitionHistory history) {
        // 确保创建时间已设置
        if (history.getCreatedAt() == null) {
            history.setCreatedAt(LocalDateTime.now());
        }
        return speechRecognitionHistoryRepository.save(history);
    }
    
    @Override
    public List<SpeechRecognitionHistory> getHistoriesByUserId(String userId) {
        return speechRecognitionHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    public List<SpeechRecognitionHistory> getHistoriesByUserIdAndTimeRange(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        return speechRecognitionHistoryRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, startTime, endTime);
    }
    
    @Override
    public List<SpeechRecognitionHistory> getHistoriesByUserIdAndType(String userId, SpeechRecognitionHistory.RecognitionType recognitionType) {
        return speechRecognitionHistoryRepository.findByUserIdAndRecognitionTypeOrderByCreatedAtDesc(userId, recognitionType);
    }
    
    @Override
    public List<SpeechRecognitionHistory> getLatestHistoriesByUserId(String userId, int limit) {
        return speechRecognitionHistoryRepository.findTopNByUserIdOrderByCreatedAtDesc(userId, limit);
    }
    
    @Override
    public long countSuccessRecognitionsByUserId(String userId) {
        return speechRecognitionHistoryRepository.countByUserIdAndSuccessTrue(userId);
    }
    
    @Override
    public long countFailedRecognitionsByUserId(String userId) {
        return speechRecognitionHistoryRepository.countByUserIdAndSuccessFalse(userId);
    }
}