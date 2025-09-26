package com.aicosplay.config;

import com.aicosplay.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 配置加载器，在Spring Boot应用启动时加载数据库配置到环境变量
 */
@Component
public class ConfigLoader implements ApplicationListener<ApplicationPreparedEvent> {

    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    
    private final SystemConfigService systemConfigService;

    @Autowired
    public ConfigLoader(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        try {
            logger.info("应用启动前加载系统配置...");
            
            // 获取可配置的环境
            ConfigurableEnvironment environment = event.getApplicationContext().getEnvironment();
            
            // 从数据库加载配置
            systemConfigService.loadConfigsToEnvironment();
            
            // 也可以直接添加到Spring的环境中
            Map<String, Object> props = new HashMap<>();
            Map<String, String> configs = systemConfigService.getAllConfigs();
            
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                props.put(key, value);
            }
            
            // 将配置添加到Spring环境中
            // 注意：这部分可能需要调整，取决于Spring Boot版本和具体需求
            
            logger.info("系统配置加载完成");
        } catch (Exception e) {
            logger.severe("加载系统配置失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}