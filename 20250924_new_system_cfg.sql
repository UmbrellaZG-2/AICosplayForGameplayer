-- 创建system_config表，用于存储系统配置项
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `config_key` VARCHAR(255) NOT NULL UNIQUE COMMENT '配置项的键名',
  `config_value` TEXT NOT NULL COMMENT '配置项的值',
  `config_group` VARCHAR(100) NOT NULL COMMENT '配置组（如xfyun或openai）',
  `description` TEXT COMMENT '配置项的描述',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入科大讯飞语音识别配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`)
VALUES
  ('xfyun.appid', 'your_appid', 'xfyun', '科大讯飞应用ID'),
  ('xfyun.api-key', 'your_api_key', 'xfyun', '科大讯飞API密钥'),
  ('xfyun.api-secret', 'your_api_secret', 'xfyun', '科大讯飞API密钥密钥'),
  ('xfyun.host-url', 'ws-api.xfyun.cn/v2/iat', 'xfyun', '科大讯飞API主机地址');

-- 插入OpenAI API配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_group`, `description`)
VALUES
  ('spring.ai.openai.api-key', 'Apikey', 'openai', 'OpenAI API密钥'),
  ('spring.ai.openai.chat.model', 'deepseek-V3', 'openai', 'OpenAI聊天模型'),
  ('spring.ai.openai.chat.temperature', '0.3', 'openai', 'OpenAI聊天温度参数');