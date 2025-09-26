package com.aicosplay;

import com.aicosplay.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.aicosplay.entity")
@EnableJpaRepositories(basePackages = "com.aicosplay.repository")
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * 在应用启动时加载系统配置
     */
    @Bean
    public CommandLineRunner loadSystemConfig(@Autowired SystemConfigService systemConfigService) {
        return args -> {
            // 加载配置到环境变量
            systemConfigService.loadConfigsToEnvironment();
        };
    }
}