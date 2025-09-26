<template>
  <div class="profile-container">
    <div class="profile-form">
      <h2>个人资料</h2>
      
      <!-- 头像上传与预览区域 -->
      <div class="avatar-section">
        <div class="avatar-preview">
          <img :src="avatarPreview" alt="用户头像" class="avatar-image">
          <input 
            type="file" 
            id="avatar-upload" 
            accept="image/*" 
            @change="handleAvatarUpload"
            class="avatar-upload-input"
            ref="fileInput"
          >
          <label for="avatar-upload" class="avatar-upload-label">
            更换头像
          </label>
        </div>

        <!-- 头像裁剪模态框 -->
        <teleport to="body">
          <div class="cropper-modal" v-if="showCropper">
            <div class="cropper-overlay" @click="closeCropper"></div>
            <div class="cropper-container">
              <div class="cropper-header">
                <h3>裁剪头像</h3>
                <button class="close-btn" @click="closeCropper">&times;</button>
              </div>
              <cropper
                ref="cropper"
                :img="cropperImg"
                :auto-crop-area="0.8"
                :aspect-ratio="1"
                view-mode="1"
                :guides="true"
                :background="false"
                class="cropper"
              ></cropper>
              <div class="cropper-buttons">
                <button class="btn cancel-btn" @click="closeCropper">取消</button>
                <button class="btn confirm-btn" @click="confirmCrop">确认裁剪</button>
              </div>
            </div>
          </div>
        </teleport>
      </div>

      <!-- 用户信息表单 -->
      <form @submit.prevent="handleUpdateProfile">
        <!-- 登录用户名（不可编辑） -->
        <div class="form-group">
          <label for="username">登录用户名</label>
          <input
            type="text"
            id="username"
            :value="userInfo.username"
            disabled
            class="disabled-input"
          />
        </div>

        <!-- 昵称编辑字段 -->
        <div class="form-group">
          <label for="nickname">昵称</label>
          <input
            type="text"
            id="nickname"
            v-model="form.nickname"
            placeholder="请输入昵称"
            maxlength="20"
          />
        </div>

        <div class="button-group">
          <button type="submit" class="update-button" :disabled="isLoading">
            {{ isLoading ? '保存中...' : '保存修改' }}
          </button>
          <button @click="goToChat" class="back-button" :disabled="isLoading">
            返回对话
          </button>
        </div>
      </form>

      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="success" class="success-message">{{ success }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import Cropper from 'vue3-cropper'
import 'vue3-cropper/lib/vue3-cropper.css'
import { useRouter } from 'vue-router'
import { authAPI } from '../utils/api.js'

const router = useRouter()
const userInfo = ref({ username: '', nickname: '' })
const form = ref({ nickname: '' })
const avatarPreview = ref('/default-avatar.png') // 默认头像路径
const isLoading = ref(false)
const error = ref('')
const success = ref('')
let avatarFile = null
// 裁剪相关变量
const showCropper = ref(false)
const cropperImg = ref('')
const cropper = ref(null)
const fileInput = ref(null)

// 加载用户信息
const loadUserInfo = async () => {
  try {
    isLoading.value = true
    const response = await authAPI.getUserInfo()
    if (response.code === 200 && response.data) {
      userInfo.value = response.data
      form.value.nickname = response.data.nickname || ''
      // 如果用户有头像，显示用户头像；否则显示默认头像
      if (response.data.avatarUrl) {
        avatarPreview.value = response.data.avatarUrl
      }
    } else {
      error.value = '获取用户信息失败'
    }
  } catch (err) {
    console.error('获取用户信息失败:', err)
    error.value = '获取用户信息失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

// 处理头像上传
const handleAvatarUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    // 检查文件类型和大小
    if (!file.type.match('image.*')) {
      error.value = '请选择图片文件'
      // 重置文件输入
      fileInput.value.value = ''
      return
    }
    if (file.size > 5 * 1024 * 1024) { // 5MB
      error.value = '图片大小不能超过5MB'
      // 重置文件输入
      fileInput.value.value = ''
      return
    }
    
    // 创建裁剪框的图片源
    const reader = new FileReader()
    reader.onload = (e) => {
      cropperImg.value = e.target.result
      showCropper.value = true
    }
    reader.readAsDataURL(file)
    
    // 清除错误信息
    error.value = ''
  }
}

// 打开裁剪框
const openCropper = () => {
  showCropper.value = true
}

// 关闭裁剪框
const closeCropper = () => {
  showCropper.value = false
  // 重置文件输入
  fileInput.value.value = ''
}

// 确认裁剪
const confirmCrop = () => {
  if (!cropper.value) return
  
  // 获取裁剪后的图片数据
  cropper.value.getCroppedCanvas().toBlob((blob) => {
    avatarFile = blob
    // 更新预览
    avatarPreview.value = URL.createObjectURL(blob)
    // 关闭裁剪框
    closeCropper()
  }, 'image/jpeg', 0.9)
}

// 返回对话页面
const goToChat = () => {
  router.push('/chat')
}

// 更新用户资料
const handleUpdateProfile = async () => {
  try {
    isLoading.value = true
    error.value = ''
    success.value = ''
    
    // 准备更新数据
    const updateData = { nickname: form.value.nickname }
    
    // 如果有新头像，准备FormData提交
    let response
    if (avatarFile) {
      const formData = new FormData()
      formData.append('nickname', form.value.nickname)
      formData.append('avatar', avatarFile)
      
      response = await authAPI.updateUserInfo(formData)
    } else {
      // 只有昵称更新，使用JSON格式
      response = await authAPI.updateUserInfo(updateData)
    }
    
    if (response.code === 200) {
      success.value = '个人资料更新成功'
      // 重新加载用户信息
      await loadUserInfo()
      // 清除头像文件引用
      avatarFile = null
      // 5秒后清除成功提示
      setTimeout(() => {
        success.value = ''
      }, 5000)
    } else {
      error.value = response.message || '更新失败，请稍后重试'
    }
  } catch (err) {
    console.error('更新用户资料失败:', err)
    error.value = '更新用户资料失败，请检查网络或服务器状态'
  } finally {
    isLoading.value = false
  }
}

// 组件挂载时加载用户信息
onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 20px;
}

