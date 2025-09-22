package com.aicosplay.service;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    public Conversation createConversation(User user, String title) {
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setConversationTitle(title);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    public Conversation getConversationById(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }

    public Message addMessageToConversation(Conversation conversation, String senderType, String content) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderType(senderType);
        message.setContent(content);
        conversation.getMessages().add(message);
        conversationRepository.save(conversation);
        return messageRepository.save(message);
    }

    public List<Message> getConversationMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional
    public void deleteConversation(Long id) {
        messageRepository.deleteByConversationId(id);
        conversationRepository.deleteById(id);
    }
}