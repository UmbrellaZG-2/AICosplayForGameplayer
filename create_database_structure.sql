-- 创建AICosplayForGamePlayer项目数据库表结构
-- 该脚本包含基础用户注册功能和历史对话保存功能所需的表结构

-- 注意：以下语句会清空数据库中的所有表数据，请谨慎执行

-- 使用数据库
USE aicosplay;

-- 禁用外键检查以便安全删除数据
SET FOREIGN_KEY_CHECKS = 0;

-- 使用存储过程来安全清空数据库中的所有表
DELIMITER $$
DROP PROCEDURE IF EXISTS clear_all_tables $$
CREATE PROCEDURE clear_all_tables()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE tableName VARCHAR(255);
    DECLARE cur CURSOR FOR SELECT table_name FROM information_schema.tables WHERE table_schema = 'aicosplay';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    -- 创建临时表存储表名
    DROP TEMPORARY TABLE IF EXISTS temp_tables;
    CREATE TEMPORARY TABLE temp_tables AS SELECT table_name FROM information_schema.tables WHERE table_schema = 'aicosplay';
    
    OPEN cur;
    
    read_loop:
    LOOP
        FETCH cur INTO tableName;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        -- 构建并执行TRUNCATE TABLE语句
        SET @sql = CONCAT('TRUNCATE TABLE ', tableName);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    
    CLOSE cur;
    DROP TEMPORARY TABLE IF EXISTS temp_tables;
END $$
DELIMITER ;

-- 调用存储过程清空所有表
CALL clear_all_tables();

-- 删除临时存储过程
DROP PROCEDURE IF EXISTS clear_all_tables;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS aicosplay 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

-- 再次使用数据库确保上下文正确
USE aicosplay;

-- 1. 用户表：存储用户注册信息
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
    `email` VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `status` TINYINT DEFAULT 1 COMMENT '状态（1-启用，0-禁用）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 对话表：存储对话信息
CREATE TABLE IF NOT EXISTS `conversation` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '对话ID',
    `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
    `title` VARCHAR(100) DEFAULT NULL COMMENT '对话标题',
    `character_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否已删除（0-未删除，1-已删除）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话表';

-- 3. 消息表：存储具体的消息内容
CREATE TABLE IF NOT EXISTS `message` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '所属对话ID',
    `sender_type` TINYINT NOT NULL COMMENT '发送者类型（1-用户，2-AI）',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 4. 游戏角色表：存储可选择的游戏角色信息
CREATE TABLE IF NOT EXISTS `game_character` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
    `name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    `prompt` TEXT DEFAULT NULL COMMENT '角色对话提示词',
    `is_preset` TINYINT DEFAULT 0 COMMENT '是否为预设角色（1-是，0-否）',
    `user_id` BIGINT DEFAULT NULL COMMENT '创建者用户ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏角色表';

-- 5. 用户角色关联表：存储用户与游戏角色的关联关系
CREATE TABLE IF NOT EXISTS `user_character` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '关联ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `character_id` BIGINT NOT NULL COMMENT '角色ID',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    
    -- 联合唯一索引
    UNIQUE KEY `uk_user_character` (`user_id`, `character_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';
CREATE INDEX `idx_user_username` ON `user`(`username`);
CREATE INDEX `idx_user_email` ON `user`(`email`);
CREATE INDEX `idx_conversation_user_id` ON `conversation`(`user_id`);
CREATE INDEX `idx_message_conversation_id` ON `message`(`conversation_id`);
CREATE INDEX `idx_game_character_name` ON `game_character`(`name`);

-- 显示创建成功的消息
SELECT '数据库表结构创建成功！已创建用户表、对话表、消息表、游戏角色表和用户角色关联表（无外键约束）。' AS '状态';

-- 显示所有创建的表
SHOW TABLES;