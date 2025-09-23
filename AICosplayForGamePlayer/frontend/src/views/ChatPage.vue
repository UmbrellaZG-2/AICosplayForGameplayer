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

  <!-- 角色选择模态框 -->
  <div v-if="showCharacterSelectModal" class="modal-overlay" @click="showCharacterSelectModal = false">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>选择角色</h3>
        <button class="close-button" @click="showCharacterSelectModal = false">×</button>
      </div>
      
      <div class="modal-body">
        <!-- 预设角色列表 -->
        <div class="character-section">
          <h4>预设角色</h4>
          <div class="character-list">
            <div 
              v-for="character in characters.filter(c => c.isPreset)" 
              :key="character.id"
              class="character-item"
              :class="{ selected: selectedCharacterId === character.id }"
              @click="handleCharacterSelect(character.id)"
            >
              <div class="character-avatar">
                <!-- 从指定文件夹读取角色头像 -->
                <img 
                  v-if="character.name" 
                  :src="`/image/Character/${character.name}.jpg`" 
                  :alt="character.name"
                  @error="this.src = `/image/Character/${character.name}.png`"
                  @error.once="this.src = '🎭'"
                >
                <span v-else>🎭</span>
              </div>
              <div class="character-info">
                <div class="character-name">{{ character.name }}</div>
                <div class="character-desc">{{ character.prompt.substring(0, 30) }}...</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 用户自定义角色列表 -->
        <div v-if="characters.filter(c => !c.isPreset).length > 0" class="character-section">
          <h4>我的角色</h4>
          <div class="character-list">
            <div 
              v-for="character in characters.filter(c => !c.isPreset)" 
              :key="character.id"
              class="character-item"
              :class="{ selected: selectedCharacterId === character.id }"
              @click="handleCharacterSelect(character.id)"
            >
              <div class="character-avatar">
                <!-- 从指定文件夹读取角色头像 -->
                <img 
                  v-if="character.name" 
                  :src="`/image/Character/${character.name}.jpg`" 
                  :alt="character.name"
                  @error="this.src = `/image/Character/${character.name}.png`"
                  @error.once="this.src = '🎭'"
                >
                <span v-else>🎭</span>
              </div>
              <div class="character-info">
                <div class="character-name">{{ character.name }}</div>
                <div class="character-desc">{{ character.prompt.substring(0, 30) }}...</div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 自定义角色表单 -->
        <div class="custom-character-section">
          <button class="custom-character-button" @click="toggleCustomCharacterForm">
            {{ showCustomCharacterForm ? '取消' : '+ 创建自定义角色' }}
          </button>
          
          <div v-if="showCustomCharacterForm" class="custom-character-form">
            <div class="form-group">
              <label>角色名称</label>
              <input 
                type="text" 
                v-model="customCharacterName" 
                placeholder="请输入角色名称"
              >
            </div>
            
            <div class="form-group">
              <label>角色描述</label>
              <textarea 
                v-model="customCharacterPrompt" 
                placeholder="请输入角色的详细描述和行为特征"
                rows="4"
              ></textarea>
            </div>
            
            <div class="form-group">
              <label>角色头像</label>
              <input 
                type="file" 
                accept="image/*" 
                @change="handleAvatarUpload"
              >
              <div v-if="customCharacterAvatar" class="file-name">
                {{ customCharacterAvatar.name }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="modal-footer">
        <button class="cancel-button" @click="showCharacterSelectModal = false">取消</button>
        <button class="confirm-button" @click="confirmCreateChat">确认</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { conversationAPI, gameCharacterAPI } from '../utils/api.js'
import { testCreateConversation } from '../utils/test.js'

const router = useRouter()
const conversations = ref([])
const messages = ref([])
const currentConversationId = ref(null)
const currentConversation = ref(null)
const inputMessage = ref('')

// 角色选择相关状态
const showCharacterSelectModal = ref(false)
const characters = ref([])
const selectedCharacterId = ref(null)
const showCustomCharacterForm = ref(false)
const customCharacterName = ref('')
const customCharacterPrompt = ref('')
const customCharacterAvatar = ref(null)

// 初始化时加载对话列表和角色列表
onMounted(() => {
  loadConversations()
  loadCharacters()
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

// 加载角色列表
const loadCharacters = async () => {
  try {
    const data = await gameCharacterAPI.getAll()
    characters.value = data
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 打开角色选择模态框
const openCharacterSelectModal = () => {
  selectedCharacterId.value = null
  showCustomCharacterForm.value = false
  customCharacterName.value = ''
  customCharacterPrompt.value = ''
  customCharacterAvatar.value = null
  loadCharacters() // 每次打开都重新加载角色列表
  showCharacterSelectModal.value = true
}

// 处理角色选择
const handleCharacterSelect = (characterId) => {
  selectedCharacterId.value = characterId
}

// 切换到自定义角色表单
const toggleCustomCharacterForm = () => {
  showCustomCharacterForm.value = !showCustomCharacterForm.value
}

// 处理头像上传
const handleAvatarUpload = (event) => {
  customCharacterAvatar.value = event.target.files[0]
}

// 创建自定义角色并创建对话
const createCustomCharacterAndChat = async () => {
  try {
    // 先创建自定义角色
    const formData = new FormData()
    formData.append('name', customCharacterName.value)
    formData.append('prompt', customCharacterPrompt.value)
    if (customCharacterAvatar.value) {
      formData.append('avatar', customCharacterAvatar.value)
    }
    
    const newCharacter = await gameCharacterAPI.create(formData)
    
    // 然后使用新创建的角色创建对话
    await createNewChatWithCharacter(newCharacter.id)
    
    // 关闭模态框
    showCharacterSelectModal.value = false
  } catch (error) {
    console.error('创建自定义角色失败:', error)
    alert('创建自定义角色失败，请重试')
  }
}

// 创建新对话（带角色）
const createNewChatWithCharacter = async (characterId) => {
  try {
    const title = '新对话'
    const newConversation = await conversationAPI.create(title, characterId)
    
    conversations.value.unshift(newConversation)
    switchConversation(newConversation.id)
  } catch (error) {
    console.error('创建新对话失败:', error)
    alert('创建新对话失败，请重试')
  }
}

// 确认创建新对话
const confirmCreateChat = async () => {
  if (selectedCharacterId.value) {
    // 使用已选择的角色创建对话
    await createNewChatWithCharacter(selectedCharacterId.value)
    showCharacterSelectModal.value = false
  } else if (showCustomCharacterForm.value && customCharacterName.value && customCharacterPrompt.value) {
    // 创建自定义角色并创建对话
    await createCustomCharacterAndChat()
  } else {
    alert('请选择一个角色或创建自定义角色')
  }
}

// 原来的创建新对话按钮现在打开角色选择模态框
const createNewChat = () => {
  openCharacterSelectModal()
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
/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  color: #333;
  font-size: 20px;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-button:hover {
  background-color: #f5f5f5;
  color: #666;
}

.modal-body {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.character-section {
  margin-bottom: 24px;
}

.character-section h4 {
  margin: 0 0 16px 0;
  color: #333;
  font-size: 16px;
  font-weight: 600;
}

.character-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.character-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background-color: #f8f9fa;
}

.character-item:hover {
  background-color: #e9ecef;
}

.character-item.selected {
  border-color: #4CAF50;
  background-color: #f1f8e9;
}

.character-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.character-name {
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.character-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.4;
}

.custom-character-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e0e0e0;
}

.custom-character-button {
  width: 100%;
  padding: 12px 20px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.custom-character-button:hover {
  background-color: #1976D2;
}

.custom-character-form {
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 600;
  color: #333;
  font-size: 14px;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
}

.form-group input[type="text"]:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.form-group input[type="file"] {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
}

.file-name {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-button,
.confirm-button {
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #333;
}

.cancel-button:hover {
  background-color: #e0e0e0;
}

.confirm-button {
  background-color: #4CAF50;
  color: white;
}

.confirm-button:hover {
  background-color: #45a049;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .character-avatar {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
}
</style>