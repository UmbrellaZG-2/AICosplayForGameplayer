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
        <div v-for="message in messages" :key="message.id" :class="['message-wrapper', message.senderType]">
          <!-- 语音消息显示 -->
          <template v-if="message.isVoiceMessage">
            <div class="message">
              <div class="message-avatar">{{ message.senderType === 'user' ? '👤' : '🤖' }}</div>
              <div :class="['message-voice', message.senderType]">
                <button class="voice-play-button" @click="playVoiceMessage(message)">
                  <span class="voice-icon">▶</span>
                  <span class="voice-duration">{{ message.voiceDuration }}s</span>
                </button>
              </div>
            </div>
          </template>
          <!-- 普通文本消息显示 -->
          <template v-else>
            <div class="message">
              <div class="message-avatar">{{ message.senderType === 'user' ? '👤' : '🤖' }}</div>
              <div :class="['message-content', message.senderType]">{{ message.content }}</div>
            </div>
          </template>
          <!-- AI消息的"显示文字"按钮 -->
          <div v-if="message.senderType === 'ai' && message.isVoiceMessage" class="ai-message-actions">
            <button class="action-btn" @click="toggleAiMessageText(message)">
              {{ message.showText ? '隐藏文字' : '显示文字' }}
            </button>
          </div>
          <!-- 显示AI消息的文字内容（如果用户点击了"显示文字"） -->
          <div v-if="message.senderType === 'ai' && message.isVoiceMessage && message.showText" class="ai-message-text">
            <div class="message">
              <div class="message-avatar">🤖</div>
              <div class="message-content ai">{{ message.content }}</div>
            </div>
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
        <p style="text-align: center; color: #999; margin-top: 5px; font-size: 12px;">
          录音最长不超过60秒
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
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { conversationAPI, gameCharacterAPI, speechAPI, userAPI } from '../utils/api.js'

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
let loading = ref(false) // 添加loading状态变量

// 获取消息列表
const fetchMessages = async () => {
  if (!currentConversationId.value) return
  
  try {
    const data = await conversationAPI.getMessages(currentConversationId.value)
    // 为AI语音消息添加showText属性，默认为false
    messages.value = data.map(msg => ({
      ...msg,
      showText: false // 默认不显示AI消息的文字
    }))
    
    // 滚动到底部
    setTimeout(scrollToBottom, 100)
  } catch (error) {
    console.error('加载消息失败:', error)
    messages.value = []
  }
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  const chatMessagesElement = document.querySelector('.chat-messages')
  if (chatMessagesElement) {
    chatMessagesElement.scrollTop = chatMessagesElement.scrollHeight
  }
}

// 切换录音状态（开始/停止）
const toggleRecording = async () => {
  if (isRecording.value) {
    await stopRecording()
  } else {
    await startRecording()
  }
}

// 初始化时加载对话列表和角色列表
onMounted(async () => {
  await loadConversations()
  await loadCharacters()
  
  // 如果有对话，加载第一条对话的消息
  if (conversations.value.length > 0 && !currentConversationId.value) {
    currentConversationId.value = conversations.value[0].id
    currentConversation.value = conversations.value[0]
    await fetchMessages()
  }
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
    // 重置录音数据
    audioChunks = []
    
    // 获取用户媒体设备
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    
    // 创建MediaRecorder实例
    const options = {
      mimeType: 'audio/webm;codecs=opus'
    }
    
    if (!MediaRecorder.isTypeSupported(options.mimeType)) {
      options.mimeType = 'audio/webm'
    }
    
    if (!MediaRecorder.isTypeSupported(options.mimeType)) {
      options.mimeType = 'audio/ogg;codecs=opus'
    }
    
    mediaRecorder = new MediaRecorder(stream, options)
    
    // 监听数据可用事件
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }
    
    // 开始录音
    mediaRecorder.start()
    
    isRecording.value = true;
    recordingDuration.value = 0;
    
    // 开始计时，并添加60秒时间限制
    recordingInterval = setInterval(() => {
      recordingDuration.value++;
      
      // 如果录音时长达到60秒，自动停止录音
      if (recordingDuration.value >= 60) {
        stopRecording();
      }
    }, 1000);
    
    console.log('录音开始');
  } catch (error) {
    console.error('开始录音失败:', error);
    alert('开始录音失败，请检查麦克风权限');
    showRecordingModal.value = false;
  }
};

