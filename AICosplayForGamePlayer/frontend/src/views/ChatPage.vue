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
        <button class="voice-button" @click="openRecordingModal" title="语音输入">
          🎤
        </button>
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
                  :src="`/resource/Character/${character.name}.jpg`" 
                  :alt="character.name"
                  @error="(e) => {
                    e.target.onerror = null;
                    // 尝试加载PNG格式
                    e.target.src = `/resource/Character/${character.name}.png`;
                    e.target.onerror = (err) => {
                      err.target.onerror = null;
                      // 如果PNG也加载失败，才显示默认头像
                      err.target.style.display = 'none';
                      const defaultAvatar = err.target.nextElementSibling;
                      if (defaultAvatar) {
                        defaultAvatar.textContent = '😓';
                        defaultAvatar.style.display = 'block';
                      }
                    };
                  }"
                >
                <span style="display: none;">😓</span>
              </div>
              <div class="character-info">
                <div class="character-name">{{ character.name }}</div>
                <div class="character-desc">{{ character.description.substring(0, 30) }}...</div>
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
                  :src="`/resource/Character/${character.name}.jpg`" 
                  :alt="character.name"
                  @error="(e) => {
                    e.target.onerror = null;
                    // 尝试加载PNG格式
                    e.target.src = `/resource/Character/${character.name}.png`;
                    e.target.onerror = (err) => {
                      err.target.onerror = null;
                      // 如果PNG也加载失败，才显示默认头像
                      err.target.style.display = 'none';
                      const defaultAvatar = err.target.nextElementSibling;
                      if (defaultAvatar) {
                        defaultAvatar.textContent = '😓';
                        defaultAvatar.style.display = 'block';
                      }
                    };
                  }"
                >
                <span style="display: none;">😓</span>
              </div>
              <div class="character-info">
                <div class="character-name">{{ character.name }}</div>
                <div class="character-desc">{{ character.description.substring(0, 30) }}...</div>
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
              <label>预设prompt</label>
              <textarea 
                v-model="customCharacterPresetPrompt" 
                placeholder="请输入预设的对话提示内容"
                rows="3"
              ></textarea>
            </div>
            <div class="form-group">
              <label>角色头像</label>
              <input 
                type="file" 
                accept="image/*" 
                @change="handleAvatarUpload"
                style="display: none;"
                ref="avatarInput"
              >
              <div class="avatar-upload-container">
                <div class="avatar-preview" @click="$refs.avatarInput.click()">
                  <img v-if="avatarPreviewUrl" :src="avatarPreviewUrl" class="preview-image">
                  <div v-else class="avatar-placeholder">
                    <span>+ 上传头像</span>
                  </div>
                </div>
                <div class="avatar-upload-text" @click="$refs.avatarInput.click()">
                  点击选择图片
                </div>
              </div>
              <div v-if="customCharacterAvatar" class="file-name">
                {{ customCharacterAvatar.name }} ({{ formatFileSize(customCharacterAvatar.size) }})
              </div>
              <div v-if="avatarError" class="error-message">{{ avatarError }}</div>
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

  <!-- 录音模态框 -->
  <div v-if="showRecordingModal" class="modal-overlay" @click.self="stopRecording">
    <div class="modal-content">
      <div class="modal-header">
        <h3>正在录音</h3>
        <button class="close-button" @click="stopRecording">&times;</button>
      </div>
      <div class="modal-body">
        <div class="recording-indicator">
          <span class="recording-dot"></span>
          <span>请开始说话...</span>
        </div>
        
        <div class="recording-duration">
          录音时长: {{ Math.floor(recordingDuration / 60) }}:{{ recordingDuration % 60 < 10 ? '0' + (recordingDuration % 60) : recordingDuration % 60 }}
        </div>
        
        <!-- 录音指示器 -->
        <div class="recording-visualizer">
          <div class="visualizer-bars">
            <div class="bar"></div>
            <div class="bar"></div>
            <div class="bar"></div>
            <div class="bar"></div>
            <div class="bar"></div>
          </div>
        </div>
        
        <p style="text-align: center; color: #666; margin-top: 10px;">
          点击确定按钮停止录音并转换为文字
        </p>
      </div>
      <div class="modal-footer">
        <button 
          class="confirm-button" 
          @click="toggleRecording"
          :disabled="showRecordingModal && !isRecording"
        >
          {{ isRecording ? '停止' : '开始' }}
        </button>
        <button class="confirm-button" @click="stopRecording" :disabled="!isRecording">
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { conversationAPI, gameCharacterAPI, speechAPI } from '../utils/api.js'

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
const customCharacterPresetPrompt = ref('')
const customCharacterAvatar = ref(null)
const avatarPreviewUrl = ref('')
const avatarError = ref('')
const avatarInput = ref(null)
const maxFileSize = 2 * 1024 * 1024; // 2MB

