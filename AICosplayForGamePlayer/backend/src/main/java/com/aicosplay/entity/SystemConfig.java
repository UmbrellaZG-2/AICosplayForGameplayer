package com.aicosplay.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 系统配置实体类，映射system_config表
 */
@Entity
@Table(name = "system_config")
public class SystemConfig {
    
    /**
     * 无参构造函数
     */
    public SystemConfig() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_key", nullable = false, unique = true, length = 255)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(name = "config_group", nullable = false, length = 100)
    private String configGroup;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
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
    
    /**
     * 获取ID
     * @return ID
     */
    public Long getId() {
        return id;
    }
    
    /**
     * 设置ID
     * @param id ID
     */
    public void setId(Long id) {
        this.id = id;
    }
    
    /**
     * 获取配置键
     * @return 配置键
     */
    public String getConfigKey() {
        return configKey;
    }
    
    /**
     * 设置配置键
     * @param configKey 配置键
     */
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }
    
    /**
     * 获取配置值
     * @return 配置值
     */
    public String getConfigValue() {
        return configValue;
    }
    
    /**
     * 设置配置值
     * @param configValue 配置值
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
    
    /**
     * 获取配置组
     * @return 配置组
     */
    public String getConfigGroup() {
        return configGroup;
    }
    
    /**
     * 设置配置组
     * @param configGroup 配置组
     */
    public void setConfigGroup(String configGroup) {
        this.configGroup = configGroup;
    }
    
    /**
     * 获取描述
     * @return 描述
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 设置描述
     * @param description 描述
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * 获取创建时间
     * @return 创建时间
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * 设置创建时间
     * @param createdAt 创建时间
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * 获取更新时间
     * @return 更新时间
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * 设置更新时间
     * @param updatedAt 更新时间
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}