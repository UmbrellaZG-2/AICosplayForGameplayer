<template>
  <div class="chat-message" :class="{
    'user-message': message.sender === 'user',
    'ai-message': message.sender === 'ai'
  }">
    <!-- 头像 -->
    <div class="message-avatar">
      <img 
        :src="getCharacterAvatar()" 
        :alt="message.sender === 'user' ? '用户' : getCharacterName()" 
        class="avatar-image"
        @error="handleAvatarError"
      >
    </div>
    
    <!-- 消息内容 -->
    <div class="message-content">
      <!-- 消息发送者名称 -->
      <div class="message-sender">{{ message.sender === 'user' ? '我' : getCharacterName() }}</div>
      
      <!-- 文本消息 -->
      <div v-if="message.type === 'text'" class="message-text">
        {{ message.content }}
      </div>
      
      <!-- 语音消息 -->
      <div v-else-if="message.type === 'voice'" class="voice-message">
        <div class="voice-controls">
          <button 
            class="play-voice-btn" 
            @click="toggleVoicePlayback"
            :disabled="isVoicePlaying"
          >
            {{ isVoicePlaying ? '⏸️' : '▶️' }}
          </button>
          <span class="voice-duration">{{ formatVoiceDuration(message.duration || 0) }}</span>
          
          <!-- 语音波形图 -->
          <div v-if="message.waveformData" class="voice-waveform">
            <canvas 
              ref="waveformCanvas"
              :width="200" 
              :height="40"
              class="waveform-canvas"
            ></canvas>
          </div>
        </div>
        
        <!-- 语音识别文本 -->
        <div v-if="message.recognizedText" class="recognized-text">
          {{ message.recognizedText }}
          <button 
            class="copy-text-btn" 
            @click="copyRecognizedText"
            title="复制文本"
          >
            📋
          </button>
        </div>
      </div>
      
      <!-- 消息时间 -->
      <div class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'

// Props
const props = defineProps({
  message: {
    type: Object,
    required: true
  },
  character: {
    type: Object,
    default: null
  }
})

// Emits
const emit = defineEmits(['playVoice', 'stopVoice'])

// 状态管理
const isVoicePlaying = ref(false)
const waveformCanvas = ref(null)

// 获取角色头像
const getCharacterAvatar = () => {
  if (props.message.sender === 'user') {
    // 用户头像可以从用户信息中获取，这里使用默认头像
    return '/user-avatar.png' // 实际项目中应该替换为真实的用户头像路径
  }
  
  // AI角色头像
  if (props.character) {
    // 尝试获取角色头像，优先使用png格式，如果不存在则尝试jpg格式
    return `/Character/${props.character.name}.jpg`
  }
  
  // 默认AI助手头像
  return '/Character/AI助手.jpg'
}

// 获取角色名称
const getCharacterName = () => {
  if (props.character) {
    return props.character.name
  }
  return 'AI助手'
}

// 处理头像加载失败
const handleAvatarError = (event) => {
  if (event && event.target) {
    const img = event.target;
    
    // 检查是否已经尝试过两种格式
    if (!img.dataset.triedBothFormats) {
      // 如果当前是jpg格式，尝试png格式；如果是png格式，尝试jpg格式
      const currentSrc = img.src;
      let newSrc = currentSrc;
      
      if (currentSrc.endsWith('.jpg')) {
        newSrc = currentSrc.replace('.jpg', '.png');
      } else if (currentSrc.endsWith('.png')) {
        newSrc = currentSrc.replace('.png', '.jpg');
      }
      
      // 添加标记表示已经尝试过两种格式
      img.dataset.triedBothFormats = 'true';
      
      // 尝试加载新格式
      img.src = newSrc;
    } else {
      // 如果两种格式都尝试过仍然失败，使用默认头像
      console.log('头像加载失败，使用默认头像');
      img.style.border = '2px solid red'; // 添加红色边框表示加载失败
      img.src = '/default-avatar.png'; // 默认头像路径
    }
  }
}

// 格式化语音时长
const formatVoiceDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${mins}:${secs.toString().padStart(2, '0')}`
}

// 格式化消息时间
const formatMessageTime = (timestamp) => {
  if (!timestamp) return ''
  
  const date = new Date(timestamp)
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  
  return `${hours}:${minutes}`
}

// 切换语音播放状态
const toggleVoicePlayback = () => {
  if (isVoicePlaying.value) {
    stopVoicePlayback()
  } else {
    playVoiceMessage()
  }
}

// 播放语音消息
const playVoiceMessage = () => {
  isVoicePlaying.value = true
  emit('playVoice', props.message)
}

// 停止语音播放
const stopVoicePlayback = () => {
  isVoicePlaying.value = false
  emit('stopVoice')
}

// 复制识别的文本
const copyRecognizedText = () => {
  if (props.message.recognizedText) {
    navigator.clipboard.writeText(props.message.recognizedText)
      .then(() => {
        // 可以添加复制成功的提示
        console.log('文本已复制到剪贴板')
      })
      .catch(err => {
        console.error('复制失败:', err)
      })
  }
}

// 绘制语音波形图
const drawWaveform = () => {
  if (!waveformCanvas.value || !props.message.waveformData) return
  
  const canvas = waveformCanvas.value
  const ctx = canvas.getContext('2d')
  const data = props.message.waveformData
  
  // 清空画布
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  
  // 设置样式
  ctx.fillStyle = isVoicePlaying.value ? '#4CAF50' : '#2196F3'
  
  const barWidth = canvas.width / data.length
  const barHeight = canvas.height / 2
  
  // 绘制波形
  for (let i = 0; i < data.length; i++) {
    const height = (data[i] / 255) * barHeight
    const x = i * barWidth
    const y = barHeight - height / 2
    
    ctx.fillRect(x, y, barWidth * 0.8, height)
  }
}

// 当组件挂载后，如果有波形数据，则绘制波形图
onMounted(async () => {
  await nextTick()
  drawWaveform()
})

// 监听isVoicePlaying的变化，更新波形图颜色
const watch = (await import('vue')).watch
watch(() => isVoicePlaying.value, () => {
  drawWaveform()
})

// 暴露方法给父组件
defineExpose({
  stopVoicePlayback,
  isVoicePlaying
})
</script>

<style scoped>
.chat-message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.user-message {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin: 0 12px;
  flex-shrink: 0;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: 70%;
  display: flex;
  flex-direction: column;
}

.user-message .message-content {
  align-items: flex-end;
}

.ai-message .message-content {
  align-items: flex-start;
}

.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.message-text {
  background-color: #f0f0f0;
  padding: 10px 14px;
  border-radius: 18px;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.user-message .message-text {
  background-color: #2196F3;
  color: white;
}

.ai-message .message-text {
  background-color: #f5f5f5;
  color: #333;
}

.voice-message {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.voice-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f0f0f0;
  border-radius: 18px;
}

.user-message .voice-controls {
  background-color: #2196F3;
  color: white;
}

.play-voice-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.play-voice-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.voice-duration {
  font-size: 14px;
  min-width: 40px;
}

.voice-waveform {
  flex: 1;
  display: flex;
  align-items: center;
}

.waveform-canvas {
  width: 100%;
  height: 40px;
}

.recognized-text {
  font-size: 14px;
  color: #666;
  padding: 4px 8px;
  background-color: rgba(0, 0, 0, 0.05);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.copy-text-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  opacity: 0.6;
  padding: 2px;
}

.copy-text-btn:hover {
  opacity: 1;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
}
</style>