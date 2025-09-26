package com.aicosplay.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI配置类，用于配置SpringDoc OpenAPI文档生成
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AICosplay API文档",
                description = "AI角色扮演游戏玩家平台的API接口文档",
                version = "1.0",
                contact = @Contact(
                        name = "AICosplay团队",
                        email = "support@aicosplay.com"
                ),
                license = @License(
                        name = "MIT许可证",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:8080",
                        description = "本地开发环境"
                )
        }
)
public class OpenApiConfig {

}