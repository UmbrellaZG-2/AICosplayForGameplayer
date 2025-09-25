package com.aicosplay.controller;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.model.ApiResponse;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.service.ConversationService;
import com.aicosplay.service.UserService;
import com.aicosplay.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ConversationRepository conversationRepository;

    // 创建新对话
    @PostMapping
    public ResponseEntity<ApiResponse<Conversation>> createConversation(@RequestBody ConversationRequest request) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();

        Conversation conversation;
        if (request.getCharacterId() != null) {
            // 通过角色ID创建对话
            conversation = conversationService.createConversationByCharacterId(user, request.getTitle(), request.getCharacterId());
        } else {
            // 通过角色名称创建对话（保持向后兼容）
            conversation = conversationService.createConversation(user, request.getTitle(), request.getCharacterName());
        }
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    // 获取用户的所有对话
    @GetMapping
    public ResponseEntity<ApiResponse<List<Conversation>>> getUserConversations() {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();

        List<Conversation> conversations = conversationService.getUserConversations(user);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    // 获取单个对话
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Conversation>> getConversation(@PathVariable Long id) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        Conversation conversation = conversationService.getConversationById(id);
        
        // 验证对话是否属于当前用户
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "您没有权限访问此对话");
        }
        
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    // 添加消息到对话
    @PostMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<Message>> addMessage(@PathVariable Long id, @RequestBody MessageRequest request) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        // 验证对话是否属于当前用户
        Conversation conversation = conversationService.getConversationById(id);
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "您没有权限访问此对话");
        }
        
        // 调用更新后的方法，senderType始终为USER，因为AI回复是自动生成的
        Message message = conversationService.addMessageToConversation(
                id,  // conversationId
                request.getContent(),
                user
        );
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // 获取对话中的所有消息
    @GetMapping("/{id}/messages")
    public ResponseEntity<ApiResponse<List<Message>>> getConversationMessages(@PathVariable Long id) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        Conversation conversation = conversationService.getConversationById(id);
        
        // 验证对话是否属于当前用户
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "您没有权限访问此对话");
        }
        
        List<Message> messages = conversationService.getConversationMessages(conversation);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    // 删除对话
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteConversation(@PathVariable Long id) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        Conversation conversation = conversationService.getConversationById(id);
        
        // 验证对话是否属于当前用户
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "您没有权限删除此对话");
        }
        
        conversationService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success("对话删除成功"));
    }
    
    // 删除单条消息
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<?>> deleteMessage(@PathVariable Long messageId) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        conversationService.deleteMessage(messageId, user);
        return ResponseEntity.ok(ApiResponse.success("消息删除成功"));
    }
    
    // 恢复已删除的对话
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<?>> restoreConversation(@PathVariable Long id) {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();
        
        // 直接获取对话而不检查是否已删除
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("对话不存在"));
        
        // 验证对话是否属于当前用户
        if (!conversation.getUser().getId().equals(user.getId())) {
            throw new BusinessException("FORBIDDEN", "您没有权限恢复此对话");
        }
        
        conversationService.restoreConversation(id);
        return ResponseEntity.ok(ApiResponse.success("对话恢复成功"));
    }
    
    // 获取已删除的对话列表
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<List<Conversation>>> getDeletedConversations() {
        // 获取当前登录用户
        User user = UserContext.getCurrentUser();

        List<Conversation> conversations = conversationService.getDeletedConversations(user);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    // 请求DTO类
    public static class ConversationRequest {
        private String title;
        private String characterName;
        private Long characterId;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        public Long getCharacterId() { return characterId; }
        public void setCharacterId(Long characterId) { this.characterId = characterId; }
    }

    public static class MessageRequest {
        private Byte senderType; // 1-用户，2-AI
        private String content;

        // Getters and Setters
        public Byte getSenderType() { return senderType; }
        public void setSenderType(Byte senderType) { this.senderType = senderType; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}