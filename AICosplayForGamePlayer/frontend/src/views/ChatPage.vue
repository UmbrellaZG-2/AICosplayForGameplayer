<template>
  <div class="chat-container">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>AICosplay</h3>
        <button class="logout-button" @click="handleLogout">退出</button>
      </div>
      
      <button class="new-chat-button" @click="createNewChat">
        + 新对话
      </button>
      
      <div class="chat-history">
        <div 
          v-for="conversation in conversations" 
          :key="conversation.id"
          class="chat-item" 
          :class="{ active: currentConversationId === conversation.id }"
          @click="switchConversation(conversation.id)"
        >
          <div class="chat-title">{{ conversation.conversationTitle || '未命名对话' }}</div>
          <button class="delete-chat" @click.stop="deleteConversation(conversation.id)">×</button>
        </div>
      </div>
    </div>
    
    <!-- 主聊天区域 -->
    <div class="chat-main" v-if="currentConversationId">
      <div class="chat-header">
        <h2>{{ currentConversation?.conversationTitle || '未命名对话' }}</h2>
        <div class="header-actions">
          <button class="action-button">📝</button>
          <button class="action-button">📤</button>
        </div>
      </div>
      
      <div class="chat-messages">
        <div v-for="message in messages" :key="message.id" class="message-wrapper">
          <div :class="['message', message.senderType]">
            <div class="message-avatar">{{ message.senderType === 'user' ? '👤' : '🤖' }}</div>
            <div class="message-content">{{ message.content }}</div>
          </div>
        </div>
      </div>
      
      <div class="chat-input-area">
        <div class="input-wrapper">
          <textarea 
            v-model="inputMessage"
            placeholder="输入消息..."
            @keydown.ctrl.enter="sendMessage"
          ></textarea>
          <button class="send-button" @click="sendMessage">发送</button>
        </div>
        <div class="input-tip">Ctrl + Enter 快速发送</div>
      </div>
    </div>
    
    <!-- 选择对话提示 -->
    <div class="select-chat-tip" v-else>
      <div class="tip-content">
        <h3>欢迎使用 AICosplayForGamePlayer</h3>
        <p>请创建一个新对话或选择一个历史对话开始聊天</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { conversationAPI } from '../utils/api.js'
import { testCreateConversation } from '../utils/test.js'

const router = useRouter()
const conversations = ref([])
const messages = ref([])
const currentConversationId = ref(null)
const currentConversation = ref(null)
const inputMessage = ref('')

// 初始化时加载对话列表
onMounted(() => {
  loadConversations()
})

// 加载对话列表
const loadConversations = async () => {
  try {
    const data = await conversationAPI.getAll()
    conversations.value = data
  } catch (error) {
    console.error('加载对话列表失败:', error)
  }
}

// 创建新对话
const createNewChat = async () => {
  try {
    console.log('点击了创建新对话按钮');
    // 使用默认标题替代prompt，避免浏览器兼容性问题
    const title = '新对话'
    
    console.log('准备发送请求到后端API...');
    
    // 使用直接的fetch请求，绕过可能存在问题的api.js
    const response = await fetch('/api/conversations', {
      method: 'POST',
      credentials: 'include', // 确保携带凭证
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ title })
    });
    
    console.log('收到响应，状态码:', response.status);
    
    if (!response.ok) {
      throw new Error(`HTTP错误! 状态码: ${response.status}`);
    }
    
    const newConversation = await response.json();
    console.log('创建新对话成功:', newConversation);
    
    conversations.value.unshift(newConversation)
    switchConversation(newConversation.id)
  } catch (error) {
    console.error('创建新对话失败:', error)
    alert('创建新对话失败，请重试')
  }
}

