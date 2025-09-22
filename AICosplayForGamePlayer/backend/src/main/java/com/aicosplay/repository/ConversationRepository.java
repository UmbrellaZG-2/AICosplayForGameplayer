package com.aicosplay.repository;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByUserOrderByUpdatedAtDesc(User user);
    void deleteByUserId(Long userId);
}