// 停止录音并处理
const stopRecording = async () => {
  try {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      // 停止录音
      mediaRecorder.stop();
    }
    
    // 停止计时器
    if (recordingInterval) {
      clearInterval(recordingInterval);
      recordingInterval = null;
    }
    
    isRecording.value = false;
    
    // 停止媒体流
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      stream = null;
    }
    
    // 等待录音数据处理完成
    await new Promise(resolve => {
      if (mediaRecorder && mediaRecorder.state === 'inactive') {
        resolve();
      } else {
        mediaRecorder.addEventListener('stop', resolve, { once: true });
      }
    });
    
    // 发送音频到后端转文字
    if (audioChunks.length > 0) {
      const audioBlob = new Blob(audioChunks, { type: 'audio/webm' });
      await convertSpeechToText(audioBlob);
    }
    
    showRecordingModal.value = false;
    console.log('录音停止并处理完成');
  } catch (error) {
    console.error('停止录音失败:', error);
    alert('停止录音失败，请重试');
    showRecordingModal.value = false;
  }
};

// 语音识别并转换为文本
    const convertSpeechToText = async (audioBlob) => {
      try {
        const formData = new FormData();
        formData.append('audio', audioBlob);
        
        // 显示加载状态
        loading.value = true;
        
        // 调用语音识别API
        const response = await speechAPI.recognize(formData);
        const recognizedText = response.data.text;
        
        // 创建语音消息对象
        const voiceMessage = {
          id: Date.now(), // 临时ID，后端会覆盖
          content: recognizedText,
          senderType: 'user',
          conversationId: currentConversation.value.id,
          isVoiceMessage: true,
          voiceDuration: Math.round(audioBlob.duration || 0), // 音频时长
          voiceUrl: URL.createObjectURL(audioBlob), // 临时URL用于前端播放
          timestamp: new Date().toISOString()
        };
        
        // 先在前端显示语音消息
        messages.value.push(voiceMessage);
        
        // 滚动到底部
        scrollToBottom();
        
        // 发送消息到后端
        const messageData = {
          content: recognizedText,
          senderType: 'user',
          isVoiceMessage: true,
          voiceDuration: Math.round(audioBlob.duration || 0)
        };
        
        try {
          await conversationAPI.addMessage(currentConversation.value.id, messageData);
          // 重新获取消息列表，确保数据同步
          await fetchMessages();
        } catch (error) {
          console.error('发送消息失败:', error);
          ElMessage.warning('消息已显示但发送失败，将在网络恢复后自动重试');
        }
        
      } catch (error) {
        console.error('语音识别失败:', error);
        ElMessage.error('语音识别失败，请重试');
      } finally {
        loading.value = false;
      }
    };
    
    // 播放语音消息
    const playVoiceMessage = async (message) => {
      try {
        // 如果消息有本地语音URL，直接播放
        if (message.voiceUrl) {
          const audio = new Audio(message.voiceUrl);
          await audio.play();
        } else {
          // 否则从后端获取语音文件
          const audioUrl = `/api/speech/${message.id}/play`;
          const audio = new Audio(audioUrl);
          await audio.play();
        }
      } catch (error) {
        console.error('播放语音失败:', error);
        ElMessage.error('播放语音失败');
      }
    };
    
    // 切换AI消息文字显示状态
    const toggleAiMessageText = (message) => {
      message.showText = !message.showText;
    };

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
  await fetchMessages()
  
  // 滚动到底部
  setTimeout(scrollToBottom, 100)
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
    
    // 滚动到底部
    setTimeout(scrollToBottom, 100)
    
    // 发送到后端
    await conversationAPI.addMessage(currentConversationId.value, {
      senderType: 'user',
      content
    })
    
    // 重新获取消息列表以确保数据同步
    await fetchMessages()
    
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败，请重试')
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
const handleLogout = async () => {
  try {
    // 调用退出登录API
    await userAPI.logout();
    
    // 清除本地存储
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    
    // 重定向到登录页面
    router.push('/login');
  } catch (error) {
    console.error('退出登录失败:', error);
  }
};

