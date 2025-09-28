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
                :src="getCharacterAvatar() || ''" 
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
                  :src="getCharacterAvatar() || ''" 
                  :alt="getCharacterName()"
                  @error="handleAvatarError"
                >
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
                  :src="getCharacterAvatar() || ''" 
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
              class="record-btn"
              :class="{ recording: isRecording }"
              :disabled="isAIThinking"
              @click="toggleRecording"
              title="录制语音"
            >
              {{ isRecording ? '⏹️' : '🎤' }}
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
import { getCharacterAvatarPath, getCharacterAvatarPathWithExtension, getCharacterAvatarPathSync, addAvatarPathsToCharacters } from '../utils/characterUtils.js'

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
const isRecording = ref(false)
const recordingDuration = ref(0)
const maxRecordingDuration = 60 // 最大录音时长60秒
const isSidebarCollapsed = ref(false)
const recognition = ref(null)
const mediaRecorder = ref(null)
const audioChunks = ref([])
const audioContext = ref(null)
const analyser = ref(null)
const dataArray = ref(null)
const canvasContext = ref(null)
const animationId = ref(null)
const timerInterval = ref(null)
const isMobile = ref(window.innerWidth <= 768)
const chatMessagesRef = ref(null)
const isAIThinking = ref(false)
// 语音播放相关状态
const playingVoiceId = ref(null)
const showTextMap = ref({})
const audioPlayers = ref({}) // 存储音频播放器实例
// 存储当前角色头像路径的响应式变量
const currentCharacterAvatar = ref(null)

// 当前录音媒体流
const currentStream = ref(null)

// 格式化语音时长
const formatVoiceDuration = (seconds) => {
  const mins = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return `${mins}:${secs.toString().padStart(2, '0')}`;
}

// 播放语音消息
const playVoiceMessage = async (message) => {
  try {
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
      const audioData = await speechAPI.getSpeechData(message.voiceId);
      const blob = new Blob([audioData], { type: 'audio/wav' });
      const audioUrl = URL.createObjectURL(blob);
      
      // 创建并播放音频
      const audio = new Audio(audioUrl);
      audioPlayers.value[message.id] = audio;
      
      audio.onended = () => {
        stopVoicePlayback(message.id);
        URL.revokeObjectURL(audioUrl);
      };
      
      await audio.play();
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
    console.error('Failed to load avatar:', event.target.src);
    // 打印当前页面URL和路径，用于调试
    console.log('Current page URL:', window.location.href);
    console.log('Current page path:', window.location.pathname);
    console.log('Character name:', getCharacterName());
    console.log('Is URL encoded?', event.target.src.includes('%'));
    
    // 尝试修复路径 - 如果路径中包含中文字符，确保正确编码
    try {
      const originalSrc = event.target.src;
      // 检查是否是相对路径且不以/开头
      if (!originalSrc.startsWith('http') && !originalSrc.startsWith('/')) {
        const basePath = window.location.pathname.endsWith('/') ? window.location.pathname : window.location.pathname.substring(0, window.location.pathname.lastIndexOf('/') + 1);
        const newSrc = basePath + originalSrc;
        console.log('Attempting to fix avatar path from', originalSrc, 'to', newSrc);
        event.target.src = newSrc;
        // 重新加载图片，只尝试一次
        if (!event.target.hasAttribute('data-fixed')) {
          event.target.setAttribute('data-fixed', 'true');
          event.target.onload = null;
          event.target.onerror = (e) => {
            console.error('Failed to load avatar even after path fix:', e.target.src);
            // 头像加载失败时显示友好错误，不再抛出异常
            console.log('显示默认头像替代');
          };
          return;
        }
      } else if (originalSrc.startsWith('/Character/')) {
        // 对于/Character/开头的路径，尝试使用encodeURIComponent编码角色名
        const characterName = originalSrc.replace('/Character/', '').split('.')[0];
        const extension = originalSrc.includes('.') ? originalSrc.substring(originalSrc.lastIndexOf('.')) : '.jpg';
        const encodedPath = '/Character/' + encodeURIComponent(characterName) + extension;
        console.log('Attempting encoded avatar path:', encodedPath);
        if (!event.target.hasAttribute('data-fixed')) {
          event.target.setAttribute('data-fixed', 'true');
          event.target.onload = null;
          event.target.onerror = (e) => {
            console.error('Failed to load avatar with encoded path:', e.target.src);
            // 头像加载失败时显示友好错误，不再抛出异常
            console.log('显示默认头像替代');
          };
          event.target.src = encodedPath;
          return;
        }
      }
    } catch (err) {
      console.error('Error in avatar path fix:', err);
    }
    
    // 头像加载失败时不再抛出错误，而是使用默认头像
    console.log('Avatar failed to load, using default avatar');
  }
}

