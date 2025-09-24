package com.aicosplay.service;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {

    /**
     * 获取所有系统配置
     * @return 配置项映射
     */
    Map<String, String> getAllConfigs();

    /**
     * 根据配置键名获取配置值
     * @param configKey 配置键名
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 根据配置组获取配置项
     * @param configGroup 配置组
     * @return 配置项映射
     */
    Map<String, String> getConfigsByGroup(String configGroup);

    /**
     * 更新配置项
     * @param configKey 配置键名
     * @param configValue 配置值
     * @return 是否更新成功
     */
    boolean updateConfig(String configKey, String configValue);

    /**
     * 加载所有配置到环境变量
     */
    void loadConfigsToEnvironment();
}