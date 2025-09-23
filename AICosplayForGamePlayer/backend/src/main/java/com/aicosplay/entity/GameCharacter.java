package com.aicosplay.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "game_character")
public class GameCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Transient
    private String avatar; // 虚拟字段，不存储到数据库，运行时根据name生成路径

    // 获取头像路径
    public String getAvatar() {
        if (name == null) {
            return null;
        }
        // 构建头像路径，支持jpg和png格式
        // 前端应根据实际文件存在情况选择正确的后缀
        String basePath = "/image/Character/";
        return basePath + name;
    }

    // 不需要setter，因为avatar是根据name自动生成的

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}