// 语音输入相关变量
const showRecordingModal = ref(false)
const isRecording = ref(false)
const recordingDuration = ref(0)
let mediaRecorder = null
let audioChunks = []
let recordingInterval = null
let stream = null

// 切换录音状态（开始/停止）
const toggleRecording = async () => {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording()
  }
}

// 初始化时加载对话列表和角色列表
onMounted(() => {
  loadConversations()
  loadCharacters()
})

onUnmounted(() => {
  // 清理录音资源
  if (recordingInterval) {
    clearInterval(recordingInterval)
  }
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
  }
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

// 初始化录音功能
const initRecording = () => {
  // 重置录音相关变量
  audioChunks = []
  recordingDuration.value = 0
}

// 打开录音模态框
const openRecordingModal = () => {
  showRecordingModal.value = true
  initRecording()
  // 不自动开始录音，等待用户点击开始按钮
}

// 开始录音
const startRecording = async () => {
  try {
    // 获取用户媒体设备权限
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    
    // 创建MediaRecorder实例
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
    
    // 重置录音数据
    audioChunks = []
    
    // 监听数据可用事件
    mediaRecorder.addEventListener('dataavailable', event => {
      audioChunks.push(event.data)
    })
    
    // 开始录音
    mediaRecorder.start()
    
    isRecording.value = true
    recordingDuration.value = 0
    
    // 开始计时
    recordingInterval = setInterval(() => {
      recordingDuration.value++
    }, 1000)
    
    console.log('录音开始')
  } catch (error) {
    console.error('开始录音失败:', error)
    alert('开始录音失败，请检查麦克风权限')
    showRecordingModal.value = false
  }
}

// 停止录音并处理
const stopRecording = async () => {
  try {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      // 停止录音
      mediaRecorder.stop()
    }
    
    // 停止计时器
    if (recordingInterval) {
      clearInterval(recordingInterval)
      recordingInterval = null
    }
    
    isRecording.value = false
    
    // 停止媒体流
    if (stream) {
      stream.getTracks().forEach(track => track.stop())
      stream = null
    }
    
    // 等待录音数据处理完成
    await new Promise(resolve => {
      if (mediaRecorder && mediaRecorder.state === 'inactive') {
        resolve()
      } else {
        mediaRecorder.addEventListener('stop', resolve, { once: true })
      }
    })
    
    // 发送音频到后端转文字
    if (audioChunks.length > 0) {
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' })
      await convertSpeechToText(audioBlob)
    }
    
    showRecordingModal.value = false
    console.log('录音停止并处理完成')
  } catch (error) {
    console.error('停止录音失败:', error)
    alert('停止录音失败，请重试')
    showRecordingModal.value = false
  }
}

