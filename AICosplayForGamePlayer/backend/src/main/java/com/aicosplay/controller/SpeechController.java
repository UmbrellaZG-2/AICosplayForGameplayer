package com.aicosplay.controller;

import com.aicosplay.exception.BusinessException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.service.impl.SpeechRecognitionDispatcher;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 语音识别控制器
 * 处理语音转文字的API请求
 */
@RestController
@RequestMapping("/api/speech")
public class SpeechController {
    
    @Autowired
    private SpeechRecognitionDispatcher speechRecognitionDispatcher;
    
    /**
     * 语音识别API
     * 接收音频文件并返回识别后的文本
     */
    @PostMapping("/recognize")
    public ResponseEntity<ApiResponse<?>> recognizeSpeech(@RequestParam("audio") MultipartFile audioFile) {
        // 验证用户是否登录
        String username = UserContext.getCurrentUsername();
        if (username == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        // 验证文件是否为空
        if (audioFile.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "音频文件不能为空");
        }
        
        // 验证文件大小（限制为10MB）
        if (audioFile.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("FILE_TOO_LARGE", "音频文件大小不能超过10MB");
        }
        
        // 调用语音识别调度器进行识别，并传递用户ID
        String recognizedText = speechRecognitionDispatcher.recognizeSpeech(audioFile, username, "");
        
        // 返回识别结果
        return ResponseEntity.ok(ApiResponse.success(recognizedText));
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> healthCheck() {
        String status = speechRecognitionDispatcher.getServicesStatus();
        return ResponseEntity.ok(ApiResponse.success(status));
    }
    
    /**
     * 切换语音识别服务类型
     */
    @PostMapping("/switch")
    public ResponseEntity<ApiResponse<?>> switchServiceType(@RequestParam String type) {
        // 验证用户是否登录
        if (UserContext.getCurrentUser() == null) {
            throw new BusinessException("USER_NOT_AUTHENTICATED", "用户未登录");
        }
        
        speechRecognitionDispatcher.switchServiceType(type);
        return ResponseEntity.ok(ApiResponse.success("服务类型切换成功"));
    }
}