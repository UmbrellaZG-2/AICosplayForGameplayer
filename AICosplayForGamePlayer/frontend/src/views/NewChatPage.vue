<template>
  <div class="chat-container">
    <!-- 侧边栏 -->
    <div class="sidebar" :class="{ collapsed: isSidebarCollapsed }">
      <div class="sidebar-header">
        <div class="logo-section">
          <div class="logo">
            <span class="logo-icon">🎮</span>
            <span class="logo-text" v-if="!isSidebarCollapsed">AICosplay</span>
          </div>
          <button class="toggle-sidebar-btn" @click="toggleSidebar">
            <span v-if="isSidebarCollapsed">☰</span>
            <span v-else>✕</span>
          </button>
        </div>
        <button class="new-chat-btn" @click="createNewChat">
          <span class="plus-icon">+</span>
          <span v-if="!isSidebarCollapsed">新建对话</span>
        </button>
      </div>
      
      <div class="conversations-list">
        <div 
          v-for="conversation in conversations" 
          :key="conversation.id"
          class="conversation-item"
          :class="{ active: currentConversationId === conversation.id }"
          @click="switchConversation(conversation.id)"
        >
          <div class="conversation-icon">💬</div>
          <div class="conversation-info" v-if="!isSidebarCollapsed">
            <div class="conversation-title">{{ getConversationTitle(conversation) }}</div>
            <div class="conversation-time">{{ formatTime(conversation.updatedAt) }}</div>
          </div>
          <button 
            v-if="!isSidebarCollapsed"
            class="delete-conversation-btn" 
            @click.stop="deleteConversation(conversation.id)"
          >
            ×
          </button>
        </div>
      </div>
      
      <div class="sidebar-footer" v-if="!isSidebarCollapsed">
        <div class="user-info">
          <div class="avatar">👤</div>
          <div class="user-details">
            <div class="username">{{ username }}</div>
            <div class="user-status">在线</div>
          </div>
        </div>
        <button class="logout-btn" @click="handleLogout">
          <span>退出登录</span>
        </button>
      </div>
    </div>
    
    <!-- 主聊天区域 -->
    <div class="main-content">
      <div class="chat-header">
        <div class="header-left">
          <button class="menu-toggle" @click="toggleSidebar">☰</button>
          <div v-if="currentConversation" class="current-conversation-info">
            <div class="character-avatar">
              <img 
                v-if="getCharacterAvatar()" 
                :src="getCharacterAvatar()" 
                :alt="getCharacterName()"
                @error="handleAvatarError"
              >
              <span v-else>🤖</span>
            </div>
            <div class="conversation-details">
              <div class="character-name">{{ getCharacterName() }}</div>
              <div class="conversation-status">在线</div>
            </div>
          </div>
        </div>
        <div class="header-actions">
          <button class="action-btn">📋</button>
          <button class="action-btn" @click="deleteCurrentConversation">🗑️</button>
        </div>
      </div>
      
      <div class="chat-messages" ref="chatMessagesRef">
        <div 
          v-for="message in messages" 
          :key="message.id"
          :class="['message', message.senderType === 1 ? 'user-message' : 'ai-message']"
        >
          <div class="message-avatar">
            <div v-if="message.senderType === 1" class="avatar">👤</div>
            <div v-else class="avatar">
              <img 
                v-if="getCharacterAvatar()" 
                :src="getCharacterAvatar()" 
                :alt="getCharacterName()"
                @error="handleAvatarError"
              >
              <span v-else>🤖</span>
            </div>
          </div>
          <div class="message-content">
            <div class="message-text">{{ message.content }}</div>
            <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
          </div>
        </div>
        
        <!-- AI思考状态 -->
        <div v-if="isAIThinking" class="message ai-message">
          <div class="message-avatar">
            <div class="avatar">
              <img 
                v-if="getCharacterAvatar()" 
                :src="getCharacterAvatar()" 
                :alt="getCharacterName()"
                @error="handleAvatarError"
              >
              <span v-else>🤖</span>
            </div>
          </div>
          <div class="message-content">
            <div class="thinking-indicator">
              <div class="dot"></div>
              <div class="dot"></div>
              <div class="dot"></div>
            </div>
          </div>
        </div>
        
        <!-- 空状态 -->
        <div v-if="!currentConversationId && conversations.length === 0" class="empty-state">
          <div class="empty-icon">🎮</div>
          <div class="empty-text">欢迎使用 AICosplay</div>
          <div class="empty-subtext">开始一段新的对话</div>
          <button class="new-chat-btn-large" @click="createNewChat">新建对话</button>
        </div>
      </div>
      
      <div class="chat-input-area" v-if="currentConversationId">
        <div class="input-container">
          <textarea
            v-model="inputMessage"
            placeholder="输入消息..."
            @keydown.enter="handleEnterKey"
            class="message-input"
            rows="1"
          ></textarea>
          <div class="input-actions">
            <button 
              class="send-btn" 
              :disabled="!inputMessage.trim() || isAIThinking"
              @click="sendMessage"
            >
              <span>发送</span>
            </button>
          </div>
        </div>
        <div class="input-tips">
          Enter 发送，Shift + Enter 换行
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { conversationAPI, gameCharacterAPI, userAPI } from '../utils/api.js'

