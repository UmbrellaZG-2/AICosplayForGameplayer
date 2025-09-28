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
            <div class="conversation-time">{{ formatMessageTime(conversation.updatedAt) }}</div>
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
        <div class="user-info" @click="navigateToProfile">
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
    
    <!-- 侧边栏遮罩层 -->
    <div 
      class="sidebar-overlay" 
      :class="{ show: isMobile.value && !isSidebarCollapsed }"
      @click="toggleSidebar"
    ></div>
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
              <span v-else-if="getCharacterName()">🤖</span>
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
                <span v-else-if="getCharacterName()">🤖</span>
                <span v-else>🤖</span>
              </div>
          </div>
          <div class="message-content">
            <!-- 文本消息 -->
            <template v-if="message.type !== 'voice'">
              <div class="message-text">{{ message.content }}</div>
              <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
            </template>
            
            <!-- 语音消息 -->
            <template v-else>
              <div class="voice-message-container">
                <button 
                  class="play-voice-btn" 
                  @click="playVoiceMessage(message)"
                  :class="{ playing: playingVoiceId === message.id }"
                  title="播放语音"
                >
                  {{ playingVoiceId === message.id ? '⏸️' : '▶️' }}
                </button>
                <div class="voice-duration">{{ formatVoiceDuration(message.duration) }}</div>
                <div class="voice-wave"></div>
              </div>
              <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
              
              <!-- 显示文字按钮和识别出的文本 -->
              <button 
                v-if="message.recognizedText || message.textContent"
                class="show-text-btn" 
                @click="toggleShowText(message.id)"
              >
                文字 {{ showTextMap[message.id] ? '▼' : '▶' }}
              </button>
              <div 
                v-if="(message.recognizedText || message.textContent) && showTextMap[message.id]"
                class="recognized-text"
              >
                {{ message.recognizedText || message.textContent }}
              </div>
            </template>
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
                <span v-else-if="getCharacterName()">🤖</span>
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
            <RecordingManager
              ref="recordingManagerRef"
              :is-ai-thinking="isAIThinking"
              @recording-complete="handleRecordingComplete"
            />
            <button 
              class="record-btn"
              :disabled="isAIThinking"
              @click="startRecording"
              title="启动录音"
            >
              <span>🎤 录音</span>
            </button>
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
          Enter 发送，Shift + Enter 换行，🎤 录制语音
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { conversationAPI, gameCharacterAPI, userAPI, speechAPI } from '../utils/api.js'
import ChatMessage from '../components/ChatMessage.vue'
import RecordingManager from '../components/RecordingManager.vue'
import CharacterSelector from '../components/CharacterSelector.vue'
import ConversationList from '../components/ConversationList.vue'

const router = useRouter()

// 状态管理
const messages = ref([])
const currentConversation = ref(null)
const currentConversationId = ref(null)
const conversations = ref([])
const characters = ref([])
const inputMessage = ref('')
const isSending = ref(false)
const username = ref('用户')
const isLoading = ref(false)
const isSidebarCollapsed = ref(false)
const recognition = ref(null)
const isMobile = ref(window.innerWidth <= 768)
const chatMessagesRef = ref(null)
const isAIThinking = ref(false)
// 语音播放相关状态
const playingVoiceId = ref(null)
const showTextMap = ref({})
const audioPlayers = ref({}) // 存储音频播放器实例
// 录音管理器引用
const recordingManagerRef = ref(null)

