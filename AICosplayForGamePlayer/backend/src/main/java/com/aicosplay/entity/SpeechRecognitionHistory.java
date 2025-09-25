package com.aicosplay.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "speech_recognition_history", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class SpeechRecognitionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", updatable = false, nullable = false, columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "character_info", length = 255)
    private String characterInfo;

    @Enumerated(EnumType.STRING)
    @Column(name = "recognition_type", nullable = false)
    private RecognitionType recognitionType;

    @Column(name = "recognized_text", columnDefinition = "TEXT")
    private String recognizedText;

    @Column(name = "audio_duration", columnDefinition = "INT DEFAULT 0")
    private Integer audioDuration = 0;

    @Column(name = "success", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean success = true;

    @Column(name = "error_message", length = 255)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum RecognitionType {
        OFFLINE,
        API
    }
}