const router = useRouter()

// 状态管理
const isSidebarCollapsed = ref(false)
const conversations = ref([])
const messages = ref([])
const currentConversationId = ref(null)
const currentConversation = ref(null)
const inputMessage = ref('')
const isAIThinking = ref(false)
const chatMessagesRef = ref(null)
const characters = ref([])

// 用户信息
const username = ref('用户')

// 切换侧边栏
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

// 格式化时间
const formatTime = (dateString) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffInDays = Math.floor((now - date) / (1000 * 60 * 60 * 24))
  
  if (diffInDays === 0) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (diffInDays === 1) {
    return '昨天'
  } else if (diffInDays < 7) {
    return date.toLocaleDateString([], { weekday: 'short' })
  } else {
    return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
  }
}

// 格式化消息时间
const formatMessageTime = (dateString) => {
  const date = new Date(dateString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// 处理头像加载错误
const handleAvatarError = (event) => {
  event.target.style.display = 'none'
}

// 获取角色头像
const getCharacterAvatar = () => {
  if (!currentConversation.value?.characterId) return null
  
  const character = getCharacterById(currentConversation.value.characterId)
  if (!character?.name) return null
  
  return `/resource/Character/${character.name}.jpg`
}

// 获取角色名称
const getCharacterName = () => {
  if (currentConversation.value) {
    if (currentConversation.value.characterName) {
      return currentConversation.value.characterName
    }
    if (currentConversation.value.characterId) {
      const character = getCharacterById(currentConversation.value.characterId)
      if (character && character.name) {
        return character.name
      }
    }
  }
  return 'AI助手'
}

// 通过ID获取角色信息
const getCharacterById = (characterId) => {
  return characters.value.find(character => character.id === characterId)
}

// 获取对话标题
const getConversationTitle = (conversation) => {
  if (conversation.characterName) {
    return conversation.characterName
  }
  if (conversation.characterId) {
    const character = getCharacterById(conversation.characterId)
    if (character && character.name) {
      return character.name
    }
  }
  return conversation.conversationTitle || '新对话'
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
    }
  })
}

// 创建新对话
const createNewChat = async () => {
  try {
    // 调用后端API创建新对话
    const response = await conversationAPI.create('新对话')
    const newConversation = response.data
    
    // 更新对话列表
    conversations.value.unshift(newConversation)
    
    // 切换到新对话
    await switchConversation(newConversation.id)
  } catch (error) {
    console.error('创建新对话失败:', error)
  }
}

// 切换对话
const switchConversation = async (id) => {
  currentConversationId.value = id
  currentConversation.value = conversations.value.find(c => c.id === id)
  // 加载对话消息
  await loadMessages(id)
}

// 加载消息
const loadMessages = async (conversationId) => {
  try {
    if (!conversationId) return
    
    // 调用后端API获取消息
    const response = await conversationAPI.getMessages(conversationId)
    messages.value = response.data || []
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  }
}

// 删除对话
const deleteConversation = async (id) => {
  if (confirm('确定要删除这个对话吗？')) {
    try {
      // 调用后端API删除对话
      await conversationAPI.delete(id)
      
      // 更新本地状态
      conversations.value = conversations.value.filter(c => c.id !== id)
      
      // 如果删除的是当前对话，清空当前对话
      if (currentConversationId.value === id) {
        currentConversationId.value = null
        currentConversation.value = null
        messages.value = []
      }
    } catch (error) {
      console.error('删除对话失败:', error)
    }
  }
}

// 删除当前对话
const deleteCurrentConversation = async () => {
  if (currentConversationId.value) {
    await deleteConversation(currentConversationId.value)
  }
}

// 处理回车键
const handleEnterKey = (event) => {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || isAIThinking.value || !currentConversationId.value) return
  
  const messageContent = inputMessage.value.trim()
  inputMessage.value = ''
  
  try {
    // 添加用户消息到界面
    const userMessage = {
      id: Date.now(),
      content: messageContent,
      senderType: 1, // 用户
      createdAt: new Date().toISOString()
    }
    
    messages.value.push(userMessage)
    scrollToBottom()
    
    // 显示AI思考状态
    isAIThinking.value = true
    scrollToBottom()
    
    // 调用后端API发送消息
    const response = await conversationAPI.addMessage(currentConversationId.value, {
      content: messageContent,
      senderType: 1 // 用户
    })
    
    // 获取AI回复
    isAIThinking.value = false
    
    if (response && response.data) {
      messages.value.push({
        id: response.data.id,
        content: response.data.content,
        senderType: 0, // AI
        createdAt: response.data.createdAt
      })
    }
    
    scrollToBottom()
  } catch (error) {
    console.error('发送消息失败:', error)
    isAIThinking.value = false
  }
}

