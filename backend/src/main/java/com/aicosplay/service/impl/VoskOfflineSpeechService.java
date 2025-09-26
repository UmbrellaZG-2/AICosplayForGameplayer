package com.aicosplay.service.impl;

import com.aicosplay.service.SpeechRecognitionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.vosk.Model;
import org.vosk.Recognizer;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Vosk离线语音识别服务实现类
 * 封装Vosk离线语音识别功能的初始化、音频处理及结果返回等核心方法
 */
@Service
public class VoskOfflineSpeechService implements SpeechRecognitionService {

    private static final Logger logger = Logger.getLogger(VoskOfflineSpeechService.class.getName());

    // Vosk模型路径
    @Value("${vosk.model.path:../../vosk-model-small-cn-0.22/}")
    private String modelPath;

    // 采样率
    @Value("${vosk.sample.rate:16000}")
    private float sampleRate;

    // 模型对象
    private Model model;

    // 服务初始化状态
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * 初始化Vosk模型
     */
    @PostConstruct
    @Override
    public void initialize() {
        try {
            logger.info("开始初始化Vosk离线语音识别模型，模型路径: " + modelPath);
            model = new Model(modelPath);
            initialized.set(true);
            logger.info("Vosk离线语音识别模型初始化成功");
        } catch (IOException e) {
            logger.severe("Vosk离线语音识别模型初始化失败: " + e.getMessage());
            initialized.set(false);
        }
    }

    /**
     * 关闭服务资源
     */
    @PreDestroy
    @Override
    public void shutdown() {
        if (model != null) {
            try {
                model.close();
                logger.info("Vosk离线语音识别模型已关闭");
            } catch (Exception e) {
                logger.warning("关闭Vosk离线语音识别模型时发生错误: " + e.getMessage());
            }
        }
    }

    /**
     * 识别语音文件
     * @param audioFile 音频文件
     * @return 识别出的文本内容
     */
    @Override
    public String recognizeSpeech(MultipartFile audioFile) {
        if (!isAvailable()) {
            logger.warning("Vosk离线语音识别服务不可用，无法执行识别任务");
            throw new RuntimeException("Vosk离线语音识别服务不可用");
        }

        try {
            logger.info("开始处理离线语音识别请求");

            // 将上传的音频文件转换为WAV格式（Vosk需要的格式）
            File wavFile = convertToWav(audioFile);
            try {
                // 进行语音识别
                String recognizedText = recognizeFromWav(wavFile);
                logger.info("离线语音识别请求处理完成");
                return recognizedText;
            } finally {
                // 清理临时文件
                if (wavFile != null && wavFile.exists()) {
                    wavFile.delete();
                }
            }
        } catch (Exception e) {
            logger.severe("离线语音识别失败: " + e.getMessage());
            throw new RuntimeException("离线语音识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 健康检查
     * @return 服务状态信息
     */
    @Override
    public String healthCheck() {
        if (isAvailable()) {
            return "Vosk离线语音识别服务运行正常，模型路径: " + modelPath;
        } else {
            return "Vosk离线语音识别服务不可用，模型初始化失败或路径无效: " + modelPath;
        }
    }

    /**
     * 获取服务类型
     * @return 服务类型标识
     */
    @Override
    public String getServiceType() {
        return "offline";
    }

    /**
     * 检查服务是否可用
     * @return 服务是否可用的布尔值
     */
    @Override
    public boolean isAvailable() {
        return initialized.get() && model != null;
    }
    
    @Override
    public RecognitionType getRecognitionType() {
        return RecognitionType.OFFLINE;
    }

    /**
     * 将音频文件转换为WAV格式
     */
    private File convertToWav(MultipartFile audioFile) throws IOException, EncoderException {
        // 创建临时输入文件
        File tempInputFile = File.createTempFile("audio_input", ".tmp");
        audioFile.transferTo(tempInputFile);

        // 创建临时输出文件
        File tempOutputFile = File.createTempFile("audio_output", ".wav");

        try {
            // 设置音频转换参数
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setBitRate(16);
            audio.setChannels(1);
            audio.setSamplingRate((int) sampleRate);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);

            // 执行转换
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(tempInputFile), tempOutputFile, attrs);

            return tempOutputFile;
        } finally {
            // 清理输入临时文件
            if (tempInputFile.exists()) {
                tempInputFile.delete();
            }
        }
    }

    /**
     * 从WAV文件中识别语音
     */
    private String recognizeFromWav(File wavFile) throws IOException {
        StringBuilder resultBuilder = new StringBuilder();
        Recognizer recognizer = null;
        InputStream is = null;

        try {
            // 创建识别器
            recognizer = new Recognizer(model, sampleRate);
            is = new FileInputStream(wavFile);

            // 读取WAV文件头
            byte[] header = new byte[44];
            is.read(header);

            // 缓冲区大小
            int bufferSize = (int) (sampleRate * 0.1 * 2); // 100ms的音频数据
            ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize);
            buf.order(ByteOrder.LITTLE_ENDIAN);

            // 读取音频数据并进行识别
            byte[] bytes = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = is.read(bytes)) > 0) {
                // 使用正确的acceptWaveForm方法参数
                if (recognizer.acceptWaveForm(bytes, bytesRead)) {
                    String jsonResult = recognizer.getResult();
                    // 提取识别文本
                    String text = extractTextFromJson(jsonResult);
                    if (!text.isEmpty()) {
                        resultBuilder.append(text);
                    }
                }
            }

            // 获取最后剩余的识别结果
            String finalResult = recognizer.getFinalResult();
            String finalText = extractTextFromJson(finalResult);
            if (!finalText.isEmpty()) {
                resultBuilder.append(finalText);
            }

            String result = resultBuilder.toString().trim();
            if (result.isEmpty()) {
                throw new RuntimeException("未识别到有效文本");
            }
            return result;

        } finally {
            // 关闭资源
            if (recognizer != null) {
                recognizer.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * 从JSON结果中提取文本
     */
    private String extractTextFromJson(String jsonResult) {
        try {
            // 简单解析JSON，提取文本内容
            int textStartPos = jsonResult.indexOf("\"text\":");
            if (textStartPos > 0) {
                textStartPos += 8; // "text":" 的长度
                int textEndPos = jsonResult.indexOf('"', textStartPos);
                if (textEndPos > textStartPos) {
                    return jsonResult.substring(textStartPos, textEndPos);
                }
            }
            return "";
        } catch (Exception e) {
            logger.warning("解析识别结果JSON时发生错误: " + e.getMessage());
            return "";
        }
    }
}