// 更新当前角色头像路径
const updateCurrentCharacterAvatar = () => {
  console.log('----- updateCurrentCharacterAvatar 开始 -----');
  
  if (!currentConversation.value?.characterId) {
    console.log('No character ID in current conversation');
    currentCharacterAvatar.value = null;
    return;
  }
  
  const character = getCharacterById(currentConversation.value.characterId);
  if (!character?.name) {
    console.log('No character name found for ID:', currentConversation.value.characterId);
    currentCharacterAvatar.value = null;
    return;
  }
  
  console.log('Updating avatar for character:', character.name);
  
  // 检查角色名是否包含中文字符
  const hasChineseChars = /[\u4e00-\u9fa5]/.test(character.name);
  console.log('Character name has Chinese characters:', hasChineseChars);
  
  // 获取头像路径，确保路径格式正确
  let avatarPath = null;
  
  if (character.avatar) {
    console.log('Using character\'s avatar property:', character.avatar);
    avatarPath = character.avatar;
  } else {
    // 生成默认头像路径作为后备
    console.log('Generating default avatar path');
    avatarPath = getCharacterAvatarPathSync(character.name, 'jpg');
    console.log('Generated default avatar path:', avatarPath);
  }
  
  // 确保头像路径以斜杠开头，避免相对路径问题
  if (avatarPath && !avatarPath.startsWith('http') && !avatarPath.startsWith('/')) {
    console.log('Fixing path format: adding leading slash');
    avatarPath = '/' + avatarPath;
  }
  
  // 检查路径是否编码
  const isEncoded = avatarPath.includes('%');
  console.log('Avatar path is encoded:', isEncoded);
  
  currentCharacterAvatar.value = avatarPath;
  console.log('Final avatar path set:', currentCharacterAvatar.value);
  
  // 预加载头像图片，提高用户体验
  if (avatarPath) {
    const img = new Image();
    img.src = avatarPath;
    img.onload = () => {
      console.log('Avatar preloaded successfully:', avatarPath);
    };
    img.onerror = (err) => {
      console.error('Failed to preload avatar:', avatarPath, err);
      // 添加备用编码路径尝试
      const encodedPath = avatarPath.split('/').map(part => {
        // 只对包含中文字符的部分进行编码
        if (/[\u4e00-\u9fa5]/.test(part)) {
          return encodeURIComponent(part);
        }
        return part;
      }).join('/');
      
      if (encodedPath !== avatarPath) {
        console.log('Trying encoded path as fallback:', encodedPath);
        const encodedImg = new Image();
        encodedImg.src = encodedPath;
        encodedImg.onload = () => {
          console.log('Encoded avatar path loaded successfully');
          currentCharacterAvatar.value = encodedPath;
        };
        encodedImg.onerror = (err) => {
          console.error('Failed to load with encoded path:', encodedPath, err);
        };
      }
    };
  }
  
  console.log('----- updateCurrentCharacterAvatar 结束 -----');
}