.profile-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 100%;
  max-width: 400px;
}

.profile-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

/* 头像上传区域样式 */
.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.avatar-preview {
  position: relative;
  width: 120px;
  height: 120px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid #ddd;
  background-color: #f9f9f9;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-upload-input {
  position: absolute;
  width: 100%;
  height: 100%;
  opacity: 0;
  cursor: pointer;
  z-index: 2;
}

.avatar-upload-label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  text-align: center;
  padding: 8px 0;
  font-size: 12px;
  cursor: pointer;
  z-index: 1;
  transition: background-color 0.3s;
}

.avatar-upload-label:hover {
  background-color: rgba(0, 0, 0, 0.8);
}

/* 表单样式 */
.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  color: #666;
  font-weight: 500;
}

.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 16px;
}

.form-group input:focus {
  outline: none;
  border-color: #4CAF50;
}

.disabled-input {
  background-color: #f5f5f5;
  cursor: not-allowed;
  color: #666;
}

/* 按钮样式 */
.update-button {
  width: 100%;
  padding: 12px;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.update-button:hover:not(:disabled) {
  background-color: #45a049;
}

.update-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 返回按钮样式 */
.button-group {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.back-button {
  width: 100%;
  padding: 12px;
  background-color: #6c757d;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.back-button:hover:not(:disabled) {
  background-color: #5a6268;
}

.back-button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

/* 裁剪模态框样式 */
.cropper-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1000;
}

.cropper-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
}

.cropper-container {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.cropper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #eee;
}

.cropper-header h3 {
  margin: 0;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0 10px;
}

.close-btn:hover {
  color: #000;
}

.cropper {
  width: 100%;
  height: 400px;
  padding: 20px;
  box-sizing: border-box;
}

.cropper-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 15px 20px;
  border-top: 1px solid #eee;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.cancel-btn {
  background-color: #6c757d;
  color: white;
  border: none;
}

.cancel-btn:hover {
  background-color: #5a6268;
}

.confirm-btn {
  background-color: #4CAF50;
  color: white;
  border: none;
}

.confirm-btn:hover {
  background-color: #45a049;

}

/* 提示信息样式 */
.error-message {
  margin-top: 15px;
  color: #f44336;
  text-align: center;
}

.success-message {
  margin-top: 15px;
  color: #4CAF50;
  text-align: center;
}
</style>