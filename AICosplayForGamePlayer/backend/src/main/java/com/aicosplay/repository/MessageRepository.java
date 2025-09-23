package com.aicosplay.repository;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    List<Message> findByConversationIdOrderByCreatedAtDesc(Long conversationId);
    void deleteByConversationId(Long conversationId);
}