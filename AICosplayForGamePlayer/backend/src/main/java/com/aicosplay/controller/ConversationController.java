package com.aicosplay.controller;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.service.ConversationService;
import com.aicosplay.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private UserService userService;

    // 创建新对话
    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody ConversationRequest request, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Conversation conversation;
            if (request.getCharacterId() != null) {
                // 通过角色ID创建对话
                conversation = conversationService.createConversationByCharacterId(user, request.getTitle(), request.getCharacterId());
            } else {
                // 通过角色名称创建对话（保持向后兼容）
                conversation = conversationService.createConversation(user, request.getTitle(), request.getCharacterName());
            }
            return ResponseEntity.ok(conversation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 获取用户的所有对话
    @GetMapping
    public ResponseEntity<?> getUserConversations(HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Conversation> conversations = conversationService.getUserConversations(user);
            return ResponseEntity.ok(conversations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 获取单个对话
    @GetMapping("/{id}")
    public ResponseEntity<?> getConversation(@PathVariable Long id, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Conversation conversation = conversationService.getConversationById(id);
            
            // 验证对话是否属于当前用户
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(new AuthController.ApiResponse(false, "Access denied"));
            }
            
            return ResponseEntity.ok(conversation);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 添加消息到对话
    @PostMapping("/{id}/messages")
    public ResponseEntity<?> addMessage(@PathVariable Long id, @RequestBody MessageRequest request, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // 验证对话是否属于当前用户
            Conversation conversation = conversationService.getConversationById(id);
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(new AuthController.ApiResponse(false, "Access denied"));
            }
            
            // 调用更新后的方法，senderType始终为USER，因为AI回复是自动生成的
            Message message = conversationService.addMessageToConversation(
                    id,  // conversationId
                    request.getContent(),
                    user
            );
            return ResponseEntity.ok(message);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 获取对话中的所有消息
    @GetMapping("/{id}/messages")
    public ResponseEntity<?> getConversationMessages(@PathVariable Long id, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Conversation conversation = conversationService.getConversationById(id);
            
            // 验证对话是否属于当前用户
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(new AuthController.ApiResponse(false, "Access denied"));
            }
            
            List<Message> messages = conversationService.getConversationMessages(conversation);
            return ResponseEntity.ok(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    // 删除对话
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Conversation conversation = conversationService.getConversationById(id);
            
            // 验证对话是否属于当前用户
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(new AuthController.ApiResponse(false, "Access denied"));
            }
            
            conversationService.deleteConversation(id);
            return ResponseEntity.ok(new AuthController.ApiResponse(true, "Conversation deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }
    
    // 恢复已删除的对话
    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreConversation(@PathVariable Long id, HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // 直接获取对话而不检查是否已删除
            Conversation conversation = conversationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));
            
            // 验证对话是否属于当前用户
            if (!conversation.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(new AuthController.ApiResponse(false, "Access denied"));
            }
            
            conversationService.restoreConversation(id);
            return ResponseEntity.ok(new AuthController.ApiResponse(true, "Conversation restored successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }
    
    // 获取已删除的对话列表
    @GetMapping("/deleted")
    public ResponseEntity<?> getDeletedConversations(HttpSession session) {
        try {
            // 从会话中获取当前登录用户的用户名
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(new AuthController.ApiResponse(false, "User not authenticated"));
            }
            
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Conversation> conversations = conversationService.getDeletedConversations(user);
            return ResponseEntity.ok(conversations);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new AuthController.ApiResponse(false, e.getMessage()));
        }
    }

    @Autowired
    private ConversationRepository conversationRepository;

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