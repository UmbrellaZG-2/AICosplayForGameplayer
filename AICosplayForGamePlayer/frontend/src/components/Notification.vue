<template>
  <div v-if="show" class="notification-overlay" @click="handleOverlayClick">
    <div class="notification-container" @click.stop>
      <h3 class="notification-title">{{ title }}</h3>
      <p class="notification-message">{{ message }}</p>
      <div class="notification-actions">
        <button 
          class="notification-button"
          @click="handleConfirm"
        >
          确定
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'

// 定义属性
const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: ''
  },
  message: {
    type: String,
    default: ''
  }
})

// 定义事件
const emit = defineEmits(['confirm', 'close'])

// 处理确认按钮点击
const handleConfirm = () => {
  emit('confirm')
}

// 处理点击遮罩层
const handleOverlayClick = () => {
  emit('close')
}
</script>

<style scoped>
.notification-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.notification-container {
  background: white;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
  width: 90%;
  max-width: 400px;
  text-align: center;
}

.notification-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #333;
}

.notification-message {
  font-size: 14px;
  color: #666;
  margin-bottom: 24px;
  line-height: 1.5;
}

.notification-actions {
  display: flex;
  justify-content: center;
}

.notification-button {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 8px 20px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.notification-button:hover {
  background-color: #45a049;
}
</style>