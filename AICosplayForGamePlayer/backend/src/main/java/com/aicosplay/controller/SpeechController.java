package com.aicosplay.controller;

import com.aicosplay.service.SpeechService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

/**
 * 语音识别控制器
 * 处理语音转文字的API请求
 */
@RestController
@RequestMapping("/api/speech")
public class SpeechController {
    
    @Autowired
    private SpeechService speechService;
    
    /**
     * 语音识别API
     * 接收音频文件并返回识别后的文本
     */
    @PostMapping("/recognize")
    public ResponseEntity<?> recognizeSpeech(
            @RequestParam("audio") MultipartFile audioFile,
            HttpSession session) {
        try {
            // 验证用户是否登录
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            // 验证文件是否为空
            if (audioFile.isEmpty()) {
                return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, "音频文件不能为空"));
            }
            
            // 验证文件大小（限制为10MB）
            if (audioFile.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, "音频文件大小不能超过10MB"));
            }
            
            // 调用语音识别服务进行识别
            String recognizedText = speechService.recognizeSpeech(audioFile);
            
            // 返回识别结果
            return ResponseEntity.ok(new SpeechResponse(true, "语音识别成功", recognizedText));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            String status = speechService.healthCheck();
            return ResponseEntity.ok(new AuthController.ApiResponse(true, status));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new AuthController.ApiResponse(false, "语音识别服务异常: " + e.getMessage()));
        }
    }
    
    // 语音识别响应DTO
    public static class SpeechResponse {
        private boolean success;
        private String message;
        private String text;
        
        public SpeechResponse(boolean success, String message, String text) {
            this.success = success;
            this.message = message;
            this.text = text;
        }
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}