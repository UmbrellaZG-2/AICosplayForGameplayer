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
import java.util.stream.Collectors;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private AIService aiService;

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

    @Transactional
    public Message addMessageToConversation(Long conversationId, String content, User sender) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // 检查用户是否有权限访问此对话
        if (!conversation.getUser().getId().equals(sender.getId())) {
            throw new RuntimeException("Unauthorized access to conversation");
        }
        
        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setContent(content);
        userMessage.setSenderType("USER");
        userMessage.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        messageRepository.save(userMessage);
        
        // 构建对话上下文（最近10条消息）
        List<Message> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
        String context = buildConversationContext(recentMessages);
        
        try {
            // 调用AI服务生成回复
            String aiResponse = aiService.generateResponseWithContext(content, context);
            
            // 保存AI回复
            Message aiMessage = new Message();
            aiMessage.setConversation(conversation);
            aiMessage.setContent(aiResponse);
            aiMessage.setSenderType("AI");
            aiMessage.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            messageRepository.save(aiMessage);
        } catch (SecurityException e) {
            // 处理安全检查失败的情况
            Message safetyMessage = new Message();
            safetyMessage.setConversation(conversation);
            safetyMessage.setContent("我无法为这个问题提供相应解答。你可以尝试提供其他话题，我会尽力为你提供支持和解答。");
            safetyMessage.setSenderType("AI");
            safetyMessage.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            messageRepository.save(safetyMessage);
        }
        
        // 返回用户消息
        return userMessage;
    }
    
    /**
     * 构建对话上下文字符串
     * @param messages 消息列表
     * @return 格式化的上下文字符串
     */
    private String buildConversationContext(List<Message> messages) {
        // 限制上下文大小，只取最近10条消息
        int maxMessages = Math.min(10, messages.size());
        
        // 按照时间顺序升序排列并格式化
        return messages.stream()
                .limit(maxMessages)
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(m -> m.getSenderType() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
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