package com.aicosplay.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "sender_type", nullable = false)
    private Byte senderType; // 1-用户，2-AI

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    // 添加语音消息相关字段
    @Column(name = "is_voice_message", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    private Byte isVoiceMessage = 0; // 0-非语音消息，1-语音消息
    
    @Column(name = "voice_file_path")
    private String voiceFilePath; // 语音文件路径
    
    @Column(name = "voice_duration")
    private Integer voiceDuration; // 语音时长（秒）
    
    // AI语音消息特有字段
    @Column(name = "show_text_button", nullable = false, columnDefinition = "TINYINT DEFAULT 1")
    private Byte showTextButton = 1; // 1-显示文字按钮，0-不显示

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Byte getSenderType() {
        return senderType;
    }

    public void setSenderType(Byte senderType) {
        this.senderType = senderType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Byte getIsVoiceMessage() {
        return isVoiceMessage;
    }
    
    public void setIsVoiceMessage(Byte isVoiceMessage) {
        this.isVoiceMessage = isVoiceMessage;
    }
    
    public String getVoiceFilePath() {
        return voiceFilePath;
    }
    
    public void setVoiceFilePath(String voiceFilePath) {
        this.voiceFilePath = voiceFilePath;
    }
    
    public Integer getVoiceDuration() {
        return voiceDuration;
    }
    
    public void setVoiceDuration(Integer voiceDuration) {
        this.voiceDuration = voiceDuration;
    }
    
    public Byte getShowTextButton() {
        return showTextButton;
    }
    
    public void setShowTextButton(Byte showTextButton) {
        this.showTextButton = showTextButton;
    }
}