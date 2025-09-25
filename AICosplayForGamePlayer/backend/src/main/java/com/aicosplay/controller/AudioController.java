package com.aicosplay.controller;

import com.aicosplay.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 音频文件控制器
 * 提供音频文件的下载服务
 */
@RestController
@RequestMapping("${audio.access.prefix}")
public class AudioController {

    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Value("${audio.storage.path}")
    private String audioStoragePath;

    /**
     * 提供音频文件下载服务
     * @param fileName 音频文件名
     * @return 音频文件资源
     */
    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> downloadAudioFile(@PathVariable String fileName) {
        try {
            // 验证文件名是否为空
            if (!StringUtils.hasText(fileName)) {
                throw new BusinessException("FILE_NAME_EMPTY", "文件名不能为空");
            }

            // 构建完整的文件路径
            Path filePath = Paths.get(audioStoragePath).resolve(fileName).normalize();
            File audioFile = filePath.toFile();

            // 检查文件是否存在且是一个普通文件
            if (!audioFile.exists() || !audioFile.isFile()) {
                throw new BusinessException("FILE_NOT_FOUND", "音频文件不存在: " + fileName);
            }

            // 创建文件系统资源
            Resource resource = new FileSystemResource(audioFile);

            // 确定媒体类型
            MediaType mediaType = MediaType.parseMediaType("audio/wav");
            if (fileName.toLowerCase().endsWith(".mp3")) {
                mediaType = MediaType.parseMediaType("audio/mpeg");
            }

            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + audioFile.getName() + "\"");

            logger.info("成功提供音频文件下载: {}", fileName);

            // 返回文件资源
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(mediaType)
                    .contentLength(audioFile.length())
                    .body(resource);
        } catch (BusinessException e) {
            logger.error("音频文件下载失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("音频文件下载异常: {}", e.getMessage(), e);
            throw new BusinessException("AUDIO_FILE_DOWNLOAD_ERROR", "音频文件下载失败: " + e.getMessage());
        }
    }
}