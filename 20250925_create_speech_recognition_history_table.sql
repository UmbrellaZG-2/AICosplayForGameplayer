-- 创建语音转文字历史记录表
CREATE TABLE IF NOT EXISTS speech_recognition_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    user_id VARCHAR(50) NOT NULL COMMENT '用户标识',
    character_info VARCHAR(255) DEFAULT NULL COMMENT '角色信息',
    recognition_type ENUM('OFFLINE', 'API') NOT NULL COMMENT '语音转文字方式（离线或API）',
    recognized_text TEXT COMMENT '识别后的文本内容',
    audio_duration INT DEFAULT 0 COMMENT '音频时长（秒）',
    success BOOLEAN NOT NULL DEFAULT TRUE COMMENT '识别是否成功',
    error_message VARCHAR(255) DEFAULT NULL COMMENT '错误信息',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='语音转文字历史记录表';

-- 添加表注释
ALTER TABLE speech_recognition_history COMMENT = '存储用户的语音转文字历史记录信息';