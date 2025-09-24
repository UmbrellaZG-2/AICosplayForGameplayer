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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 语音识别服务实现类
 * 使用科大讯飞的HTTP API进行语音转文字
 */
@Service
public class SpeechServiceImpl implements SpeechService {
    
    @Value("${xfyun.appid}")
    private String appId;
    
    @Value("${xfyun.api-key}")
    private String apiKey;
    
    @Value("${xfyun.api-secret}")
    private String apiSecret;
    
    @Value("${xfyun.host-url}")
    private String hostUrl;
    
    @Override
    public String recognizeSpeech(MultipartFile audioFile) {
        try {
            // 读取音频文件内容
            byte[] audioBytes = audioFile.getBytes();
            
            // 发送音频数据到科大讯飞API并获取识别结果
            String recognizedText = sendAudioToXfyun(audioBytes);
            
            return recognizedText;
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
    
    /**
     * 发送音频数据到科大讯飞API并获取识别结果
     */
    private String sendAudioToXfyun(byte[] audioBytes) throws Exception {
        // 生成鉴权头
        Map<String, String> authHeaders = generateAuthHeaders();
        
        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("Authorization", authHeaders.get("Authorization"));
        headers.set("Date", authHeaders.get("Date"));
        
        // 创建请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        
        // 构建JSON参数
        Map<String, Object> jsonParams = buildJsonParams();
        String businessJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(jsonParams.get("business"));
        String commonJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(jsonParams.get("common"));
        String audioJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(jsonParams.get("audio"));
        
        // 添加音频文件和参数
        body.add("audio", new org.springframework.core.io.ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return "audio.webm";
            }
        });
        body.add("business", businessJson);
        body.add("common", commonJson);
        body.add("audio", audioJson);
        
        // 创建HTTP请求
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        // 发送请求到科大讯飞API
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "https://api.xfyun.cn/v2/iat",
                HttpMethod.POST,
                requestEntity,
                String.class
        );
        
        // 解析响应
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return parseXfyunResponse(response.getBody());
        } else {
            throw new RuntimeException("语音识别请求失败: " + response.getStatusCode());
        }
    }
    
    /**
     * 生成鉴权头
     */
    private Map<String, String> generateAuthHeaders() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        
        // 构建签名字符串
        String signatureOrigin = "host: api.xfyun.cn\ndate: " + date + "\nPOST /v2/iat HTTP/1.1";
        
        // 使用SHA-256算法生成签名
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        
        // 使用Base64编码
        String signature = Base64.getEncoder().encodeToString(hash);
        
        // 构建Authorization
        String authorizationOrigin = "api_key=" + apiKey + ", algorithm=" + "hmac-sha256" + ", headers=" + "host date request-line" + ", signature=" + signature;
        String authorization = Base64.getEncoder().encodeToString(authorizationOrigin.getBytes(StandardCharsets.UTF_8));
        
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", authorization);
        headers.put("Date", date);
        
        return headers;
    }
    
    /**
     * 构建JSON参数
     */
    private Map<String, Object> buildJsonParams() {
        Map<String, Object> params = new HashMap<>();
        Map<String, Object> common = new HashMap<>();
        Map<String, Object> business = new HashMap<>();
        Map<String, Object> audio = new HashMap<>();
        
        // 填充公共参数
        common.put("app_id", appId);
        
        // 填充业务参数
        business.put("language", "zh_cn");
        business.put("domain", "iat");
        business.put("accent", "mandarin");
        
        // 填充音频参数
        audio.put("format", "audio/webm");
        audio.put("encoding", "opus");
        audio.put("sample_rate", 16000);
        audio.put("channels", 1);
        
        // 组合参数
        params.put("common", common);
        params.put("business", business);
        params.put("audio", audio);
        
        return params;
    }
    
    /**
     * 解析科大讯飞API的响应
     */
    private String parseXfyunResponse(String responseBody) throws Exception {
        // 解析JSON响应
        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
        
        // 检查是否有错误
        if (rootNode.has("code") && !rootNode.get("code").asText().equals("0")) {
            String errorMsg = rootNode.get("message").asText();
            throw new RuntimeException("语音识别错误: " + errorMsg);
        }
        
        // 提取识别结果
        if (rootNode.has("data")) {
            com.fasterxml.jackson.databind.JsonNode dataNode = rootNode.get("data");
            if (dataNode.has("result")) {
                com.fasterxml.jackson.databind.JsonNode resultNode = dataNode.get("result");
                if (resultNode.has("text")) {
                    return resultNode.get("text").asText();
                }
            }
        }
        
        throw new RuntimeException("无法从响应中提取识别结果");
    }
}