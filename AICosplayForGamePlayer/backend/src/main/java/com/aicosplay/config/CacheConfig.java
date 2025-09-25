package com.aicosplay.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置类
 * 用于优化对话上下文处理性能
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 使用ConcurrentMapCacheManager作为本地缓存实现
     * 后续可扩展为Redis等分布式缓存
     */
    @Bean
    public CacheManager cacheManager() {
        // 定义需要的缓存名称
        return new ConcurrentMapCacheManager(
                "conversations",    // 对话列表缓存
                "conversationDetail", // 对话详情缓存
                "messages"          // 消息列表缓存
        );
    }
}