// 处理录音完成事件
const handleRecordingComplete = async (audioBlob, duration) => {
  try {
    if (!currentConversationId.value) {
      console.error('没有选择对话')
      alert('请先选择或创建一个对话')
      return
    }

    // 验证音频数据
    if (!audioBlob || audioBlob.size === 0) {
      console.error('无效的音频数据')
      alert('录音失败，请重试')
      return
    }

    console.log('录音完成，音频大小:', audioBlob.size, '字节，时长:', duration, '秒')

    // 创建临时消息
    const tempMessage = {
      id: Date.now(),
      type: 'voice',
      duration: Math.round(duration),
      sender: 'user',
      senderType: 1,
      createdAt: new Date().toISOString(),
      status: 'sending' // 添加状态标识
    }

    // 添加到本地消息列表
    messages.value.push(tempMessage)
    scrollToBottom()

    // 准备发送音频数据
    const formData = new FormData()
    formData.append('audio', audioBlob)
    formData.append('conversationId', currentConversationId.value)
    formData.append('sender', 'user')

    console.log('准备发送语音消息到对话:', currentConversationId.value)

    // 调用API发送语音消息
    try {
      const response = await speechAPI.transcribeAudio(formData)
      
      console.log('语音消息发送成功，响应:', response)
      
      if (response && response.code === 200 && response.data) {
        // 用服务器返回的消息替换本地临时消息
        const index = messages.value.findIndex(msg => msg.id === tempMessage.id)
        if (index !== -1) {
          messages.value[index] = {
            ...response.data,
            status: 'sent' // 更新状态
          }
        }

        // 主动获取完整的消息列表，确保包含AI回复
        try {
          const messagesResponse = await conversationAPI.getMessages(currentConversationId.value)
          if (messagesResponse && messagesResponse.code === 200 && messagesResponse.data) {
            messages.value = messagesResponse.data
          }
        } catch (getMessagesError) {
          console.error('获取消息列表失败:', getMessagesError)
          // 不影响主流程，继续执行
        }
      } else {
        throw new Error('服务器返回无效响应: ' + JSON.stringify(response))
      }
    } catch (apiError) {
      console.error('语音消息API调用失败:', apiError)
      
      // 为特定错误类型提供更友好的提示
      let errorMessage = '发送语音消息失败，请稍后重试'
      if (apiError.response && apiError.response.status === 400) {
        errorMessage = '语音数据格式不正确，请重新录音'
      } else if (apiError.response && apiError.response.status === 413) {
        errorMessage = '音频文件过大，请缩短录音时长'
      } else if (apiError.response && apiError.response.status === 500) {
        errorMessage = '服务器错误，请稍后再试'
      } else if (apiError.message && apiError.message.includes('Network Error')) {
        errorMessage = '网络连接失败，请检查网络后重试'
      }
      
      throw new Error(errorMessage)
    }
  } catch (error) {
    console.error('发送语音消息失败:', error)
    alert(error.message || '发送语音消息失败，请稍后重试')
    
    // 回滚：移除临时添加的消息或更新状态为失败
    const index = messages.value.findIndex(msg => msg.sender === 'user' && msg.type === 'voice')
    if (index !== -1) {
      if (messages.value[index].status === 'sending') {
        messages.value[index].status = 'failed' // 标记为发送失败
        // 可以考虑添加重试按钮等UI元素
      } else {
        // 如果还没有状态标记，直接移除
        messages.value.splice(index, 1)
      }
    }
  } finally {
    scrollToBottom()
  }
}

// 启动录音
const startRecording = () => {
  try {
    if (recordingManagerRef.value && typeof recordingManagerRef.value.startRecording === 'function') {
      recordingManagerRef.value.startRecording();
    } else {
      console.error('录音管理器未初始化或没有startRecording方法');
      alert('录音功能暂时不可用，请稍后重试');
    }
  } catch (error) {
    console.error('启动录音失败:', error);
    alert('启动录音失败，请稍后重试');
  }
}

