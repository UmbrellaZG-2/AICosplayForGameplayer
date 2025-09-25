package com.aicosplay.controller;

import com.aicosplay.entity.SpeechRecognitionHistory;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.SpeechRecognitionHistoryService;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 语音识别历史记录控制器
 * 提供查询语音识别历史记录的API接口
 */
@RestController
@RequestMapping("/api/speech/history")
public class SpeechRecognitionHistoryController {

    private final SpeechRecognitionHistoryService historyService;

    /**
     * 构造函数，注入语音识别历史记录服务
     */
    @Autowired
    public SpeechRecognitionHistoryController(SpeechRecognitionHistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * 获取当前用户的所有语音识别历史记录
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllHistories() {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        
        // 查询历史记录
        List<SpeechRecognitionHistory> histories = historyService.getHistoriesByUserId(username);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 获取当前用户指定时间段内的语音识别历史记录
     */
    @GetMapping("/time-range")
    public ResponseEntity<ApiResponse<?>> getHistoriesByTimeRange(
            @RequestParam("startTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam("endTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        
        // 查询历史记录
        List<SpeechRecognitionHistory> histories = historyService.getHistoriesByUserIdAndTimeRange(username, startTime, endTime);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 获取当前用户指定识别方式的语音识别历史记录
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<?>> getHistoriesByType(
            @PathVariable("type") String type) {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        
        // 验证识别类型
        SpeechRecognitionHistory.RecognitionType recognitionType;
        try {
            recognitionType = SpeechRecognitionHistory.RecognitionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_RECOGNITION_TYPE", "无效的识别类型: " + type);
        }
        
        // 查询历史记录
        List<SpeechRecognitionHistory> histories = historyService.getHistoriesByUserIdAndType(username, recognitionType);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 获取当前用户的最新N条语音识别历史记录
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<?>> getLatestHistories(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        
        // 查询历史记录
        List<SpeechRecognitionHistory> histories = historyService.getLatestHistoriesByUserId(username, limit);
        return ResponseEntity.ok(ApiResponse.success(histories));
    }

    /**
     * 获取当前用户的语音识别统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getStatistics() {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        
        // 统计信息
        long successCount = historyService.countSuccessRecognitionsByUserId(username);
        long failedCount = historyService.countFailedRecognitionsByUserId(username);
        long totalCount = successCount + failedCount;
        
        // 构建响应
        StatisticsResponse stats = new StatisticsResponse(successCount, failedCount, totalCount);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // 统计信息响应DTO
    public static class StatisticsResponse {
        private long successCount;
        private long failedCount;
        private long totalCount;

        public StatisticsResponse(long successCount, long failedCount, long totalCount) {
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.totalCount = totalCount;
        }

        // Getters
        public long getSuccessCount() { return successCount; }
        public long getFailedCount() { return failedCount; }
        public long getTotalCount() { return totalCount; }
    }


}