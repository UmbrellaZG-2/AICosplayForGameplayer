package com.aicosplay.controller;

import com.aicosplay.controller.ConversationController.ConversationRequest;
import com.aicosplay.controller.ConversationController.MessageRequest;
import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.service.ConversationService;
import com.aicosplay.service.UserService;
import com.aicosplay.utils.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 加载完整应用上下文的测试类
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "testuser")
public class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConversationService conversationService;

    @MockBean
    private UserService userService;

    // 所有ConversationService依赖的组件都需要mock
    @MockBean
    private com.aicosplay.repository.ConversationRepository conversationRepository;
    
    @MockBean
    private com.aicosplay.repository.MessageRepository messageRepository;
    
    @MockBean
    private com.aicosplay.repository.GameCharacterRepository gameCharacterRepository;
    
    @MockBean
    private com.aicosplay.service.AIService aiService;
    
    @MockBean
    private com.aicosplay.service.impl.TextToSpeechService textToSpeechService;

    private User testUser;
    private Conversation testConversation;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setUser(testUser);
        testConversation.setTitle("与初音未来的对话");
        testConversation.setCharacterName("初音未来");
        testConversation.setIsDeleted((byte) 0);
        testConversation.setCreatedAt(LocalDateTime.now());
        testConversation.setUpdatedAt(LocalDateTime.now());

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setConversation(testConversation);
        testMessage.setContent("你好，初音未来！");
        testMessage.setSenderType((byte) 1); // 用户
        testMessage.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateConversation_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            ConversationRequest request = new ConversationRequest();
            request.setTitle("新对话");
            request.setCharacterName("初音未来");

            // 模拟依赖
            when(conversationService.createConversation(any(User.class), eq(request.getTitle()), eq(request.getCharacterName())))
                    .thenReturn(testConversation);

            // 执行测试
            mockMvc.perform(post("/api/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testConversation.getId()))
                    .andExpect(jsonPath("$.data.title").value(testConversation.getTitle()))
                    .andExpect(jsonPath("$.data.characterName").value(testConversation.getCharacterName()))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }
    }

    @Test
    void testCreateConversationByCharacterId_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            ConversationRequest request = new ConversationRequest();
            request.setTitle("新对话");
            request.setCharacterId(1L);

            // 模拟依赖
            when(conversationService.createConversationByCharacterId(any(User.class), eq(request.getTitle()), eq(request.getCharacterId())))
                    .thenReturn(testConversation);

            // 执行测试
            mockMvc.perform(post("/api/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(testConversation.getId()))
                    .andExpect(jsonPath("$.data.title").value(testConversation.getTitle()))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"));
        }
    }

    @Test
    void testCreateConversation_Failure() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            ConversationRequest request = new ConversationRequest();
            request.setTitle("新对话");
            request.setCharacterName("不存在的角色");

            // 模拟依赖 - 抛出异常
            when(conversationService.createConversation(any(User.class), eq(request.getTitle()), eq(request.getCharacterName())))
                    .thenThrow(new BusinessException("CHARACTER_NOT_FOUND", "角色不存在"));

            // 执行测试
            mockMvc.perform(post("/api/conversations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("角色不存在"));
        }
    }

    @Test
    void testGetUserConversations_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            List<Conversation> conversations = Arrays.asList(testConversation);

            // 模拟依赖
            when(conversationService.getUserConversations(any(User.class))).thenReturn(conversations);

            // 执行测试
            mockMvc.perform(get("/api/conversations")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.size()").value(conversations.size()))
                    .andExpect(jsonPath("$.data[0].id").value(testConversation.getId()))
                    .andExpect(jsonPath("$.data[0].title").value(testConversation.getTitle()));
        }
    }

    @Test
    void testGetConversation_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 1L;

            // 模拟依赖
            when(conversationService.getConversationById(conversationId, testUser)).thenReturn(testConversation);

            // 执行测试
            mockMvc.perform(get("/api/conversations/{id}", conversationId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.id").value(testConversation.getId()))
                    .andExpect(jsonPath("$.data.title").value(testConversation.getTitle()));
        }
    }

    @Test
    void testGetConversation_Failure() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 999L;

            // 模拟依赖 - 抛出异常
            when(conversationService.getConversationById(conversationId, testUser))
                    .thenThrow(new BusinessException("CONVERSATION_NOT_FOUND", "对话不存在"));

            // 执行测试
            mockMvc.perform(get("/api/conversations/{id}", conversationId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("对话不存在"));
        }
    }

    @Test
    void testSendMessage_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 1L;
            MessageRequest request = new MessageRequest();
            request.setContent("你好，初音未来！");

            // 模拟依赖
            when(conversationService.addMessageToConversation(conversationId, request.getContent(), testUser))
                    .thenReturn(testMessage);

            // 执行测试
            mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.id").value(testMessage.getId()))
                    .andExpect(jsonPath("$.data.content").value(testMessage.getContent()))
                    .andExpect(jsonPath("$.data.senderType").value(is((int) testMessage.getSenderType())));
        }
    }

    @Test
    void testSendMessage_Failure() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 999L;
            MessageRequest request = new MessageRequest();
            request.setContent("你好！");

            // 模拟依赖 - 抛出异常
            when(conversationService.addMessageToConversation(conversationId, request.getContent(), testUser))
                    .thenThrow(new BusinessException("CONVERSATION_NOT_FOUND", "对话不存在"));

            // 执行测试
            mockMvc.perform(post("/api/conversations/{id}/messages", conversationId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("对话不存在"));
        }
    }

    @Test
    void testDeleteConversation_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 1L;

            // 执行测试 - 模拟成功删除
            mockMvc.perform(delete("/api/conversations/{id}", conversationId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").value("对话删除成功"));
        }
    }

    @Test
    void testRestoreConversation_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 1L;

            // 执行测试 - 模拟成功恢复
            mockMvc.perform(put("/api/conversations/{id}/restore", conversationId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").value("对话恢复成功"));
        }
    }

    @Test
    void testGetDeletedConversations_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            testConversation.setIsDeleted((byte) 1); // 设置为已删除
            List<Conversation> deletedConversations = Arrays.asList(testConversation);

            // 模拟依赖
            when(conversationService.getDeletedConversations(any(User.class))).thenReturn(deletedConversations);

            // 执行测试
            mockMvc.perform(get("/api/conversations/deleted")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.size()").value(deletedConversations.size()))
                    .andExpect(jsonPath("$.data[0].id").value(testConversation.getId()))
                    .andExpect(jsonPath("$.data[0].isDeleted").value(1));
        }
    }

    @Test
    void testDeleteMessage_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long messageId = 1L;

            // 执行测试 - 模拟成功删除
            mockMvc.perform(delete("/api/conversations/messages/{id}", messageId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data").value("消息删除成功"));
        }
    }

    @Test
    void testDeleteMessage_Failure() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long messageId = 999L;

            // 模拟依赖 - 抛出异常
            Mockito.doThrow(new BusinessException("MESSAGE_NOT_FOUND", "消息不存在"))
                    .when(conversationService).deleteMessage(messageId, testUser);

            // 执行测试
            mockMvc.perform(delete("/api/conversations/messages/{id}", messageId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(1004))
                    .andExpect(jsonPath("$.message").value("消息不存在"));
        }
    }

    @Test
    void testGetConversationMessages_Success() throws Exception {
        // 使用Mockito模拟静态方法
        try (MockedStatic<UserContext> mockedUserContext = Mockito.mockStatic(UserContext.class)) {
            // 配置模拟行为
            mockedUserContext.when(UserContext::getCurrentUser).thenReturn(testUser);
            
            // 准备测试数据
            Long conversationId = 1L;
            List<Message> messages = Arrays.asList(testMessage);

            // 模拟依赖
            when(conversationService.getConversationById(conversationId, testUser)).thenReturn(testConversation);
            when(conversationService.getConversationMessages(testConversation)).thenReturn(messages);

            // 执行测试
            mockMvc.perform(get("/api/conversations/{id}/messages", conversationId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("操作成功"))
                    .andExpect(jsonPath("$.data.size()").value(messages.size()))
                    .andExpect(jsonPath("$.data[0].content").value(testMessage.getContent()));
        }
    }
}