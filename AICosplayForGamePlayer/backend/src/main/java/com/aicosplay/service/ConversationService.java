package com.aicosplay.service;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.entity.GameCharacter;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.repository.MessageRepository;
import com.aicosplay.repository.GameCharacterRepository;
import com.aicosplay.service.impl.TextToSpeechService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConversationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private AIService aiService;
    
    @Autowired
    private GameCharacterRepository gameCharacterRepository;
    
    @Autowired
    private TextToSpeechService textToSpeechService;

    public Conversation createConversation(User user, String title, String characterName) {
        // 检查角色是否存在
        GameCharacter gameCharacter = gameCharacterRepository.findByName(characterName)
                .orElseThrow(() -> new BusinessException("CHARACTER_NOT_FOUND", "Character not found"));
                
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setCharacterName(characterName);
        return conversationRepository.save(conversation);
    }
    
    // 根据角色ID创建对话
    public Conversation createConversationByCharacterId(User user, String title, Long characterId) {
        // 检查角色是否存在
        GameCharacter gameCharacter = gameCharacterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException("CHARACTER_NOT_FOUND", "Character not found"));
                
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setCharacterName(gameCharacter.getName());
        return conversationRepository.save(conversation);
    }

    /**
     * 获取用户的所有未删除对话
     * 使用缓存提高性能
     */
    @Cacheable(value = "conversations", key = "#user.id")
    public List<Conversation> getUserConversations(User user) {
        logger.debug("Fetching conversations for user: {}", user.getId());
        // 获取未删除的对话
        return conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(user, (byte) 0);
    }

    /**
     * 根据ID获取对话详情
     * 使用缓存提高性能
     */
    @Cacheable(value = "conversationDetail", key = "#id")
    public Conversation getConversationById(Long id) {
        logger.debug("Fetching conversation by id: {}", id);
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found"));
                
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == (byte) 1) {
            throw new BusinessException("CONVERSATION_DELETED", "Conversation has been deleted");
        }
                
        return conversation;
    }
    
    /**
     * 根据ID获取对话详情并验证用户权限
     * 使用缓存提高性能
     */
    @Cacheable(value = "conversationDetail", key = "#id")
    public Conversation getConversationById(Long id, User user) {
        logger.debug("Fetching conversation by id: {} for user: {}", id, user.getId());
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found"));
                
        // 验证对话是否属于当前用户
        if (!conversation.getUser().getId().equals(user.getId())) {
            logger.warn("Unauthorized access attempt to conversation: {} by user: {}", id, user.getId());
            throw new BusinessException("FORBIDDEN", "您没有权限访问此对话");
        }
                
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == (byte) 1) {
            throw new BusinessException("CONVERSATION_DELETED", "Conversation has been deleted");
        }
                
        return conversation;
    }

    /**
     * 向对话添加消息并获取AI回复
     * 清理相关缓存以确保数据一致性
     */
    @Transactional
    @CacheEvict(value = {"conversations", "conversationDetail", "messages"}, allEntries = true)
    public Message addMessageToConversation(Long conversationId, String content, User sender) {
        logger.debug("Adding message to conversation: {} for user: {}", conversationId, sender.getId());
        
        // 使用优化的查询方法获取对话
        Conversation conversation = conversationRepository.findByIdAndUserWithMessages(conversationId, sender);
        if (conversation == null) {
            throw new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found or unauthorized access");
        }
        
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == (byte) 1) {
            throw new BusinessException("CONVERSATION_DELETED", "Cannot add message to deleted conversation");
        }
        
        // 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversation(conversation);
        userMessage.setContent(content);
        userMessage.setSenderType((byte) 1); // 1表示用户
        userMessage.setCreatedAt(java.time.LocalDateTime.now());
        messageRepository.save(userMessage);
        
        // 构建对话上下文（使用分页查询优化性能）
        // 只获取最近10条消息，避免加载过多数据
        List<Message> recentMessages = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, 10));
        String context = buildConversationContext(recentMessages, conversation.getCharacterName());
        
        try {
            // 获取角色信息
            GameCharacter character = gameCharacterRepository.findByName(conversation.getCharacterName())
                    .orElseThrow(() -> new BusinessException("CHARACTER_NOT_FOUND", "Character not found"));
            
            // 判断是否是首次对话
            boolean isFirstMessage = messageRepository.countByConversationId(conversationId) <= 1;
            
            logger.debug("Generating AI response for conversation: {}, isFirstMessage: {}", 
                        conversationId, isFirstMessage);
            
            // 调用AI服务生成回复，传入角色设定、用户信息和首次对话标志
            String aiResponse = aiService.generateResponseWithContext(
                    content, 
                    context, 
                    character.getPrompt(), 
                    sender.getUsername(), 
                    isFirstMessage
            );
            
            // 保存AI回复
            Message aiMessage = new Message();
            aiMessage.setConversation(conversation);
            aiMessage.setContent(aiResponse);
            aiMessage.setSenderType((byte) 2); // 2表示AI
            aiMessage.setCreatedAt(java.time.LocalDateTime.now());
            
            try {
                // 尝试将AI回复转换为语音
                Message messageWithVoice = textToSpeechService.convertTextToSpeech(aiMessage);
                messageRepository.save(messageWithVoice);
                logger.info("成功为AI回复生成语音");
            } catch (Exception e) {
                // 语音转换失败时，仍然保存文本消息，但记录错误
                logger.error("AI回复转语音失败，仅保存文本消息: {}", e.getMessage());
                messageRepository.save(aiMessage);
            }
        } catch (Exception e) {
            logger.error("Error processing message in conversation: {}", conversationId, e);
            throw new BusinessException("MESSAGE_PROCESSING_ERROR", "Failed to process message: " + e.getMessage());
        }
        
        logger.debug("Message added successfully to conversation: {}", conversationId);
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
                .map(m -> (m.getSenderType() == (byte) 1 ? "用户" : "AI") + ": " + m.getContent())
                .collect(Collectors.joining("\n"));
    }

    public List<Message> getConversationMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    /**
     * 逻辑删除对话并清理相关缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "conversationDetail", "messages"}, allEntries = true)
    public void deleteConversation(Long id, User user) {
        logger.debug("Deleting conversation: {} for user: {}", id, user.getId());
        
        // 使用优化的查询方法获取对话
        Conversation conversation = conversationRepository.findByIdAndUserWithMessages(id, user);
        if (conversation == null) {
            throw new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found or unauthorized access");
        }
        
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == (byte) 1) {
            logger.debug("Conversation already deleted: {}", id);
            return; // 已删除，无需重复操作
        }
        
        try {
            // 逻辑删除，将is_deleted设置为1
            conversation.setIsDeleted((byte) 1);
            conversationRepository.save(conversation);
            logger.debug("Conversation deleted successfully: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting conversation: {}", id, e);
            throw new BusinessException("CONVERSATION_DELETION_FAILED", "Failed to delete conversation: " + e.getMessage());
        }
    }

    /**
     * 恢复已删除的对话并清理相关缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "conversationDetail", "messages"}, allEntries = true)
    public void restoreConversation(Long id, User user) {
        logger.debug("Restoring conversation: {} for user: {}", id, user.getId());
        
        // 使用优化的查询方法获取对话
        Conversation conversation = conversationRepository.findByIdAndUserWithMessages(id, user);
        if (conversation == null) {
            throw new BusinessException("CONVERSATION_NOT_FOUND", "Conversation not found or unauthorized access");
        }
        
        // 检查对话是否已恢复
        if (conversation.getIsDeleted() == (byte) 0) {
            logger.debug("Conversation already restored: {}", id);
            return; // 已恢复，无需重复操作
        }
        
        try {
            // 恢复对话，将is_deleted设置为0
            conversation.setIsDeleted((byte) 0);
            conversationRepository.save(conversation);
            logger.debug("Conversation restored successfully: {}", id);
        } catch (Exception e) {
            logger.error("Error restoring conversation: {}", id, e);
            throw new BusinessException("CONVERSATION_RESTORE_FAILED", "Failed to restore conversation: " + e.getMessage());
        }
    }

    // 获取已删除的对话
    public List<Conversation> getDeletedConversations(User user) {
        return conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(user, (byte) 1);
    }
    
    /**
     * 删除单条消息
     * @param messageId 消息ID
     * @param user 当前登录用户
     */
    /**
     * 删除消息并清理相关缓存
     */
    @Transactional
    @CacheEvict(value = {"conversations", "conversationDetail", "messages"}, allEntries = true)
    public void deleteMessage(Long messageId, User user) {
        logger.debug("Deleting message: {} for user: {}", messageId, user.getId());
        
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException("MESSAGE_NOT_FOUND", "Message not found"));
        
        // 检查消息所属的对话是否属于当前用户
        Conversation conversation = message.getConversation();
        if (!conversation.getUser().getId().equals(user.getId())) {
            logger.warn("Unauthorized attempt to delete message: {} by user: {}", messageId, user.getId());
            throw new BusinessException("UNAUTHORIZED_ACCESS", "Unauthorized access to delete message");
        }
        
        // 检查对话是否已删除
        if (conversation.getIsDeleted() == (byte) 1) {
            throw new BusinessException("CONVERSATION_DELETED", "Cannot delete message from deleted conversation");
        }
        
        try {
            messageRepository.delete(message);
            logger.debug("Message deleted successfully: {}", messageId);
        } catch (Exception e) {
            logger.error("Error deleting message: {}", messageId, e);
            throw new BusinessException("MESSAGE_DELETION_FAILED", "Failed to delete message: " + e.getMessage());
        }
    }
}