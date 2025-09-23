package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

/**
 * 语音识别服务实现类
 * 使用OpenAI的Whisper API进行语音转文字
 */
@Service
public class SpeechServiceImpl implements SpeechService {
    
    @Value("${spring.ai.openai.api-key}")
    private String openAiApiKey;
    
    private final RestTemplate restTemplate;
    
    public SpeechServiceImpl() {
        // 初始化RestTemplate用于HTTP请求
        this.restTemplate = new RestTemplate();
    }
    
    @Override
    public String recognizeSpeech(MultipartFile audioFile) {
        try {
            // 读取音频文件内容
            byte[] audioBytes = audioFile.getBytes();
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Bearer " + openAiApiKey);
            
            // 创建请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return "audio.webm";
                }
            });
            body.add("model", "whisper-1");
            
            // 创建HTTP请求
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            
            // 发送请求到OpenAI的Whisper API
            ResponseEntity<WhisperResponse> response = restTemplate.exchange(
                    "https://api.openai.com/v1/audio/transcriptions",
                    HttpMethod.POST,
                    requestEntity,
                    WhisperResponse.class
            );
            
            // 返回识别结果
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getText();
            } else {
                throw new RuntimeException("语音识别请求失败: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("处理音频文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String healthCheck() {
        return "语音识别服务运行正常";
    }
    
    // Whisper API响应DTO
    private static class WhisperResponse {
        private String text;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
}