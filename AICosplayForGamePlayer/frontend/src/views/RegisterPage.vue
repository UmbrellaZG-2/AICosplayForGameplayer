<template>
  <div class="register-container">
    <div class="register-form">
      <h2>创建账号</h2>
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            required
            placeholder="请设置用户名"
          />
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            type="email"
            id="email"
            v-model="form.email"
            required
            placeholder="请输入邮箱"
          />
        </div>
        <div class="form-group">
          <label for="verificationCode">验证码</label>
          <div class="verification-code-container">
            <input
              type="text"
              id="verificationCode"
              v-model="form.verificationCode"
              required
              placeholder="请输入验证码"
              maxlength="6"
              pattern="[0-9]{6}"
            />
            <button 
              type="button" 
              class="get-code-button"
              :disabled="countdown > 0 || !form.email"
              @click="handleGetVerificationCode"
            >
              {{ countdown > 0 ? `${countdown}秒后重试` : '获取验证码' }}
            </button>
          </div>
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            required
            placeholder="请设置密码"
          />
        </div>
        <button type="submit" class="register-button">注册</button>
      </form>
      <div class="login-link">
        <span>已有账号？</span>
        <router-link to="/">返回登录</router-link>
      </div>
    </div>
  </div>
  
  <!-- 通知组件 -->
  <Notification
    :show="showNotification"
    :title="notificationTitle"
    :message="notificationMessage"
    @confirm="handleNotificationConfirm"
    @close="handleNotificationClose"
  />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../utils/api.js'
import Notification from '../components/Notification.vue'

const router = useRouter()
const form = ref({
  username: '',
  email: '',
  password: '',
  verificationCode: ''
})

// 验证码倒计时
const countdown = ref(0)
let countdownTimer = null

// 通知相关状态
const showNotification = ref(false)
const notificationTitle = ref('')
const notificationMessage = ref('')
const notificationType = ref('') // success, usernameDuplicate, emailDuplicate

// 处理获取验证码
const handleGetVerificationCode = async () => {
  try {
    const response = await authAPI.generateVerificationCode(form.value.email)
    if (response.success) {
      // 开始倒计时
      countdown.value = 60
      startCountdown()
      
      notificationTitle.value = '验证码已发送'
      notificationMessage.value = '验证码已发送到控制台，请查看控制台获取验证码。'
      notificationType.value = 'verificationSuccess'
      showNotification.value = true
    } else {
      notificationTitle.value = '获取验证码失败'
      notificationMessage.value = response.message
      notificationType.value = 'error'
      showNotification.value = true
    }
  } catch (err) {
    notificationTitle.value = '获取验证码失败'
    notificationMessage.value = '获取验证码失败，请检查网络或服务器状态'
    notificationType.value = 'error'
    showNotification.value = true
  }
}

// 开始倒计时
const startCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  
  countdownTimer = setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

const handleRegister = async () => {
  try {
    const response = await authAPI.register(form.value)
    if (response.success) {
      // 注册成功
      notificationTitle.value = '注册成功'
      notificationMessage.value = '注册成功！'
      notificationType.value = 'success'
      showNotification.value = true
    } else {
        // 根据错误信息显示不同的通知
        if (response.message === '用户名重复') {
          notificationTitle.value = '用户名重复'
          notificationMessage.value = '该用户名已被注册，请尝试其他用户名。'
          notificationType.value = 'usernameDuplicate'
        } else if (response.message === '邮箱重复') {
          notificationTitle.value = '邮箱重复'
          notificationMessage.value = '该邮箱已被注册，请尝试其他邮箱。'
          notificationType.value = 'emailDuplicate'
        } else if (response.message === '验证码错误') {
          notificationTitle.value = '验证码错误'
          notificationMessage.value = '验证码错误！'
          notificationType.value = 'error'
        } else if (response.message === '验证码已过期或不存在') {
          notificationTitle.value = '验证码过期'
          notificationMessage.value = '验证码已过期或不存在，请重新获取。'
          notificationType.value = 'error'
        } else {
          notificationTitle.value = '注册失败'
          notificationMessage.value = response.message
          notificationType.value = 'error'
        }
        showNotification.value = true
      }
  } catch (err) {
    notificationTitle.value = '注册失败'
    notificationMessage.value = '注册失败，请检查网络或服务器状态'
    notificationType.value = 'error'
    showNotification.value = true
  }
}

// 处理通知确认按钮点击和关闭
const handleNotificationConfirm = () => {
  showNotification.value = false
  
  // 注册成功后跳转到登录页面
  if (notificationType.value === 'success') {
    router.push('/')
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.register-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
}

.register-form h2 {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

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

.register-button {
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

  .register-button:hover {
    background-color: #45a049;
  }

  .login-link {
    text-align: center;
    margin-top: 20px;
    color: #666;
  }

  .login-link a {
    color: #4CAF50;
    text-decoration: none;
  }

  .login-link a:hover {
    text-decoration: underline;
  }

  /* 验证码容器样式 */
  .verification-code-container {
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .verification-code-container input {
    flex: 1;
  }

  .get-code-button {
    padding: 10px 16px;
    background-color: #6c757d;
    color: white;
    border: none;
    border-radius: 6px;
    font-size: 14px;
    cursor: pointer;
    white-space: nowrap;
    transition: background-color 0.3s;
  }

  .get-code-button:hover:not(:disabled) {
    background-color: #5a6268;
  }

  .get-code-button:disabled {
    background-color: #adb5bd;
    cursor: not-allowed;
  }
</style>