// 获取角色头像
const getCharacterAvatar = () => {
  // 如果没有头像，返回null
  if (!currentCharacterAvatar.value) {
    console.log('getCharacterAvatar: 没有头像路径');
    return null;
  }
  
  // 确保返回的路径格式正确
  if (!currentCharacterAvatar.value.startsWith('http') && 
      !currentCharacterAvatar.value.startsWith('/')) {
    // 修复路径，确保以斜杠开头
    const fixedPath = '/' + currentCharacterAvatar.value;
    console.log('getCharacterAvatar: 修复路径格式:', currentCharacterAvatar.value, '->', fixedPath);
    return fixedPath;
  }
  console.log('getCharacterAvatar: 返回路径:', currentCharacterAvatar.value);
  return currentCharacterAvatar.value;
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
  // 更新当前角色头像
  await updateCurrentCharacterAvatar()
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
    messages.value = response.data || []
    
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
      
      // 检查是否有AI回复
      if (response.data && response.data.reply) {
        messages.value.push(response.data.reply)
      }
    }
  } catch (error) {
    console.error('发送消息失败:', error)
    
    // 显示错误提示
    alert('发送消息失败，请稍后重试')
    
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

// 切换录音状态
const toggleRecording = async () => {
  if (isRecording.value) {
    // 停止录音
    stopRecording()
  } else {
    // 开始录音
    await startRecording()
  }
}

// 开始录音
const startRecording = async () => {
  try {
    // 获取用户媒体权限
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    
    // 初始化音频上下文和声纹分析
    audioContext.value = new (window.AudioContext || window.webkitAudioContext)()
    analyser.value = audioContext.value.createAnalyser()
    const source = audioContext.value.createMediaStreamSource(stream)
    source.connect(analyser.value)
    
    analyser.value.fftSize = 256
    const bufferLength = analyser.value.frequencyBinCount
    dataArray.value = new Uint8Array(bufferLength)
    
    // 设置canvas用于声纹显示
    const canvas = document.getElementById('audio-visualizer')
    if (canvas) {
      canvasContext.value = canvas.getContext('2d')
      // 开始绘制声纹
      drawWaveform()
    }
    
    // 初始化媒体记录器
    // 检测浏览器支持的音频格式，优先选择wav，否则使用默认格式
    let mimeType = 'audio/wav'
    if (!MediaRecorder.isTypeSupported(mimeType)) {
      // 尝试其他常见格式
      const supportedTypes = ['audio/webm', 'audio/ogg', 'audio/mp4']
      for (const type of supportedTypes) {
        if (MediaRecorder.isTypeSupported(type)) {
          mimeType = type
          break
        }
      }
    }
    
    mediaRecorder.value = new MediaRecorder(stream, { mimeType })
    audioChunks.value = []
    
    // 保存stream到ref中以确保在onstop回调中可以访问
    currentStream.value = stream
    
    mediaRecorder.value.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.value.push(event.data)
      }
    }
    
    mediaRecorder.value.onstop = async () => {
      // 停止声纹绘制
      if (animationId.value) {
        cancelAnimationFrame(animationId.value)
      }
      
      // 停止计时器
      if (timerInterval.value) {
        clearInterval(timerInterval.value)
        timerInterval.value = null
      }
      
      isRecording.value = false
      
      // 如果有录音数据，处理并发送
      if (audioChunks.value.length > 0) {
        console.log('录音数据块数量:', audioChunks.value.length)
        console.log('录音持续时间:', recordingDuration.value, '秒')
        
        // 使用媒体记录器的实际mimeType创建Blob
        const audioBlob = new Blob(audioChunks.value, { type: mediaRecorder.value.mimeType })
        console.log('创建的音频Blob:', audioBlob.size, '字节, 类型:', audioBlob.type)
        
        // 处理录音数据
        transcribeAudio(audioBlob)
      } else {
        console.error('没有录音数据，可能是录音时间太短或麦克风未接收到声音')
        alert('未检测到声音，请确保麦克风正常工作且音量足够')
      }
      
      // 关闭媒体流
      if (currentStream.value) {
        currentStream.value.getTracks().forEach(track => track.stop())
        currentStream.value = null
      }
      
      // 清理DOM元素
      const recordingOverlay = document.getElementById('recording-overlay')
      if (recordingOverlay) {
        recordingOverlay.remove()
      }
    }
    
    // 开始录音
    mediaRecorder.value.start()
    isRecording.value = true
    
    // 显示录音提示和倒计时
    showRecordingOverlay()
    
    // 启动计时器
    startTimer()
    
    // 设置60秒自动停止计时器
    setTimeout(() => {
      if (isRecording.value) {
        stopRecording()
        // 自动发送消息
        if (inputMessage.value.trim()) {
          sendMessage()
        }
      }
    }, maxRecordingDuration * 1000)
    
  } catch (error) {
    console.error('开始录音失败:', error)
    alert('获取麦克风权限失败，请确保已授予权限')
  }
}

