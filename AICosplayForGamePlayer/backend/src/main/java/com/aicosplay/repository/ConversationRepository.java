package com.aicosplay.repository;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /**
     * 根据用户查找未删除的对话，按更新时间降序排列
     * 添加索引后，此查询性能将显著提升
     */
    List<Conversation> findByUserAndIsDeletedOrderByUpdatedAtDesc(User user, Byte isDeleted);
    
    /**
     * 删除用户的所有对话
     */
    void deleteByUserId(Long userId);
    
    /**
     * 获取对话详情（包含最近N条消息）
     * 使用JPQL优化查询，避免N+1问题
     */
    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.messages m WHERE c.id = :id AND c.user = :user AND c.isDeleted = 0 ORDER BY m.createdAt ASC")
    Conversation findByIdAndUserWithMessages(@Param("id") Long id, @Param("user") User user);
    
    /**
     * 统计用户的对话数量
     */
    long countByUserAndIsDeleted(User user, Byte isDeleted);
}