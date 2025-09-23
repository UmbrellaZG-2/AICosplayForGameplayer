package com.aicosplay.service;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.entity.GameCharacter;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.repository.MessageRepository;
import com.aicosplay.repository.GameCharacterRepository;
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
    
    @Autowired
    private GameCharacterRepository gameCharacterRepository;

    public Conversation createConversation(User user, String title, String characterName) {
        // 检查角色是否存在
        GameCharacter gameCharacter = gameCharacterRepository.findByName(characterName)
                .orElseThrow(() -> new RuntimeException("Character not found"));
                
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setCharacterName(characterName);
        return conversationRepository.save(conversation);
    }

    public List<Conversation> getUserConversations(User user) {
        // 获取未删除的对话
        return conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(user, 0);
    }

    public Conversation getConversationById(Long id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
                
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == 1) {
            throw new RuntimeException("Conversation has been deleted");
        }
                
        return conversation;
    }

    @Transactional
    public Message addMessageToConversation(Long conversationId, String content, User sender) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        // 检查用户是否有权限访问此对话
        if (!conversation.getUser().getId().equals(sender.getId())) {
            throw new RuntimeException("Unauthorized access to conversation");
        }
        
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == 1) {
            throw new RuntimeException("Cannot add message to deleted conversation");
        }
        
        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setContent(content);
        userMessage.setSenderType(1); // 1表示用户
        userMessage.setCreatedAt(java.time.LocalDateTime.now());
        messageRepository.save(userMessage);
        
        // 构建对话上下文（最近10条消息）
        List<Message> recentMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId);
        String context = buildConversationContext(recentMessages, conversation.getCharacterName());
        
        try {
            // 调用AI服务生成回复
            String aiResponse = aiService.generateResponseWithContext(content, context);
            
            // 保存AI回复
            Message aiMessage = new Message();
            aiMessage.setConversation(conversation);
            aiMessage.setContent(aiResponse);
            aiMessage.setSenderType(2); // 2表示AI
            aiMessage.setCreatedAt(java.time.LocalDateTime.now());
            messageRepository.save(aiMessage);
        } catch (SecurityException e) {
            // 处理安全检查失败的情况
            Message safetyMessage = new Message();
            safetyMessage.setConversation(conversation);
            safetyMessage.setContent("我无法为这个问题提供相应解答。你可以尝试提供其他话题，我会尽力为你提供支持和解答。");
            safetyMessage.setSenderType(2); // 2表示AI
            safetyMessage.setCreatedAt(java.time.LocalDateTime.now());
            messageRepository.save(safetyMessage);
        }
        
        // 返回用户消息
        return userMessage;
    }
    
    /**
     * 构建对话上下文字符串
     * @param messages 消息列表
     * @param characterName 角色名称
     * @return 格式化的上下文字符串
     */
    private String buildConversationContext(List<Message> messages, String characterName) {
        // 限制上下文大小，只取最近10条消息
        int maxMessages = Math.min(10, messages.size());
        
        // 按照时间顺序升序排列并格式化
        return messages.stream()
                .limit(maxMessages)
                .sorted((m1, m2) -> m1.getCreatedAt().compareTo(m2.getCreatedAt()))
                .map(m -> (m.getSenderType() == 1 ? "用户" : "AI") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    public List<Message> getConversationMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional
    public void deleteConversation(Long id) {
        // 软删除对话
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
                
        conversation.setIsDeleted(1);
        conversationRepository.save(conversation);
    }

    // 恢复已删除的对话
    @Transactional
    public void restoreConversation(Long id) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
                
        conversation.setIsDeleted(0);
        conversationRepository.save(conversation);
    }

    // 获取已删除的对话
    public List<Conversation> getDeletedConversations(User user) {
        return conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(user, 1);
    }
}