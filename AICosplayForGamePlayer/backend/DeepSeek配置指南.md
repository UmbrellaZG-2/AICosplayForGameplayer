## 配置步骤

1. 在MySQL数据库的`system_config`表中添加以下配置项：

```sql
INSERT INTO system_config (config_key, config_value, config_group, description, created_at, updated_at) VALUES 
('spring.ai.deepseek.api-key', 'YOUR_DEEPSEEK_API_KEY', 'ai', 'DeepSeek API密钥', NOW(), NOW()),
('spring.ai.deepseek.chat.model', 'deepseek-chat', 'ai', 'DeepSeek模型名称', NOW(), NOW()),
('spring.ai.deepseek.chat.temperature', '0.7', 'ai', 'DeepSeek温度参数', NOW(), NOW());
```

2. 将`YOUR_DEEPSEEK_API_KEY`替换为您的实际DeepSeek API密钥

## 切换总结

本项目已成功从本地Ollama大模型切换为DeepSeek大模型，主要修改包括：

1. 添加了DeepSeek相关依赖
2. 配置了DeepSeek API参数
3. 将AIClientConfig中的OllamaChatClient替换为DeepSeekChatClient
4. 保留了原有的AIService接口，确保业务逻辑无需修改

## 注意事项

- 请确保您的DeepSeek API密钥有效且具有足够的使用额度
- 系统启动时会自动从数据库加载配置到环境变量
- 如有配置问题，请检查application.properties和system_config表中的配置项是否正确