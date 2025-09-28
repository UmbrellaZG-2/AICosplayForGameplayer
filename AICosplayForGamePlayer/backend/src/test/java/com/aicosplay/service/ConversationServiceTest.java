package com.aicosplay.service;

import com.aicosplay.entity.Conversation;
import com.aicosplay.entity.GameCharacter;
import com.aicosplay.entity.Message;
import com.aicosplay.entity.User;
import com.aicosplay.exception.BusinessException;
import com.aicosplay.repository.ConversationRepository;
import com.aicosplay.repository.GameCharacterRepository;
import com.aicosplay.repository.MessageRepository;
import com.aicosplay.service.ConversationService;
import com.aicosplay.service.AIService;
import com.aicosplay.service.impl.TextToSpeechService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConversationServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ConversationServiceTest.class);

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private GameCharacterRepository gameCharacterRepository;

    @Mock
    private AIService aiService;

    @Mock
    private TextToSpeechService textToSpeechService;

    @InjectMocks
    private ConversationService conversationService;

    private User testUser;
    private GameCharacter testCharacter;
    private Conversation testConversation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 初始化测试数据
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");

        testCharacter = new GameCharacter();
        testCharacter.setId(1L);
        testCharacter.setName("初音未来");
        testCharacter.setPrompt("你是初音未来，一个活泼可爱的虚拟歌手。");
        testCharacter.setDescription("虚拟歌手");

        testConversation = new Conversation();
        testConversation.setId(1L);
        testConversation.setUser(testUser);
        testConversation.setTitle("与初音未来的对话");
        testConversation.setCharacterName("初音未来");
        testConversation.setIsDeleted((byte) 0);
        testConversation.setCreatedAt(LocalDateTime.now());
        testConversation.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateConversation_Success() {
        // 准备测试数据
        String title = "新对话";
        String characterName = "初音未来";

        // 模拟依赖
        when(gameCharacterRepository.findByName(characterName)).thenReturn(Optional.of(testCharacter));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // 执行测试
        Conversation result = conversationService.createConversation(testUser, title, characterName);

        // 验证结果
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(characterName, result.getCharacterName());
        logger.info("创建对话测试通过");
    }

    @Test
    void testCreateConversation_CharacterNotFound() {
        // 准备测试数据
        String title = "新对话";
        String characterName = "不存在的角色";

        // 模拟依赖 - 角色不存在
        when(gameCharacterRepository.findByName(characterName)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            conversationService.createConversation(testUser, title, characterName);
        });

        // 验证异常信息
        assertEquals("CHARACTER_NOT_FOUND", exception.getErrorCode());
        logger.info("创建对话-角色不存在测试通过");
    }

    @Test
    void testCreateConversationByCharacterId_Success() {
        // 准备测试数据
        String title = "新对话";
        Long characterId = 1L;

        // 模拟依赖
        when(gameCharacterRepository.findById(characterId)).thenReturn(Optional.of(testCharacter));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // 执行测试
        Conversation result = conversationService.createConversationByCharacterId(testUser, title, characterId);

        // 验证结果
        assertNotNull(result);
        assertEquals(title, result.getTitle());
        assertEquals(testCharacter.getName(), result.getCharacterName());
        logger.info("通过角色ID创建对话测试通过");
    }

    @Test
    void testGetUserConversations_Success() {
        // 准备测试数据
        List<Conversation> expectedConversations = Arrays.asList(testConversation);

        // 模拟依赖
        when(conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(testUser, (byte) 0))
                .thenReturn(expectedConversations);

        // 执行测试
        List<Conversation> result = conversationService.getUserConversations(testUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedConversations.size(), result.size());
        logger.info("获取用户对话列表测试通过");
    }

    @Test
    void testGetConversationById_Success() {
        // 准备测试数据
        Long conversationId = 1L;

        // 模拟依赖
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(testConversation));

        // 执行测试
        Conversation result = conversationService.getConversationById(conversationId);

        // 验证结果
        assertNotNull(result);
        assertEquals(conversationId, result.getId());
        logger.info("根据ID获取对话测试通过");
    }

    @Test
    void testGetConversationById_NotFound() {
        // 准备测试数据
        Long conversationId = 999L;

        // 模拟依赖 - 对话不存在
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            conversationService.getConversationById(conversationId);
        });

        // 验证异常信息
        assertEquals("CONVERSATION_NOT_FOUND", exception.getErrorCode());
        logger.info("根据ID获取对话-对话不存在测试通过");
    }

    @Test
    void testAddMessageToConversation_Success() {
        // 准备测试数据
        Long conversationId = 1L;
        String content = "你好，初音未来！";
        String aiResponse = "你好呀！很高兴见到你，我是初音未来！";

        // 创建用户消息
        Message userMessage = new Message();
        userMessage.setId(1L);
        userMessage.setConversation(testConversation);
        userMessage.setContent(content);
        userMessage.setSenderType((byte) 1); // 用户
        userMessage.setCreatedAt(LocalDateTime.now());

        // 创建AI消息
        Message aiMessage = new Message();
        aiMessage.setId(2L);
        aiMessage.setConversation(testConversation);
        aiMessage.setContent(aiResponse);
        aiMessage.setSenderType((byte) 2); // AI
        aiMessage.setCreatedAt(LocalDateTime.now());

        // 模拟依赖
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(testConversation);
        when(messageRepository.save(any(Message.class))).thenReturn(userMessage, aiMessage);
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(new ArrayList<>()); // 首次对话，没有历史消息
        when(gameCharacterRepository.findByName(testConversation.getCharacterName())).thenReturn(Optional.of(testCharacter));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(1L); // 只有用户消息，是首次对话
        when(aiService.generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(aiResponse);
        when(textToSpeechService.convertTextToSpeech(any(Message.class))).thenReturn(aiMessage);

        // 执行测试
        Message result = conversationService.addMessageToConversation(conversationId, content, testUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(userMessage, result);
        verify(messageRepository, times(2)).save(any(Message.class)); // 保存用户消息和AI消息
        verify(aiService).generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), eq(true));
        logger.info("添加消息到对话并获取AI回复测试通过");
    }

    @Test
    void testAddMessageToConversation_WithHistory_Success() {
        // 准备测试数据
        Long conversationId = 1L;
        String content = "唱首歌吧！";
        String aiResponse = "好的，我来为你唱一首歌！";

        // 创建历史消息
        Message historyUserMessage = new Message();
        historyUserMessage.setContent("你好，初音未来！");
        historyUserMessage.setSenderType((byte) 1);
        historyUserMessage.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        Message historyAiMessage = new Message();
        historyAiMessage.setContent("你好呀！很高兴见到你，我是初音未来！");
        historyAiMessage.setSenderType((byte) 2);
        historyAiMessage.setCreatedAt(LocalDateTime.now().minusMinutes(4));

        List<Message> historyMessages = Arrays.asList(historyUserMessage, historyAiMessage);

        // 创建用户消息
        Message userMessage = new Message();
        userMessage.setId(3L);
        userMessage.setConversation(testConversation);
        userMessage.setContent(content);
        userMessage.setSenderType((byte) 1);
        userMessage.setCreatedAt(LocalDateTime.now());

        // 创建AI消息
        Message aiMessage = new Message();
        aiMessage.setId(4L);
        aiMessage.setConversation(testConversation);
        aiMessage.setContent(aiResponse);
        aiMessage.setSenderType((byte) 2);
        aiMessage.setCreatedAt(LocalDateTime.now());

        // 模拟依赖
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(testConversation);
        when(messageRepository.save(any(Message.class))).thenReturn(userMessage, aiMessage);
        when(messageRepository.findTopByConversationIdOrderByCreatedAtDesc(eq(conversationId), any(PageRequest.class)))
                .thenReturn(historyMessages);
        when(gameCharacterRepository.findByName(testConversation.getCharacterName())).thenReturn(Optional.of(testCharacter));
        when(messageRepository.countByConversationId(conversationId)).thenReturn(3L); // 不是首次对话
        when(aiService.generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(aiResponse);
        when(textToSpeechService.convertTextToSpeech(any(Message.class))).thenReturn(aiMessage);

        // 执行测试
        Message result = conversationService.addMessageToConversation(conversationId, content, testUser);

        // 验证结果
        assertNotNull(result);
        verify(aiService).generateResponseWithContext(anyString(), anyString(), anyString(), anyString(), eq(false));
        logger.info("带历史消息的对话测试通过");
    }

    @Test
    void testAddMessageToConversation_ConversationNotFound() {
        // 准备测试数据
        Long conversationId = 999L;
        String content = "你好！";

        // 模拟依赖 - 对话不存在
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(null);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            conversationService.addMessageToConversation(conversationId, content, testUser);
        });

        // 验证异常信息
        assertEquals("CONVERSATION_NOT_FOUND", exception.getErrorCode());
        logger.info("添加消息到对话-对话不存在测试通过");
    }

    @Test
    void testAddMessageToConversation_DeletedConversation() {
        // 准备测试数据
        Long conversationId = 1L;
        String content = "你好！";

        // 设置对话为已删除
        testConversation.setIsDeleted((byte) 1);

        // 模拟依赖
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(testConversation);

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            conversationService.addMessageToConversation(conversationId, content, testUser);
        });

        // 验证异常信息
        assertEquals("CONVERSATION_DELETED", exception.getErrorCode());
        logger.info("添加消息到对话-对话已删除测试通过");
    }

    @Test
    void testDeleteConversation_Success() {
        // 准备测试数据
        Long conversationId = 1L;

        // 模拟依赖
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(testConversation);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // 执行测试
        conversationService.deleteConversation(conversationId, testUser);

        // 验证结果
        assertEquals((byte) 1, testConversation.getIsDeleted());
        verify(conversationRepository).save(testConversation);
        logger.info("删除对话测试通过");
    }

    @Test
    void testRestoreConversation_Success() {
        // 准备测试数据
        Long conversationId = 1L;
        testConversation.setIsDeleted((byte) 1); // 预设为已删除

        // 模拟依赖
        when(conversationRepository.findByIdAndUserWithMessages(conversationId, testUser)).thenReturn(testConversation);
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);

        // 执行测试
        conversationService.restoreConversation(conversationId, testUser);

        // 验证结果
        assertEquals((byte) 0, testConversation.getIsDeleted());
        verify(conversationRepository).save(testConversation);
        logger.info("恢复对话测试通过");
    }

    @Test
    void testGetDeletedConversations_Success() {
        // 准备测试数据
        testConversation.setIsDeleted((byte) 1); // 预设为已删除
        List<Conversation> expectedConversations = Arrays.asList(testConversation);

        // 模拟依赖
        when(conversationRepository.findByUserAndIsDeletedOrderByUpdatedAtDesc(testUser, (byte) 1))
                .thenReturn(expectedConversations);

        // 执行测试
        List<Conversation> result = conversationService.getDeletedConversations(testUser);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedConversations.size(), result.size());
        logger.info("获取已删除对话测试通过");
    }

    @Test
    void testDeleteMessage_Success() {
        // 准备测试数据
        Long messageId = 1L;

        // 创建消息
        Message message = new Message();
        message.setId(messageId);
        message.setConversation(testConversation);
        message.setContent("测试消息");

        // 模拟依赖
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        doNothing().when(messageRepository).delete(message);

        // 执行测试
        conversationService.deleteMessage(messageId, testUser);

        // 验证结果
        verify(messageRepository).delete(message);
        logger.info("删除单条消息测试通过");
    }

    @Test
    void testDeleteMessage_MessageNotFound() {
        // 准备测试数据
        Long messageId = 999L;

        // 模拟依赖 - 消息不存在
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        // 执行测试并验证异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            conversationService.deleteMessage(messageId, testUser);
        });

        // 验证异常信息
        assertEquals("MESSAGE_NOT_FOUND", exception.getErrorCode());
        logger.info("删除单条消息-消息不存在测试通过");
    }

    @Test
    void testGetConversationMessages_Success() {
        // 准备测试数据
        List<Message> expectedMessages = new ArrayList<>();
        Message message1 = new Message();
        message1.setContent("消息1");
        expectedMessages.add(message1);

        // 模拟依赖
        when(messageRepository.findByConversationOrderByCreatedAtAsc(testConversation)).thenReturn(expectedMessages);

        // 执行测试
        List<Message> result = conversationService.getConversationMessages(testConversation);

        // 验证结果
        assertNotNull(result);
        assertEquals(expectedMessages.size(), result.size());
        logger.info("获取对话消息列表测试通过");
    }

    @Test
    void testBuildConversationContext_Success() {
        // 准备测试数据
        Message userMessage1 = new Message();
        userMessage1.setContent("你好");
        userMessage1.setSenderType((byte) 1);
        userMessage1.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        Message aiMessage1 = new Message();
        aiMessage1.setContent("你好！");
        aiMessage1.setSenderType((byte) 2);
        aiMessage1.setCreatedAt(LocalDateTime.now().minusMinutes(9));

        Message userMessage2 = new Message();
        userMessage2.setContent("今天天气怎么样？");
        userMessage2.setSenderType((byte) 1);
        userMessage2.setCreatedAt(LocalDateTime.now().minusMinutes(8));

        Message aiMessage2 = new Message();
        aiMessage2.setContent("今天天气很好！");
        aiMessage2.setSenderType((byte) 2);
        aiMessage2.setCreatedAt(LocalDateTime.now().minusMinutes(7));

        List<Message> messages = Arrays.asList(
                aiMessage2, userMessage2, aiMessage1, userMessage1 // 注意顺序是降序
        );

        // 使用反射调用私有方法测试
        try {
            java.lang.reflect.Method method = ConversationService.class.getDeclaredMethod("buildConversationContext", List.class, String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(conversationService, messages, "测试角色");

            // 验证结果 - 应该按时间升序排列
            assertTrue(result.contains("用户: 你好"));
            assertTrue(result.contains("AI: 你好！"));
            assertTrue(result.contains("用户: 今天天气怎么样？"));
            assertTrue(result.contains("AI: 今天天气很好！"));
            logger.info("构建对话上下文测试通过");
        } catch (Exception e) {
            fail("反射调用私有方法失败: " + e.getMessage());
        }
    }
}