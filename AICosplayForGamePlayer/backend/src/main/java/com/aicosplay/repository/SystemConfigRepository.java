package com.aicosplay.repository;

import com.aicosplay.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问接口
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * 根据配置键名查找配置项
     * @param configKey 配置键名
     * @return 配置项
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据配置组查找配置项
     * @param configGroup 配置组
     * @return 配置项列表
     */
    List<SystemConfig> findByConfigGroup(String configGroup);
}