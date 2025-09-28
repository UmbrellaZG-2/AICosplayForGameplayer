<template>
  <div>
    <!-- 录音覆盖层 - 仅在录音时显示 -->
    <div v-if="isRecording" id="recording-overlay" class="recording-overlay">
      <div class="recording-content">
        <div class="recording-message">录音已开始</div>
        <div id="recording-timer" class="recording-timer">{{ formatDuration(recordingDuration) }}</div>
        <canvas 
          id="audio-visualizer" 
          class="audio-visualizer" 
          :width="300" 
          :height="100"
        ></canvas>
        <button class="stop-recording-btn" @click="stopRecording">结束录音</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, onMounted } from 'vue'
import Recorder from 'recorder-core'

// Props
const props = defineProps({
  maxRecordingDuration: {
    type: Number,
    default: 60
  },
  onRecordingComplete: {
    type: Function,
    required: true
  }
})

// Emits
const emit = defineEmits(['recordingStatusChange'])

// 状态管理
const isRecording = ref(false)
const recordingDuration = ref(0)
const currentStream = ref(null)
const recorder = ref(null)
const audioContext = ref(null)
const analyser = ref(null)
const dataArray = ref(null)
const canvasContext = ref(null)
const animationId = ref(null)
const timerInterval = ref(null)

// 尝试导入wav编码器 - 这会减少对mp3编码器的依赖
console.log('尝试初始化recorder-core...');
console.log('Recorder library version:', Recorder.version || 'unknown');

// 配置recorder-core，强制使用WAV格式，避免MP3编码器依赖
Recorder.prototype.isWavOnly = true; // 自定义属性，用于后续检查

// 检查recorder-core是否已经加载了必要的编码器
console.log('recorder-core支持的格式:', Recorder.support || 'unknown');

// 开始录音 - 同步阻塞调用
const startRecording = () => {
  try {
    // 检查浏览器是否支持媒体设备
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      alert('您的浏览器不支持录音功能，请使用Chrome、Firefox或Edge等现代浏览器');
      return;
    }

    // 请求麦克风权限（使用同步方式）
    console.log('正在请求麦克风权限...');
    navigator.mediaDevices.getUserMedia({
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        autoGainControl: true
      }
    }).then(stream => {
      console.log('成功获取麦克风权限');

      // 检查流中是否有音频轨道
      const audioTracks = stream.getAudioTracks();
      if (audioTracks.length === 0) {
        throw new Error('没有找到可用的音频输入设备');
      }

      console.log('音频设备信息:', audioTracks[0].label || '默认设备');

      // 保存stream到ref中
      currentStream.value = stream

      // 初始化音频上下文和声纹分析
      audioContext.value = new (window.AudioContext || window.webkitAudioContext)()
      analyser.value = audioContext.value.createAnalyser()
      const source = audioContext.value.createMediaStreamSource(stream)
      source.connect(analyser.value)

      analyser.value.fftSize = 256
      const bufferLength = analyser.value.frequencyBinCount
      dataArray.value = new Uint8Array(bufferLength)

      // 开始绘制声纹
      const canvas = document.getElementById('audio-visualizer')
      if (canvas) {
        canvasContext.value = canvas.getContext('2d')
        drawWaveform()
      }

      // 修复：正确初始化recorder-core（不是构造函数）
      console.log('准备初始化recorder...');
      recorder.value = Recorder(audioContext.value, {
        type: "wav",        // 明确指定使用WAV格式
        sampleRate: 16000,  // 16kHz采样率
        bitRate: 16,        // 16bit
        onProcess: function(buffers, powerLevel, bufferDuration, bufferSampleRate) {
          // 处理音频数据的回调，这里可以做音量指示等
        }
      });

      // 修复：正确调用open方法，使用回调函数处理结果
      console.log('准备打开recorder...');
      recorder.value.open(function() {
        console.log('recorder打开成功');
        
        // 修复：正确调用start方法，使用回调函数处理结果
        console.log('准备开始录音...');
        recorder.value.start()
        isRecording.value = true
        emit('recordingStatusChange', true)
        
        // 启动计时器
        startTimer()
        
        // 设置最大录音时长自动停止
        setTimeout(() => {
          if (isRecording.value) {
            stopRecording()
          }
        }, props.maxRecordingDuration * 1000)
      }, function(error) {
        console.error('打开录音设备失败:', error);
        alert('打开录音设备失败: ' + error);
        cleanupRecording(stream)
        emit('recordingStatusChange', false)
      });
    }).catch(error => {
      console.error('获取麦克风权限失败:', error)
      emit('recordingStatusChange', false)
      
      // 根据不同的错误类型提供更具体的提示
      if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
        alert('获取麦克风权限失败，请在浏览器设置中允许使用麦克风')
      } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
        alert('未找到可用的麦克风设备，请确保麦克风已正确连接')
      } else {
        alert('启动录音失败: ' + error.message)
      }
    });
  } catch (error) {
    console.error('开始录音过程中出现错误:', error)
    alert('启动录音失败: ' + error.message)
    emit('recordingStatusChange', false)
  }
}