// 复制消息
const copyMessage = (content) => {
  navigator.clipboard.writeText(content)
    .then(() => {
      alert('复制成功')
    })
    .catch(err => {
      console.error('复制失败:', err)
      alert('复制失败，请手动复制')
    })
}

// 将文本转换为语音
const convertTextToVoice = async (message) => {
  try {
    // 调用文本转语音API
    const response = await speechAPI.textToSpeech({
      text: message.content,
      characterName: currentConversation?.characterName || ''
    })
    
    if (response && response.success) {
      // 更新消息状态
      message.hasConvertedToVoice = true
      
      // 显示成功提示
      alert('文本已成功转换为语音')
    } else {
      throw new Error('转换失败')
    }
  } catch (error) {
    console.error('文本转语音失败:', error)
    alert('文本转语音失败，请重试')
  }
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

/* 侧边栏样式 */
.sidebar {
  width: 300px;
  background-color: #f8f9fa;
  border-right: 1px solid #dee2e6;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #dee2e6;
}

.sidebar-header h2 {
  margin: 0;
  margin-bottom: 15px;
  font-size: 20px;
  color: #333;
}

.new-chat-button {
  width: 100%;
  padding: 10px 15px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.new-chat-button:hover {
  background-color: #45a049;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.conversation-item {
  padding: 15px;
  margin-bottom: 10px;
  background-color: white;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.conversation-item:hover {
  background-color: #f0f0f0;
}

.conversation-item.active {
  border-color: #4CAF50;
  background-color: #e8f5e9;
}

.conversation-title {
  font-weight: 500;
  color: #333;
  margin-bottom: 5px;
}

.conversation-character {
  font-size: 12px;
  color: #666;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #dee2e6;
}

.logout-button {
  width: 100%;
  padding: 10px 15px;
  background-color: transparent;
  color: #666;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.logout-button:hover {
  background-color: #f8f9fa;
  color: #333;
}

/* 主聊天区域样式 */
.main-chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}

.chat-header {
  padding: 20px;
  background-color: white;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.action-button {
  width: 36px;
  height: 36px;
  border: 1px solid #dee2e6;
  background-color: white;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
}

.action-button:hover {
  background-color: #f8f9fa;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-wrapper {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.message-wrapper.user {
  align-items: flex-end;
}

.message-wrapper.ai {
  align-items: flex-start;
}

.message {
  display: flex;
  gap: 12px;
  max-width: 70%;
  align-items: flex-start;
}

.message.ai .message-content {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  color: #333;
}

.message.user .message-content {
  background-color: #4CAF50;
  color: white;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f0f0;
  border-radius: 50%;
}

.message-content {
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

/* 语音消息样式 */
.message-voice {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  max-width: 200px;
}

.message-voice.user {
  background-color: #4CAF50;
  color: white;
}

.message-voice.ai {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  color: #333;
}

.voice-play-button {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border: none;
  background: none;
  color: inherit;
  cursor: pointer;
  font-size: 16px;
  border-radius: 8px;
  transition: background-color 0.2s;
  width: 100%;
  justify-content: flex-start;
}

.voice-play-button:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.voice-play-button:hover .voice-icon {
  transform: scale(1.1);
}

.voice-icon {
  font-size: 18px;
  transition: transform 0.2s;
}

.voice-duration {
  font-size: 14px;
  color: inherit;
  opacity: 0.9;
}

/* AI消息特有操作区 */
.ai-message-actions {
  display: flex;
  gap: 8px;
  margin-left: 52px;
}

.action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: 1px solid #e0e0e0;
  background-color: white;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn:hover {
  background-color: #f8f9fa;
  border-color: #4CAF50;
}

/* 聊天输入区域样式 */
.chat-input-area {
  padding: 20px;
  background-color: white;
  border-top: 1px solid #dee2e6;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.input-wrapper textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  min-height: 40px;
  max-height: 120px;
}

.input-wrapper textarea:focus {
  outline: none;
  border-color: #4CAF50;
}

.voice-button {
  width: 44px;
  height: 44px;
  border: 1px solid #dee2e6;
  background-color: white;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
}

.voice-button:hover {
  background-color: #f8f9fa;
  border-color: #4CAF50;
}

.send-button {
  padding: 0 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: background-color 0.2s;
}

.send-button:hover {
  background-color: #45a049;
}

.send-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.input-tip {
  text-align: center;
  font-size: 12px;
  color: #999;
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
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.modal-header {
  padding: 20px;
  border-bottom: 1px solid #dee2e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.close-button {
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  transition: color 0.2s;
}

.close-button:hover {
  color: #333;
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  padding: 20px;
  border-top: 1px solid #dee2e6;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-button {
  padding: 10px 20px;
  border: 1px solid #dee2e6;
  background-color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.cancel-button:hover {
  background-color: #f8f9fa;
}

.confirm-button {
  padding: 10px 20px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.confirm-button:hover {
  background-color: #45a049;
}

.confirm-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 角色选择样式 */
.character-list {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.character-item {
  flex: 1;
  min-width: 150px;
  padding: 15px;
  border: 2px solid #dee2e6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
}

.character-item:hover {
  border-color: #4CAF50;
  background-color: #f8f9fa;
}

.character-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 10px;
}

.character-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.character-description {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-bottom: 10px;
}

.character-item input[type="radio"] {
  position: absolute;
  top: 10px;
  right: 10px;
  transform: scale(1.2);
}

.custom-character-button {
  width: 100%;
  padding: 12px;
  border: 2px dashed #dee2e6;
  background-color: white;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.custom-character-button:hover {
  border-color: #4CAF50;
  color: #4CAF50;
}

/* 录音模态框样式 */
.recording-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
  font-size: 16px;
  color: #333;
}

.recording-dot {
  width: 12px;
  height: 12px;
  background-color: #f44336;
  border-radius: 50%;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  50% {
    transform: scale(1.2);
    opacity: 0.8;
  }
  100% {
    transform: scale(0.8);
    opacity: 1;
  }
}

.recording-duration {
  text-align: center;
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
  color: #333;
}

.recording-visualizer {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  margin-bottom: 20px;
}

.visualizer-bars {
  display: flex;
  align-items: flex-end;
  gap: 5px;
  height: 100%;
}

.bar {
  width: 8px;
  background-color: #4CAF50;
  border-radius: 4px;
  animation: sound-wave 1s infinite ease-in-out;
}

.bar:nth-child(1) { animation-delay: 0s; }
.bar:nth-child(2) { animation-delay: 0.1s; }
.bar:nth-child(3) { animation-delay: 0.2s; }
.bar:nth-child(4) { animation-delay: 0.3s; }
.bar:nth-child(5) { animation-delay: 0.4s; }

@keyframes sound-wave {
  0%, 100% { height: 20%; }
  50% { height: 100%; }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  
  .message {
    max-width: 85%;
  }
  
  .chat-header,
  .chat-input-area {
    padding: 15px;
  }
  
  .chat-messages {
    padding: 15px;
  }
}
</style>