package com.aicosplay.repository;

import com.aicosplay.entity.SpeechRecognitionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SpeechRecognitionHistoryRepository extends JpaRepository<SpeechRecognitionHistory, Long> {

    /**
     * 根据用户ID查询语音识别历史记录，按创建时间倒序排列
     */
    List<SpeechRecognitionHistory> findByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 根据用户ID和时间段查询语音识别历史记录，按创建时间倒序排列
     */
    List<SpeechRecognitionHistory> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据用户ID和识别方式查询语音识别历史记录，按创建时间倒序排列
     */
    List<SpeechRecognitionHistory> findByUserIdAndRecognitionTypeOrderByCreatedAtDesc(String userId, SpeechRecognitionHistory.RecognitionType recognitionType);

    /**
     * 查询指定用户的最新N条语音识别历史记录
     */
    List<SpeechRecognitionHistory> findTop100ByUserIdOrderByCreatedAtDesc(String userId, org.springframework.data.domain.Pageable pageable);

    /**
     * 统计用户的语音识别成功次数
     */
    long countByUserIdAndSuccessTrue(String userId);

    /**
     * 统计用户的语音识别失败次数
     */
    long countByUserIdAndSuccessFalse(String userId);
}