// 格式化语音时长
const formatVoiceDuration = (seconds) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// 播放语音消息
const playVoiceMessage = async (message) => {
  try {
    // 首先验证消息对象
    if (!message || typeof message !== 'object') {
      console.error('播放语音失败: 无效的消息对象');
      alert('播放语音失败: 消息数据无效');
      return;
    }
    
    // 如果当前正在播放其他语音，先停止
    if (playingVoiceId.value && playingVoiceId.value !== message.id) {
      stopVoicePlayback();
    }
    
    // 如果当前是暂停状态，继续播放
    if (playingVoiceId.value === message.id) {
      stopVoicePlayback();
      return;
    }
    
    // 标记为正在播放
    playingVoiceId.value = message.id;
    
    // 如果是AI的语音消息，需要从服务器获取语音数据
    if (message.senderType !== 1) {
      // 验证voiceId是否存在且有效
      if (!message.voiceId) {
        console.error('播放语音失败: 语音ID不存在');
        alert('播放语音失败: 语音数据已丢失');
        playingVoiceId.value = null;
        return;
      }
      
      console.log('尝试获取语音数据，voiceId:', message.voiceId);
      
      try {
        const audioData = await speechAPI.getSpeechData(message.voiceId);
        
        // 检查返回的数据是否为空
        if (!audioData || audioData.byteLength === 0) {
          throw new Error('获取到的音频数据为空');
        }
        
        const blob = new Blob([audioData], { type: 'audio/wav' });
        const audioUrl = URL.createObjectURL(blob);
        
        console.log('成功获取语音数据，大小:', blob.size, '字节');
        
        // 创建并播放音频
        const audio = new Audio(audioUrl);
        audioPlayers.value[message.id] = audio;
        
        audio.onended = () => {
          stopVoicePlayback(message.id);
          URL.revokeObjectURL(audioUrl);
        };
        
        // 处理音频播放错误
        audio.onerror = (event) => {
          console.error('音频播放错误:', event);
          alert('音频文件格式不支持，请重试');
          stopVoicePlayback(message.id);
          URL.revokeObjectURL(audioUrl);
        };
        
        await audio.play();
      } catch (error) {
        console.error('获取语音数据失败:', error);
        // 为特定错误类型提供更友好的提示
        if (error.response && error.response.status === 400) {
          alert('语音数据请求无效，请确认语音ID是否正确');
        } else if (error.response && error.response.status === 404) {
          alert('语音数据未找到，可能已被删除');
        } else if (error.response && error.response.status === 500) {
          alert('服务器错误，请稍后再试');
        } else {
          alert('播放语音失败，请重试');
        }
        playingVoiceId.value = null;
        return;
      }
    } else {
      // 对于用户的语音消息，我们只模拟播放（因为实际音频没有保存）
      setTimeout(() => {
        stopVoicePlayback(message.id);
      }, message.duration * 1000);
    }
  } catch (error) {
    console.error('播放语音失败:', error);
    alert('播放语音失败，请重试');
    playingVoiceId.value = null;
  }
}

// 停止语音播放
const stopVoicePlayback = (voiceId = null) => {
  const idToStop = voiceId || playingVoiceId.value;
  
  if (audioPlayers.value[idToStop]) {
    audioPlayers.value[idToStop].pause();
    audioPlayers.value[idToStop].onended = null;
    delete audioPlayers.value[idToStop];
  }
  
  if (!voiceId || voiceId === playingVoiceId.value) {
    playingVoiceId.value = null;
  }
}

// 切换显示文本
const toggleShowText = (messageId) => {
  showTextMap.value[messageId] = !showTextMap.value[messageId];
}

// 切换侧边栏折叠状态
const toggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
  
  // 确保在移动端点击菜单按钮时侧边栏可以正常显示
  if (isMobile.value && !isSidebarCollapsed.value) {
    // 防止背景内容滚动
    document.body.style.overflow = 'hidden';
  } else if (isMobile.value && isSidebarCollapsed.value) {
    // 恢复背景内容滚动
    document.body.style.overflow = 'auto';
  }
}

// 处理窗口大小变化
const handleResize = () => {
  const wasMobile = isMobile.value
  isMobile.value = window.innerWidth <= 768
  
  // 只在从桌面切换到移动设备时设置初始状态，不覆盖用户的操作
  if (!wasMobile && isMobile.value) {
    isSidebarCollapsed.value = true
  }
}

// 添加事件监听器
window.addEventListener('resize', handleResize)

// 计算属性
const displayedMessages = computed(() => {
  return messages.value.map(msg => ({
    ...msg,
    timestamp: formatMessageTime(msg.createdAt),
    date: formatMessageDate(msg.createdAt)
  }))
})

