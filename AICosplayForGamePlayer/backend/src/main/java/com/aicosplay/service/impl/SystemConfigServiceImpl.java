package com.aicosplay.service.impl;

import com.aicosplay.entity.SystemConfig;
import com.aicosplay.repository.SystemConfigRepository;
import com.aicosplay.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * 系统配置服务实现类
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final Logger logger = Logger.getLogger(SystemConfigServiceImpl.class.getName());

    private final SystemConfigRepository systemConfigRepository;

    @Autowired
    public SystemConfigServiceImpl(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Override
    public Map<String, String> getAllConfigs() {
        Map<String, String> configMap = new HashMap<>();
        systemConfigRepository.findAll().forEach(config -> {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        });
        return configMap;
    }

    @Override
    public String getConfigValue(String configKey) {
        Optional<SystemConfig> config = systemConfigRepository.findByConfigKey(configKey);
        return config.map(SystemConfig::getConfigValue).orElse(null);
    }

    @Override
    public Map<String, String> getConfigsByGroup(String configGroup) {
        Map<String, String> configMap = new HashMap<>();
        systemConfigRepository.findByConfigGroup(configGroup).forEach(config -> {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        });
        return configMap;
    }

    @Override
    @Transactional
    public boolean updateConfig(String configKey, String configValue) {
        Optional<SystemConfig> configOptional = systemConfigRepository.findByConfigKey(configKey);
        if (configOptional.isPresent()) {
            SystemConfig config = configOptional.get();
            config.setConfigValue(configValue);
            systemConfigRepository.save(config);
            return true;
        }
        return false;
    }

    @Override
    public void loadConfigsToEnvironment() {
        try {
            logger.info("开始加载系统配置到环境变量...");
            Map<String, String> configs = getAllConfigs();
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String configKey = entry.getKey();
                String configValue = entry.getValue();
                
                // 将配置键转换为环境变量格式（大写、点替换为下划线）
                String envVarName = convertToEnvVarName(configKey);
                
                // 设置环境变量
                System.setProperty(envVarName, configValue);
                logger.info("加载配置到环境变量: " + envVarName + " = " + maskSensitiveInfo(configKey, configValue));
            }
            logger.info("成功加载 " + configs.size() + " 个系统配置到环境变量");
        } catch (Exception e) {
            logger.severe("加载系统配置到环境变量失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 将配置键转换为环境变量格式
     * @param configKey 配置键
     * @return 环境变量名
     */
    private String convertToEnvVarName(String configKey) {
        return configKey.replace('.', '_').toUpperCase();
    }

    /**
     * 对敏感信息进行掩码处理
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 处理后的配置值
     */
    private String maskSensitiveInfo(String configKey, String configValue) {
        if (configKey.contains("api-key") || configKey.contains("api_secret") || configKey.contains("password")) {
            if (configValue.length() > 4) {
                return configValue.substring(0, 4) + "****";
            } else {
                return "****";
            }
        }
        return configValue;
    }
}