package com.aicosplay.repository;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据对话获取所有消息，按创建时间升序排列
     */
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    
    /**
     * 根据对话ID获取最新的消息，按创建时间降序排列
     */
    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<Message> findTopByConversationIdOrderByCreatedAtDesc(@Param("conversationId") Long conversationId, Pageable pageable);
    
    /**
     * 统计对话中的消息数量
     */
    long countByConversationId(Long conversationId);
    
    /**
     * 删除对话中的所有消息
     */
    void deleteByConversationId(Long conversationId);
    
    /**
     * 根据ID和对话ID获取消息，确保数据隔离
     */
    Optional<Message> findByIdAndConversationId(Long id, Long conversationId);
}