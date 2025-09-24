package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechRecognitionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 语音识别服务实现类（在线）
 * 负责处理语音识别的核心逻辑
 */
@Service
public class SpeechServiceImpl implements SpeechRecognitionService {
    
    private static final Logger logger = Logger.getLogger(SpeechServiceImpl.class.getName());
    
    // 配置属性，在initialize方法中从环境变量手动加载
    private String appId = "default_appid";
    private String apiKey = "default_api_key";
    private String apiSecret = "default_api_secret";
    private String hostUrl = "wss://iat-api.xfyun.cn/v2/iat";
    
    // 定义音频状态常量
    private static final int STATUS_FIRST_FRAME = 0; // 第一帧
    private static final int STATUS_CONTINUE_FRAME = 1; // 中间帧
    private static final int STATUS_LAST_FRAME = 2; // 最后一帧
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public String recognizeSpeech(MultipartFile audioFile) {
        try {
            logger.info("开始处理语音识别请求");
            
            // 读取音频文件内容
            byte[] audioBytes = audioFile.getBytes();
            
            // 发送音频数据到科大讯飞API并获取识别结果
            String recognizedText = sendAudioToXfyun(audioBytes);
            
            logger.info("语音识别请求处理完成");
            return recognizedText;
        } catch (IOException e) {
            logger.severe("处理音频文件失败: " + e.getMessage());
            throw new RuntimeException("处理音频文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.severe("语音识别失败: " + e.getMessage());
            throw new RuntimeException("语音识别失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String healthCheck() {
        return "在线语音识别服务运行正常，AppID: " + appId;
    }

    @Override
    public String getServiceType() {
        return "online";
    }

    @Override
    public boolean isAvailable() {
        // 简单检查必要配置是否存在
        return appId != null && !appId.isEmpty() && 
               apiKey != null && !apiKey.isEmpty() && 
               apiSecret != null && !apiSecret.isEmpty() && 
               hostUrl != null && !hostUrl.isEmpty();
    }

    @Override
    @PostConstruct
    public void initialize() {
        logger.info("初始化在线语音识别服务");
        
        try {
            // 从环境变量手动加载配置（注意：配置可能尚未从数据库加载，这里可能获取到默认值）
            appId = System.getProperty("XFYUN_APPID", appId);
            apiKey = System.getProperty("XFYUN_API_KEY", apiKey);
            apiSecret = System.getProperty("XFYUN_API_SECRET", apiSecret);
            hostUrl = System.getProperty("XFYUN_HOST_URL", hostUrl);
            
            logger.info("从环境变量加载配置: AppID=" + appId);
            
            // 验证配置是否完整
            if (!isAvailable()) {
                logger.warning("在线语音识别服务配置不完整，某些功能可能受限。服务将继续初始化，但在使用时会通过isAvailable()检查");
            } else {
                logger.info("在线语音识别服务初始化完成，配置有效");
            }
        } catch (Exception e) {
            logger.severe("初始化在线语音识别服务时发生异常: " + e.getMessage());
            // 即使初始化异常，也不抛出异常，避免影响Spring容器初始化
        }
    }

    @Override
    @PreDestroy
    public void shutdown() {
        logger.info("关闭在线语音识别服务");
        // 在线服务不需要特殊的关闭过程
    }
    
    /**
     * 发送音频数据到科大讯飞API并获取识别结果
     */
    private String sendAudioToXfyun(byte[] audioBytes) throws Exception {
        // 生成鉴权URL
        String authUrl = generateAuthUrl();
        
        // 将HTTP URL转换为WebSocket URL
        String wsUrl = authUrl.replace("http://", "ws://").replace("https://", "wss://");
        logger.info("WebSocket URL: " + wsUrl);
        
        // 创建结果容器和同步工具
        StringBuilder resultBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        // 使用数组来包装异常，这样可以在内部类中修改
        Exception[] errorHolder = new Exception[1];
        
        // 创建WebSocket客户端
        WebSocketClient client = new WebSocketClient(new URI(wsUrl)) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                logger.info("WebSocket连接已打开");
                try {
                    // 发送音频数据
                    sendAudioData(this, audioBytes);
                } catch (Exception e) {
                    logger.severe("发送音频数据失败: " + e.getMessage());
                    errorHolder[0] = e;
                    latch.countDown();
                }
            }
            
            @Override
            public void onMessage(String message) {
                try {
                    // 处理响应消息
                    boolean isFinal = processResponseMessage(message, resultBuilder);
                    if (isFinal) {
                        latch.countDown();
                    }
                } catch (Exception e) {
                    logger.severe("处理响应消息失败: " + e.getMessage());
                    errorHolder[0] = e;
                    latch.countDown();
                }
            }
            
            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.info("WebSocket连接已关闭: " + reason + " (code: " + code + ")");
                try {
                    if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                        latch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    latch.countDown();
                }
            }
            
            @Override
            public void onError(Exception ex) {
                logger.severe("WebSocket连接错误: " + ex.getMessage());
                errorHolder[0] = ex;
                try {
                    if (!latch.await(500, TimeUnit.MILLISECONDS)) {
                        latch.countDown();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    latch.countDown();
                }
            }
        };
        
        // 连接WebSocket
        client.connect();
        
        // 等待识别完成或超时
        boolean awaitResult = latch.await(60, TimeUnit.SECONDS);
        
        // 关闭连接
        client.close();
        
        // 检查是否有错误
        if (errorHolder[0] != null) {
            throw errorHolder[0];
        }
        
        // 检查是否超时
        if (!awaitResult) {
            throw new RuntimeException("语音识别超时");
        }
        
        // 检查识别结果是否为空
        String result = resultBuilder.toString().trim();
        if (result.isEmpty()) {
            throw new RuntimeException("未识别到有效文本");
        }
        
        return result;
    }
    
    /**
     * 生成鉴权URL
     */
    private String generateAuthUrl() throws Exception {
        // 获取当前时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        
        // 构建签名字符串
        String signature_origin = "host: " + getHost(hostUrl) + "\n";
        signature_origin += "date: " + date + "\n";
        signature_origin += "GET " + getPath(hostUrl) + " HTTP/1.1";
        
        // 计算HMAC-SHA256签名
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(spec);
        byte[] digest = mac.doFinal(signature_origin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(digest);
        
        // 构建Authorization头
        String authorization_origin = "api_key=" + apiKey + ", algorithm=hmac-sha256, headers=host date request-line, signature=" + signature;
        String authorization = Base64.getEncoder().encodeToString(authorization_origin.getBytes(StandardCharsets.UTF_8));
        
        // 构建鉴权URL
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://").append(getHost(hostUrl)).append(getPath(hostUrl));
        urlBuilder.append("?authorization=").append(URLEncoder.encode(authorization, StandardCharsets.UTF_8.toString()));
        urlBuilder.append("&date=").append(URLEncoder.encode(date, StandardCharsets.UTF_8.toString()));
        urlBuilder.append("&host=").append(getHost(hostUrl));
        
        return urlBuilder.toString();
    }
    
    /**
     * 从URL中提取主机名
     */
    private String getHost(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            throw new RuntimeException("解析URL失败: " + url, e);
        }
    }
    
    /**
     * 从URL中提取路径
     */
    private String getPath(String url) {
        URI uri = null;
        try {
            uri = new URI(url);
            return uri.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException("解析URL失败: " + url, e);
        }
    }
    
    /**
     * 发送音频数据
     */
    private void sendAudioData(WebSocketClient client, byte[] audioBytes) throws IOException {
        // 音频分片大小（10ms音频，16kHz采样率，16bit位深，单声道）
        int frameSize = 3200;
        
        // 发送第一帧
        int seq = 0;
        sendFrame(client, audioBytes, 0, Math.min(frameSize, audioBytes.length), seq++, STATUS_FIRST_FRAME);
        
        // 发送中间帧
        int offset = frameSize;
        while (offset < audioBytes.length - frameSize) {
            sendFrame(client, audioBytes, offset, frameSize, seq++, STATUS_CONTINUE_FRAME);
            offset += frameSize;
        }
        
        // 发送最后一帧
        sendFrame(client, audioBytes, offset, audioBytes.length - offset, seq, STATUS_LAST_FRAME);
    }
    
    /**
     * 发送单个音频帧
     */
    private void sendFrame(WebSocketClient client, byte[] audioBytes, int offset, int length, int seq, int status) throws IOException {
        // 截取音频数据
        byte[] frameData = Arrays.copyOfRange(audioBytes, offset, offset + length);
        
        // 构建发送数据
        String jsonData = buildFrameData(frameData, seq, status);
        
        // 发送数据
        client.send(jsonData);
        
        // 短暂休眠，控制发送速度
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 构建音频帧数据
     */
    private String buildFrameData(byte[] audioData, int seq, int status) throws IOException {
        Map<String, Object> data = new HashMap<>();
        
        // 构建header
        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);
        header.put("uid", "user" + System.currentTimeMillis());
        header.put("status", status);
        header.put("seq", seq);
        header.put("format", "raw");
        header.put("codec", "raw");
        header.put("sample_rate", 16000);
        header.put("channel", 1);
        header.put("language", "zh_cn");
        header.put("accent", "mandarin");
        data.put("header", header);
        
        // 构建parameter
        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> iat = new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        result.put("encoding", "utf8");
        result.put("compress", "raw");
        result.put("format", "json");
        iat.put("result", result);
        parameter.put("iat", iat);
        
        // 构建payload
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> audio = new HashMap<>();
        audio.put("encoding", "raw");
        audio.put("sample_rate", 16000);
        audio.put("channels", 1);
        audio.put("bit_depth", 16);
        audio.put("seq", seq);
        audio.put("status", status);
        
        // 只有非最后一帧才包含音频数据
        if (status != STATUS_LAST_FRAME) {
            audio.put("audio", Base64.getEncoder().encodeToString(audioData));
        } else {
            audio.put("audio", "");
        }
        
        payload.put("audio", audio);
        
        // 组合数据
        data.put("header", header);
        data.put("parameter", parameter);
        data.put("payload", payload);
        
        // 转换为JSON字符串
        return objectMapper.writeValueAsString(data);
    }
    
    /**
     * 处理响应消息
     */
    private boolean processResponseMessage(String message, StringBuilder resultBuilder) throws Exception {
        // 解析JSON响应
        JsonNode rootNode = objectMapper.readTree(message);
        
        // 检查是否有错误
        if (rootNode.has("header")) {
            JsonNode headerNode = rootNode.get("header");
            if (headerNode.has("code") && headerNode.get("code").asInt() != 0) {
                String errorMsg = headerNode.get("message").asText();
                logger.severe("语音识别错误: " + errorMsg + " (code=" + headerNode.get("code").asInt() + ")");
                throw new RuntimeException("语音识别错误: " + errorMsg + " (code=" + headerNode.get("code").asInt() + ")");
            }
        }
        
        // 处理识别结果
        if (rootNode.has("payload")) {
            JsonNode payloadNode = rootNode.get("payload");
            if (payloadNode.has("result")) {
                JsonNode resultNode = payloadNode.get("result");
                if (resultNode.has("text")) {
                    // 解码识别结果
                    String textBase64 = resultNode.get("text").asText();
                    byte[] decodedBytes = Base64.getDecoder().decode(textBase64);
                    String decodeRes = new String(decodedBytes, StandardCharsets.UTF_8);
                    
                    // 解析结果JSON
                    JsonNode textNode = objectMapper.readTree(decodeRes);
                    if (textNode.has("ws")) {
                        Iterator<JsonNode> wsIterator = textNode.get("ws").elements();
                        while (wsIterator.hasNext()) {
                            JsonNode wsNode = wsIterator.next();
                            if (wsNode.has("cw")) {
                                Iterator<JsonNode> cwIterator = wsNode.get("cw").elements();
                                while (cwIterator.hasNext()) {
                                    JsonNode cwNode = cwIterator.next();
                                    if (cwNode.has("w")) {
                                        resultBuilder.append(cwNode.get("w").asText());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 检查是否是最后一条消息
        if (rootNode.has("header") && rootNode.get("header").has("status") && rootNode.get("header").get("status").asInt() == 2) {
            return true; // 是最终结果
        }
        
        return false; // 不是最终结果
    }
}