// 切换对话
const switchConversation = async (id) => {
  currentConversationId.value = id
  currentConversation.value = conversations.value.find(c => c.id === id)
  
  // 加载该对话的消息
  try {
    const data = await conversationAPI.getMessages(id)
    messages.value = data
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || !currentConversationId.value) return
  
  const content = inputMessage.value.trim()
  inputMessage.value = ''
  
  try {
    // 先在前端显示用户消息
    const userMessage = {
      id: Date.now(), // 临时ID
      senderType: 'user',
      content,
      createdAt: new Date()
    }
    messages.value.push(userMessage)
    
    // 发送到后端
    await conversationAPI.addMessage(currentConversationId.value, {
      senderType: 'user',
      content
    })
    
    // 模拟AI回复
    setTimeout(() => {
      const aiMessage = {
        id: Date.now() + 1, // 临时ID
        senderType: 'ai',
        content: '感谢您的提问！我是AICosplay助手，很高兴为您提供帮助。',
        createdAt: new Date()
      }
      messages.value.push(aiMessage)
      
      // 发送AI消息到后端
      conversationAPI.addMessage(currentConversationId.value, {
        senderType: 'ai',
        content: aiMessage.content
      })
    }, 1000)
    
  } catch (error) {
    console.error('发送消息失败:', error)
    alert('发送消息失败，请重试')
    // 移除临时显示的消息
    messages.value.pop()
  }
}

// 删除对话
const deleteConversation = async (id) => {
  if (!confirm('确定要删除这个对话吗？')) return
  
  try {
    await conversationAPI.delete(id)
    conversations.value = conversations.value.filter(c => c.id !== id)
    
    if (currentConversationId.value === id) {
      currentConversationId.value = null
      currentConversation.value = null
      messages.value = []
    }
  } catch (error) {
    console.error('删除对话失败:', error)
    alert('删除对话失败，请重试')
  }
}

// 退出登录
const handleLogout = () => {
  localStorage.removeItem('isLoggedIn')
  router.push('/')
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f0f2f5;
}

/* 侧边栏样式 */
.sidebar {
  width: 280px;
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 5px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sidebar-header h3 {
  color: #333;
  margin: 0;
}

.logout-button {
  padding: 6px 12px;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.logout-button:hover {
  background-color: #d32f2f;
}

.new-chat-button {
  margin: 15px;
  padding: 12px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.new-chat-button:hover {
  background-color: #45a049;
}

.chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.chat-item {
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
  background-color: #f8f9fa;
}

.chat-item:hover {
  background-color: #e9ecef;
}

.chat-item.active {
  background-color: #e3f2fd;
  border-left: 4px solid #2196f3;
}

.chat-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-right: 10px;
  color: #333;
}

.delete-chat {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.chat-item:hover .delete-chat {
  opacity: 1;
}

.delete-chat:hover {
  background-color: #ff5252;
  color: white;
}

/* 主聊天区域样式 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  padding: 20px 30px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.chat-header h2 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-button {
  background: none;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 6px 10px;
  cursor: pointer;
  font-size: 16px;
}

.action-button:hover {
  background-color: #f5f5f5;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 30px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background-color: #f9f9f9;
}

.message-wrapper {
  display: flex;
}

.message-wrapper.user {
  justify-content: flex-end;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 70%;
}

.message.user {
  justify-content: flex-end;
}

.message.ai .message-content {
  background-color: #fff;
  border: 1px solid #e0e0e0;
}

.message.user .message-content {
  background-color: #4CAF50;
  color: white;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.chat-input-area {
  padding: 20px 30px;
  border-top: 1px solid #e0e0e0;
  background-color: #fff;
}

.input-wrapper {
  display: flex;
  gap: 10px;
}

.input-wrapper textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  resize: none;
  font-size: 16px;
  line-height: 1.5;
  min-height: 60px;
  max-height: 120px;
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.send-button {
  padding: 12px 24px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  align-self: flex-end;
}

.send-button:hover {
  background-color: #45a049;
}

.input-tip {
  margin-top: 8px;
  color: #999;
  font-size: 12px;
  text-align: right;
}

/* 选择对话提示 */
.select-chat-tip {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f9f9f9;
}

.tip-content {
  text-align: center;
  color: #666;
}

.tip-content h3 {
  margin-bottom: 10px;
  color: #333;
}
</style>