// 退出登录
const handleLogout = async () => {
  try {
    // 调用后端退出登录API
    await userAPI.logout()
    
    // 清除本地存储
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('isLoggedIn')
    
    // 跳转到登录页面
    router.push('/')
  } catch (error) {
    console.error('退出登录失败:', error)
  }
}

// 加载对话列表
const loadConversations = async () => {
  try {
    // 调用后端API获取对话列表
    const response = await conversationAPI.getAll()
    conversations.value = response.data || []
  } catch (error) {
    console.error('加载对话列表失败:', error)
  }
}

// 加载角色列表
const loadCharacters = async () => {
  try {
    // 调用后端API获取角色列表
    const response = await gameCharacterAPI.getAll()
    characters.value = response.data || []
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 初始化
onMounted(async () => {
  await loadConversations()
  await loadCharacters()
  
  // 如果有对话，加载第一个对话的消息
  if (conversations.value.length > 0) {
    await switchConversation(conversations.value[0].id)
  }
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f5f5f5;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 侧边栏样式 */
.sidebar {
  width: 260px;
  background-color: #202123;
  color: white;
  display: flex;
  flex-direction: column;
  transition: width 0.3s ease;
  border-right: 1px solid #444;
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #444;
}

.logo-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
}

.logo-text {
  font-size: 20px;
  font-weight: bold;
}

.toggle-sidebar-btn {
  background: none;
  border: none;
  color: white;
  font-size: 18px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.toggle-sidebar-btn:hover {
  background-color: #333;
}

.new-chat-btn {
  width: 100%;
  background-color: #444654;
  color: white;
  border: 1px solid #555;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #555766;
}

.new-chat-btn-large {
  margin-top: 20px;
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.2s;
}

.new-chat-btn-large:hover {
  background-color: #45a049;
}

.plus-icon {
  font-size: 18px;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  position: relative;
}

.conversation-item:hover {
  background-color: #333;
}

.conversation-item.active {
  background-color: #444;
}

.conversation-icon {
  font-size: 16px;
  margin-right: 10px;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-time {
  font-size: 12px;
  color: #aaa;
  margin-top: 4px;
}

.delete-conversation-btn {
  background: none;
  border: none;
  color: #aaa;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-conversation-btn:hover {
  background-color: #555;
  color: white;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid #444;
}

.user-info {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #555;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  margin-right: 10px;
}

.user-details {
  flex: 1;
}

.username {
  font-size: 14px;
  font-weight: 500;
}

.user-status {
  font-size: 12px;
  color: #aaa;
}

.logout-btn {
  width: 100%;
  background-color: #444654;
  color: white;
  border: 1px solid #555;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.logout-btn:hover {
  background-color: #555766;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #eee;
  background-color: white;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.menu-toggle {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}

.menu-toggle:hover {
  background-color: #f5f5f5;
}

.current-conversation-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.character-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
}

.character-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.character-avatar .avatar {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.conversation-details {
  display: flex;
  flex-direction: column;
}

.character-name {
  font-weight: 500;
  font-size: 16px;
}

.conversation-status {
  font-size: 12px;
  color: #666;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  padding: 8px;
  border-radius: 4px;
}

.action-btn:hover {
  background-color: #f5f5f5;
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message {
  display: flex;
  gap: 16px;
  max-width: 85%;
}

.user-message {
  align-self: flex-end;
}

.ai-message {
  align-self: flex-start;
}

.message-avatar {
  flex-shrink: 0;
}

.message-avatar .avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-message .message-content {
  align-items: flex-end;
}

.ai-message .message-content {
  align-items: flex-start;
}

.message-text {
  padding: 12px 16px;
  border-radius: 18px;
  line-height: 1.5;
  max-width: 100%;
  word-wrap: break-word;
}

.user-message .message-text {
  background-color: #4CAF50;
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-message .message-text {
  background-color: #f0f0f0;
  color: #333;
  border-bottom-left-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* AI思考状态指示器 */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
}

.dot {
  width: 8px;
  height: 8px;
  background-color: #999;
  border-radius: 50%;
  animation: bounce 1.5s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}

/* 聊天输入区域 */
.chat-input-area {
  padding: 16px;
  border-top: 1px solid #eee;
  background-color: white;
}

.input-container {
  display: flex;
  flex-direction: column;
  border: 1px solid #ddd;
  border-radius: 12px;
  background-color: white;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.message-input {
  width: 100%;
  border: none;
  outline: none;
  resize: none;
  padding: 16px;
  font-size: 16px;
  border-radius: 12px 12px 0 0;
  min-height: 60px;
  max-height: 200px;
}

.message-input:focus {
  outline: none;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  padding: 0 16px 16px;
}

.send-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.send-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.send-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.input-tips {
  text-align: center;
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  padding: 20px;
  text-align: center;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-text {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 8px;
  color: #333;
}

.empty-subtext {
  font-size: 16px;
  color: #666;
  margin-bottom: 24px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: absolute;
    z-index: 100;
    height: 100%;
  }
  
  .sidebar.collapsed {
    width: 0;
    overflow: hidden;
  }
}
</style>