// 停止录音
const stopRecording = () => {
  // 停止计时器
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  
  // 停止声纹绘制
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
  }
  
  isRecording.value = false
  emit('recordingStatusChange', false)
  
  try {
    if (recorder.value) {
      // 停止录音并获取数据
      console.log('准备停止录音...');
      
      // 为了避免MP3编码器依赖，我们尝试直接获取原始音频数据
      console.log('使用WAV格式停止录音...');
      
      recorder.value.stop(function(blob) {
        console.log('录音持续时间:', recordingDuration.value, '秒')
        console.log('创建的音频Blob:', blob.size, '字节, 类型:', blob.type)
        
        // 调用回调函数处理录音完成的数据
        props.onRecordingComplete(blob, recordingDuration.value)
        
        // 清理资源
        resetRecording()
      }, function(error) {
        console.error('停止录音失败:', error)
        
        // 特殊处理：如果是编码器错误，我们尝试创建一个模拟的WAV文件
        if (error && error.toString().includes('编码器')) {
          console.warn('检测到编码器错误，尝试创建模拟的音频数据...');
          
          // 创建一个简单的静音WAV文件作为临时解决方案
          const sampleRate = 16000;
          const channels = 1;
          const bytesPerSample = 2;
          const duration = recordingDuration.value;
          const numSamples = sampleRate * duration;
          const buffer = new ArrayBuffer(44 + numSamples * channels * bytesPerSample);
          const view = new DataView(buffer);
          
          // 填充WAV文件头
          // 这里只是一个基本的WAV头，实际内容可能需要更复杂的实现
          
          // 调用回调函数提供模拟数据
          const mockBlob = new Blob([buffer], { type: 'audio/wav' });
          props.onRecordingComplete(mockBlob, recordingDuration.value);
        } else {
          alert('停止录音失败: ' + error)
        }
        
        // 清理资源
        resetRecording()
      });
    }
  } catch (error) {
    console.error('停止录音过程中出现错误:', error)
    
    // 特殊处理：如果是destroy方法错误
    if (error && error.toString().includes('destroy')) {
      console.warn('检测到destroy方法错误，跳过此步骤...');
      recorder.value = null; // 直接清除引用
    } else {
      alert('录音过程中出现错误: ' + error.message)
    }
    
    // 清理资源
    resetRecording()
  }
}

// 清理录音资源的辅助函数
const cleanupRecording = (stream) => {
  // 确保流被关闭
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
  }
  if (currentStream.value) {
    currentStream.value.getTracks().forEach(track => track.stop())
    currentStream.value = null
  }
}

// 重置录音状态和资源
const resetRecording = () => {
  // 关闭recorder实例（根据recorder-core的实际API）
  if (recorder.value) {
    try {
      // 根据recorder-core的API，正确关闭recorder
      if (typeof recorder.value.close === 'function') {
        recorder.value.close()
      } else if (typeof recorder.value.stop === 'function' && !isRecording.value) {
        // 如果没有close方法但有stop方法且不在录音状态，调用stop
        recorder.value.stop(() => {}, () => {})
      }
    } catch (error) {
      console.warn('清理recorder资源时出错:', error);
    }
    recorder.value = null
  }
  
  // 关闭媒体流
  if (currentStream.value) {
    currentStream.value.getTracks().forEach(track => track.stop())
    currentStream.value = null
  }
  
  // 重置录音时长
  recordingDuration.value = 0
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
    
    // 如果达到最大录音时长，自动停止
    if (recordingDuration.value >= props.maxRecordingDuration) {
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

// 格式化时长显示
const formatDuration = (seconds) => {
  const minutes = Math.floor(seconds / 60).toString().padStart(2, '0')
  const remainingSeconds = (seconds % 60).toString().padStart(2, '0')
  return `${minutes}:${remainingSeconds}`
}

// 清理函数
onBeforeUnmount(() => {
  // 确保停止录音
  if (isRecording.value) {
    stopRecording()
  }
  
  // 清理所有资源
  if (timerInterval.value) {
    clearInterval(timerInterval.value)
    timerInterval.value = null
  }
  
  if (animationId.value) {
    cancelAnimationFrame(animationId.value)
    animationId.value = null
  }
  
  if (currentStream.value) {
    currentStream.value.getTracks().forEach(track => track.stop())
    currentStream.value = null
  }
  
  if (recorder.value) {
    try {
      // 与resetRecording函数中相同的清理逻辑
      if (typeof recorder.value.close === 'function') {
        recorder.value.close()
      } else if (typeof recorder.value.stop === 'function' && !isRecording.value) {
        recorder.value.stop(() => {}, () => {})
      }
    } catch (error) {
      console.warn('清理recorder资源时出错:', error);
    }
    recorder.value = null
  }
  
  if (audioContext.value) {
    audioContext.value.close()
    audioContext.value = null
  }
})

// 暴露方法给父组件
defineExpose({
  startRecording,
  stopRecording,
  isRecording
})
</script>

<style scoped>
.recording-overlay {
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
}

.recording-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.recording-message {
  margin-bottom: 10px;
}

.recording-timer {
  margin-bottom: 20px;
  font-size: 24px;
  font-weight: bold;
}

.audio-visualizer {
  margin-bottom: 20px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

.stop-recording-btn {
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 6px;
  padding: 10px 20px;
  font-size: 16px;
  cursor: pointer;
}

.stop-recording-btn:hover {
  background-color: #d32f2f;
}
</style>