// 将语音转换为文字
const convertSpeechToText = async (audioBlob) => {
  try {
    // 调用语音识别API
    const response = await speechAPI.recognize(audioBlob)
    
    // 检查响应并填充识别结果
    if (response && response.text) {
      inputMessage.value = response.text
    } else {
      // 如果后端API不可用，使用模拟结果
      inputMessage.value = '这是一段模拟的语音识别结果。在实际项目中，这里会显示从后端API返回的真实语音识别结果。'
    }
  } catch (error) {
    console.error('语音转文字失败:', error)
    // 错误处理：如果API调用失败，使用模拟结果
    inputMessage.value = '语音识别服务暂时不可用，这是一段模拟的语音识别结果。'
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
  const file = event.target.files[0]
  if (!file) return
  
  // 重置错误信息
  avatarError.value = ''
  
  // 检查文件大小
  if (file.size > maxFileSize) {
    avatarError.value = `文件大小不能超过 ${formatFileSize(maxFileSize)}`
    customCharacterAvatar.value = null
    avatarPreviewUrl.value = ''
    return
  }
  
  // 检查文件类型
  if (!file.type.startsWith('image/')) {
    avatarError.value = '请选择图片文件'
    customCharacterAvatar.value = null
    avatarPreviewUrl.value = ''
    return
  }
  
  // 创建预览
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarPreviewUrl.value = e.target.result
    customCharacterAvatar.value = file
  }
  reader.readAsDataURL(file)
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes'
  
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 创建自定义角色并创建对话
const createCustomCharacterAndChat = async () => {
  try {
    // 表单验证
    if (!customCharacterName.value.trim()) {
      alert('请输入角色名称')
      return
    }
    
    if (!customCharacterPrompt.value.trim()) {
      alert('请输入角色描述')
      return
    }
    
    if (!customCharacterPresetPrompt.value.trim()) {
      alert('请输入预设prompt')
      return
    }
    
    // 先创建自定义角色
    const formData = new FormData()
    formData.append('name', customCharacterName.value)
    formData.append('prompt', customCharacterPrompt.value)
    formData.append('presetPrompt', customCharacterPresetPrompt.value)
    if (customCharacterAvatar.value) {
      formData.append('avatar', customCharacterAvatar.value)
    } else {
      // 添加标志让后端知道使用默认头像
      formData.append('useDefaultAvatar', 'true')
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
/* 录音按钮样式 */
.voice-button {
  padding: 12px 16px;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  align-self: flex-end;
  transition: background-color 0.3s;
}

.voice-button:hover {
  background-color: #1976D2;
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
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: #e0e0e0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  overflow: hidden;
}

.character-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
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

.avatar-upload-container {
  text-align: center;
  margin-bottom: 12px;
}

.avatar-preview {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 2px dashed #ccc;
  margin: 0 auto 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #f9f9f9;
}

.avatar-preview:hover {
  border-color: #4CAF50;
  background-color: #f0f8f0;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-placeholder {
  color: #999;
  font-size: 14px;
  text-align: center;
  line-height: 1.4;
}

.avatar-upload-text {
  color: #4CAF50;
  font-size: 14px;
  cursor: pointer;
  transition: color 0.3s;
}

.avatar-upload-text:hover {
  color: #45a049;
  text-decoration: underline;
}

.error-message {
  color: #f44336;
  font-size: 12px;
  margin-top: 4px;
}

/* 模态框底部按钮样式 */
.modal-footer {
  padding: 20px 24px;
  border-top: 1px solid #e0e0e0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 按钮样式 */
.confirm-button,
.cancel-button {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s;
  background-color: #4CAF50;
  color: white;
}

.cancel-button {
  background-color: #f5f5f5;
  color: #333;
}

.confirm-button:hover {
  background-color: #45a049;
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(76, 175, 80, 0.2);
}

.cancel-button:hover {
  background-color: #e0e0e0;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* 录音模态框样式 */
.recording-indicator {
  text-align: center;
  margin-bottom: 20px;
}

.recording-dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #f44336;
  animation: pulse 1.5s infinite;
  margin-right: 8px;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.7;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.recording-duration {
  font-size: 16px;
  color: #666;
}

#waveform {
  margin: 20px 0;
  height: 80px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .character-avatar {
    width: 28px;
    height: 28px;
    font-size: 16px;
  }
  
  .voice-button {
    padding: 10px 12px;
    font-size: 14px;
  }
  
  .send-button {
    padding: 10px 16px;
    font-size: 14px;
  }
}
</style>