// 停止录音
const stopRecording = () => {
  // 停止计时器
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  
  if (mediaRecorder.value && mediaRecorder.value.state !== 'inactive') {
    mediaRecorder.value.stop()
  }
}

// 显示录音覆盖层
const showRecordingOverlay = () => {
  // 检查是否已存在覆盖层
  if (document.getElementById('recording-overlay')) {
    return
  }
  
  const overlay = document.createElement('div')
  overlay.id = 'recording-overlay'
  overlay.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.7);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    color: white;
    font-size: 18px;
  `
  
  const message = document.createElement('div')
  message.textContent = '录音已开始'
  message.style.marginBottom = '10px'
  
  // 添加计时器显示
  const timerDisplay = document.createElement('div')
  timerDisplay.id = 'recording-timer'
  timerDisplay.textContent = '00:00'
  timerDisplay.style.marginBottom = '20px'
  timerDisplay.style.fontSize = '24px'
  timerDisplay.style.fontWeight = 'bold'
  
  const canvas = document.createElement('canvas')
  canvas.id = 'audio-visualizer'
  canvas.width = 300
  canvas.height = 100
  canvas.style.marginBottom = '20px'
  canvas.style.backgroundColor = 'rgba(255, 255, 255, 0.1)'
  canvas.style.borderRadius = '8px'
  
  const stopButton = document.createElement('button')
  stopButton.textContent = '结束录音'
  stopButton.style.cssText = `
    background-color: #f44336;
    color: white;
    border: none;
    border-radius: 6px;
    padding: 10px 20px;
    font-size: 16px;
    cursor: pointer;
  `
  
  stopButton.addEventListener('click', stopRecording)
  
  overlay.appendChild(message)
  overlay.appendChild(timerDisplay)
  overlay.appendChild(canvas)
  overlay.appendChild(stopButton)
  
  document.body.appendChild(overlay)
}

// 启动计时器
const startTimer = () => {
  recordingDuration.value = 0
  
  // 清除可能存在的旧计时器
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
  }
  
  // 创建新的计时器
  timerInterval.value = setInterval(() => {
    recordingDuration.value++
    
    // 更新计时器显示
    const timerElement = document.getElementById('recording-timer')
    if (timerElement) {
      const minutes = Math.floor(recordingDuration.value / 60).toString().padStart(2, '0')
      const seconds = (recordingDuration.value % 60).toString().padStart(2, '0')
      timerElement.textContent = `${minutes}:${seconds}`
    }
    
    // 如果达到最大录音时长，自动停止
    if (recordingDuration.value >= maxRecordingDuration) {
      stopRecording()
    }
  }, 1000)
}

// 绘制音频波形
const drawWaveform = () => {
  if (!analyser.value || !canvasContext.value) return
  
  const canvas = canvasContext.value.canvas
  const WIDTH = canvas.width
  const HEIGHT = canvas.height
  
  // 确保analyser有正确的参数设置
  analyser.value.fftSize = 256
  const bufferLength = analyser.value.frequencyBinCount
  dataArray.value = new Uint8Array(bufferLength)
  
  const draw = () => {
    animationId.value = requestAnimationFrame(draw)
    
    // 获取当前的音频数据
    analyser.value.getByteFrequencyData(dataArray.value)
    
    // 清空canvas
    canvasContext.value.fillStyle = 'rgba(255, 255, 255, 0.1)'
    canvasContext.value.fillRect(0, 0, WIDTH, HEIGHT)
    
    // 绘制音频波形
    const barWidth = (WIDTH / dataArray.value.length) * 2.5
    let x = 0
    
    for (let i = 0; i < dataArray.value.length; i++) {
      const barHeight = (dataArray.value[i] / 255) * HEIGHT
      
      // 根据音频强度动态调整颜色
      canvasContext.value.fillStyle = `rgb(${255}, ${100 + barHeight}, ${100})`
      canvasContext.value.fillRect(x, HEIGHT - barHeight, barWidth, barHeight)
      
      x += barWidth + 1
    }
  }
  
  draw()
}

// 音频转文字并发送消息
const transcribeAudio = async (audioBlob) => {
  let tempMessage = null
  try {
    // 显示正在处理提示
    tempMessage = {
      id: Date.now(),
      content: `语音${recordingDuration.value}秒`,
      sender: 'user',
      type: 'voice',
      duration: recordingDuration.value,
      createdAt: new Date().toISOString()
    }
    messages.value.push(tempMessage)
    scrollToBottom()

    // 发送音频到后端进行语音识别
    console.log('正在发送音频进行识别，大小:', audioBlob.size, '类型:', audioBlob.type)
    const recognitionResult = await speechAPI.recognize(audioBlob)
    console.log('语音识别API返回结果:', recognitionResult)
    
    // 适配不同的数据格式，确保能获取到识别文本
    let textContent = ''
    if (recognitionResult && recognitionResult.data) {
      // 尝试从不同的字段获取识别文本
      if (typeof recognitionResult.data === 'string') {
        // 如果直接是字符串，尝试解析JSON
        try {
          const parsed = JSON.parse(recognitionResult.data)
          textContent = parsed.text || parsed.result || parsed || ''
        } catch (e) {
          textContent = recognitionResult.data
        }
      } else if (typeof recognitionResult.data === 'object') {
        // 如果是对象，尝试从不同字段获取
        textContent = recognitionResult.data.text || recognitionResult.data.result || ''
      }
    }
    
    textContent = textContent.trim()
    console.log('提取的识别文本:', textContent)
    
    if (!textContent) {
      console.error('语音识别结果为空')
      alert('无法识别语音内容，请重试')
      // 移除临时消息
      const index = messages.value.findIndex(msg => msg.id === tempMessage.id)
      if (index !== -1) {
        messages.value.splice(index, 1)
      }
      return
    }

    // 更新临时消息，添加识别出的文本
    const updatedMessage = {
      ...tempMessage,
      recognizedText: textContent
    }
    const index = messages.value.findIndex(msg => msg.id === tempMessage.id)
    if (index !== -1) {
      messages.value[index] = updatedMessage
    }

    // 调用API发送识别出的文本消息
    if (currentConversationId.value) {
      const response = await conversationAPI.addMessage(currentConversationId.value, {
        content: textContent,
        sender: 'user',
        type: 'voice'
      })

      // 检查响应是否正常
      if (response && response.code === 200 && response.data) {
        // 用服务器返回的消息替换本地临时消息
        const msgIndex = messages.value.findIndex(msg => msg.id === tempMessage.id)
        if (msgIndex !== -1) {
          messages.value[msgIndex] = {
            ...response.data,
            recognizedText: textContent,
            type: 'voice',
            duration: recordingDuration.value,
            sender: 'user' // 明确设置为用户
          }
        }

        // 检查是否有AI回复
        if (response.data && response.data.reply) {
          messages.value.push(response.data.reply)
        }
      }
    }
  } catch (error) {
    console.error('语音处理失败:', error)
    alert('语音处理失败，请重试')
    // 移除临时消息
    if (tempMessage) {
      const index = messages.value.findIndex(msg => msg.id === tempMessage.id)
      if (index !== -1) {
        messages.value.splice(index, 1)
      }
    }
  } finally {
    // 重置录音时长
    recordingDuration.value = 0
  }
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

// 加载角色信息（模仿RoleSelectionPage.vue的处理方式，但只加载一个角色）
const loadCharacters = async () => {
  try {
    // 检查localStorage中是否有已选择的角色信息
    const selectedRoleName = localStorage.getItem('selectedRoleName');
    const selectedRoleId = localStorage.getItem('selectedRoleId');
    let characterData = [];
    
    // 首先尝试获取所有角色（模仿RoleSelectionPage.vue的方式）
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
      
      // 使用工具函数为所有角色添加头像路径
      characters.value = addAvatarPathsToCharacters(characterData);
      
      // 如果有当前对话，更新头像
      if (currentConversation.value?.characterId) {
        await updateCurrentCharacterAvatar();
      }
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