// 格式化消息日期
const formatMessageDate = (dateString) => {
  const date = new Date(dateString)
  const now = new Date()
  const diffInTime = now - date
  const diffInDays = Math.floor(diffInTime / (1000 * 60 * 60 * 24))
  
  if (diffInDays === 0) {
    return '今天'
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
  if (event && event.target) {
    // 检查是否已经尝试过两种格式
    const hasTriedBothFormats = event.target.dataset.triedBothFormats === 'true';
    if (!hasTriedBothFormats) {
      console.log('尝试加载头像:', event.target.src);
      // 显示红色边框表示加载失败
      event.target.style.border = '3px solid red';
      // 尝试切换文件格式
      const currentSrc = event.target.src;
      const baseName = currentSrc.substring(0, currentSrc.lastIndexOf('.'));
      const newExt = currentSrc.endsWith('.jpg') ? '.png' : '.jpg';
      const newSrc = baseName + newExt;
      console.log('尝试使用不同格式:', newSrc);
      event.target.src = newSrc;
      event.target.dataset.triedBothFormats = 'true';
    } else {
      // 两种格式都尝试过了，记录实际的错误
      console.error('加载头像失败:', event.target.src);
    }
  }
}

// 获取角色头像 - 简化实现，直接返回路径
const getCharacterAvatar = () => {
  const characterName = getCharacterName();
  if (characterName && characterName !== 'AI助手') {
    // 直接返回简单的头像路径
    return `/Character/${encodeURIComponent(characterName)}.jpg`;
  }
  return null;
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
const createNewChat = () => {
  // 导航到角色选择页面
  router.push('/role-selection');
}

// 切换对话
const switchConversation = async (id) => {
  currentConversationId.value = id
  currentConversation.value = conversations.value.find(c => c.id === id)
  // 加载对话消息
  await loadMessages(id)
}

// 导航到用户个人资料页面
const navigateToProfile = () => {
  router.push('/profile')
}

// 加载消息
const loadMessages = async (conversationId) => {
  try {
    if (!conversationId) return
    
    // 调用后端API获取消息
    const response = await conversationAPI.getMessages(conversationId)
    
    // 处理从后端返回的消息数据，转换为前端所需的格式
    if (response.data && Array.isArray(response.data)) {
      messages.value = response.data.map(message => {
        // 转换后端的消息格式为前端组件可用的格式
        return {
          id: message.id,
          content: message.content,
          // 后端使用senderType(1-用户，2-AI)，前端使用sender(user/ai)
          sender: message.senderType === 1 ? 'user' : 'ai',
          senderType: message.senderType,
          // 处理语音消息相关字段
          type: message.isVoiceMessage === 1 ? 'voice' : 'text',
          duration: message.voiceDuration || 0,
          voiceId: message.voiceFilePath,
          // AI生成的语音消息，显示原始文本
          textContent: message.isVoiceMessage === 1 && message.senderType === 2 ? message.content : '',
          createdAt: message.createdAt
        }
      })
    } else {
      messages.value = []
    }
    
    // 滚动到底部
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
    // 检查是否是对话已删除的错误
    if (error.code === 1004 && error.message === 'Conversation has been deleted') {
      // 显示错误消息
      alert('该对话已被删除')
      // 如果当前选择的对话是这个已删除的对话，清除选择
      if (currentConversationId.value === conversationId) {
        currentConversationId.value = null
        messages.value = []
      }
      // 重新加载对话列表
      loadConversations()
    }
  }
}

// 发送消息
const sendMessage = async () => {
  // 验证输入
  if (!inputMessage.value.trim() || isSending.value) {
    return
  }
  
  if (!currentConversationId.value) {
    console.error('没有选择对话')
    return
  }
  
  // 在try-catch外部定义变量以确保作用域正确
  let userMessage = null
  const messageContent = inputMessage.value.trim()
  
  try {
    isSending.value = true
    
    // 添加用户消息到本地
    userMessage = {
      id: Date.now(), // 临时ID
      content: messageContent,
      sender: 'user',
      senderType: 1,
      createdAt: new Date().toISOString()
    }
    messages.value.push(userMessage)
    
    // 清空输入框
    inputMessage.value = ''
    
    // 滚动到底部
    scrollToBottom()
    
    // 调用API发送消息
    const response = await conversationAPI.addMessage(currentConversationId.value, {
      content: messageContent,
      sender: 'user'
    })
    
    // 检查响应是否正常
    if (response && response.code === 200 && response.data) {
      // 用服务器返回的消息替换本地临时消息
      const index = messages.value.findIndex(msg => msg.id === userMessage.id)
      if (index !== -1) {
        messages.value[index] = response.data
      }
      
      // 主动获取完整的消息列表，确保包含AI回复
      const messagesResponse = await conversationAPI.getMessages(currentConversationId.value)
      if (messagesResponse && messagesResponse.code === 200 && messagesResponse.data) {
        // 对消息进行格式转换，特别是处理语音消息
        if (Array.isArray(messagesResponse.data)) {
          messages.value = messagesResponse.data.map(message => {
            // 转换后端的消息格式为前端组件可用的格式
            return {
              id: message.id,
              content: message.content,
              // 后端使用senderType(1-用户，2-AI)，前端使用sender(user/ai)
              sender: message.senderType === 1 ? 'user' : 'ai',
              senderType: message.senderType,
              // 处理语音消息相关字段
              type: message.isVoiceMessage === 1 ? 'voice' : 'text',
              duration: message.voiceDuration || 0,
              voiceId: message.voiceFilePath,
              // AI生成的语音消息，显示原始文本
              textContent: message.isVoiceMessage === 1 && message.senderType === 2 ? message.content : '',
              createdAt: message.createdAt
            }
          })
        } else {
          messages.value = messagesResponse.data
        }
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    
    // 根据错误类型提供更具体的错误提示
    let errorMessage = '发送消息失败，请稍后重试'
    
    // 检查是否是DeepSeek API相关的错误
    if (error.code === 1004 && error.message && error.message.includes('与DeepSeek API通信时发生错误')) {
      errorMessage = 'AI服务配置错误: 请确保DeepSeek API密钥已正确配置'
    } else if (error.code === 401) {
      errorMessage = '认证失败: 请重新登录'
    } else if (error.message) {
      // 显示更具体的错误信息
      errorMessage = error.message
    }
    
    // 显示错误提示
    alert(errorMessage)
    
    // 回滚：移除临时添加的消息
    const index = messages.value.findIndex(msg => msg.sender === 'user' && msg.id === userMessage?.id)
    if (index !== -1) {
      messages.value.splice(index, 1)
    }
    
    // 恢复输入框内容
    if (userMessage) {
      inputMessage.value = userMessage.content
    }
  } finally {
    isSending.value = false
    scrollToBottom()
  }
}

// 处理发送失败
const handleSendFailed = () => {
  console.error('发送消息失败')
  alert('发送消息失败，请检查网络连接或稍后重试')
}


// 退出登录
const handleLogout = async () => {
  try {
    // 调用退出登录API
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

// 加载角色信息
const loadCharacters = async () => {
  try {
    // 检查localStorage中是否有已选择的角色信息
    const selectedRoleName = localStorage.getItem('selectedRoleName');
    const selectedRoleId = localStorage.getItem('selectedRoleId');
    let characterData = [];
    
    // 首先尝试获取所有角色
    const response = await gameCharacterAPI.getAll();
    
    if (response && response.code === 200 && response.data) {
      // 如果有选择的角色ID和/或角色名，尝试从所有角色中找到对应的角色
      if (selectedRoleId && selectedRoleName) {
        console.log('尝试找到选择的角色:', selectedRoleName);
        // 从所有角色中筛选出匹配的角色
        const selectedCharacter = response.data.find(character => 
          character.id === selectedRoleId || character.name === selectedRoleName
        );
        
        if (selectedCharacter) {
          characterData = [selectedCharacter];
          console.log('成功找到选择的角色');
        } else {
          console.log('未找到选择的角色，使用所有角色');
          characterData = response.data;
        }
      } else if (selectedRoleName) {
        // 只有角色名，尝试根据角色名查找
        console.log('尝试根据角色名找到选择的角色:', selectedRoleName);
        const selectedCharacter = response.data.find(character => 
          character.name === selectedRoleName
        );
        
        if (selectedCharacter) {
          characterData = [selectedCharacter];
          console.log('成功找到选择的角色');
        } else {
          console.log('未找到选择的角色，使用所有角色');
          characterData = response.data;
        }
      } else {
        // 没有选择的角色，使用所有角色
        console.log('没有选择的角色，使用所有角色');
        characterData = response.data;
      }
      
      characters.value = characterData;
    }
  } catch (error) {
    console.error('加载角色列表失败:', error);
    alert('加载角色列表失败，请稍后重试');
  }
}

// 初始化
onMounted(async () => {
  await loadConversations()
  await loadCharacters()
  await loadUserInfo()
  
  // 检查是否从角色选择页面返回并有选择的角色
  await checkSelectedRole()
  
  // 如果有对话，加载第一个对话的消息
  if (conversations.value.length > 0) {
    await switchConversation(conversations.value[0].id)
  }
})

// 检查是否有选择的角色并创建对话
const checkSelectedRole = async () => {
  const selectedRoleId = localStorage.getItem('selectedRoleId')
  const selectedRoleName = localStorage.getItem('selectedRoleName')
  
  if (selectedRoleId && selectedRoleName) {
    try {
      // 创建新对话
      const response = await conversationAPI.create(selectedRoleName, selectedRoleId)
      
      if (response && response.code === 200 && response.data) {
        // 清除本地存储中的角色信息
        localStorage.removeItem('selectedRoleId')
        localStorage.removeItem('selectedRoleName')
        
        // 更新当前对话
        currentConversationId.value = response.data.id
        currentConversation.value = response.data
        
        // 重新加载对话列表
        await loadConversations()
        
        // 加载新对话的消息
        await loadMessages(response.data.id)
      }
    } catch (error) {
      console.error('创建对话失败:', error)
    }
  }
}

// 加载用户信息
const loadUserInfo = async () => {
  try {
    // 首先检查本地存储是否有用户信息
    const storedUserInfo = localStorage.getItem('userInfo')
    if (storedUserInfo) {
      const userInfo = JSON.parse(storedUserInfo)
      username.value = userInfo.username || '用户'
      return
    }
    
    // 如果本地存储没有，从API获取
    const response = await userAPI.getUserInfo()
    if (response.code === 200 && response.data && response.data.username) {
      username.value = response.data.username
      // 将获取到的用户信息保存到localStorage
      localStorage.setItem('userInfo', JSON.stringify({ username: response.data.username }))
    }
  } catch (error) {
    console.error('加载用户信息失败:', error)
    // 如果失败，保持默认值
  }
}

// 删除指定对话
const deleteConversation = async (conversationId) => {
  try {
    // 询问用户确认删除
    if (!confirm('确定要删除这个对话吗？')) {
      return
    }
    
    // 调用API删除对话
    const response = await conversationAPI.delete(conversationId)
    
    if (response && response.code === 200) {
      // 从本地对话列表中移除
      const index = conversations.value.findIndex(c => c.id === conversationId)
      if (index !== -1) {
        conversations.value.splice(index, 1)
      }
      
      // 如果删除的是当前对话，清除当前对话信息
      if (currentConversationId.value === conversationId) {
        currentConversationId.value = null
        currentConversation.value = null
        messages.value = []
        
        // 如果还有其他对话，自动切换到第一个
        if (conversations.value.length > 0) {
          await switchConversation(conversations.value[0].id)
        }
      }
    }
  } catch (error) {
    console.error('删除对话失败:', error)
    alert('删除对话失败，请稍后重试')
  }
}

// 删除当前对话
const deleteCurrentConversation = async () => {
  if (!currentConversationId.value) {
    alert('没有选择要删除的对话')
    return
  }
  
  // 调用删除对话函数
  await deleteConversation(currentConversationId.value)
}
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
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #333;
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
  overflow: hidden;
}

.message-avatar .avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  gap: 8px;
}

.record-btn {
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.2s;
}

.record-btn:hover:not(:disabled) {
  background-color: #d32f2f;
}

.record-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.record-btn.recording {
  background-color: #ffeb3b;
  color: #333;
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
    position: fixed; /* 改为fixed定位，确保在最上层 */
    z-index: 1000; /* 增加z-index确保覆盖其他内容 */
    height: 100%;
    transition: transform 0.3s ease, width 0.3s ease; /* 添加transform过渡 */
  }
  
  .sidebar.collapsed {
    width: 0;
    overflow: hidden;
    transform: translateX(-100%); /* 添加transform确保完全移出屏幕 */
  }
  
  .sidebar:not(.collapsed) {
    transform: translateX(0); /* 确保显示时回到原位 */
  }
  
  /* 添加遮罩层 */
  .sidebar-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    z-index: 999;
    display: none;
  }
  
  .sidebar-overlay.show {
    display: block;
  }
}
</style>