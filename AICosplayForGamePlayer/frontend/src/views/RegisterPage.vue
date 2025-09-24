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
            @input="handleUsernameInput"
            required
            placeholder="请设置用户名"
          />
          <div v-if="usernameChecking" class="username-status checking">
            正在检查用户名可用性...
          </div>
          <div v-else-if="usernameStatus === 'available'" class="username-status available">
            用户名可用
          </div>
          <div v-else-if="usernameStatus === 'taken'" class="username-status taken">
            用户名已被使用
          </div>
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input
            type="email"
            id="email"
            v-model="form.email"
            @input="handleEmailInput"
            required
            placeholder="请输入邮箱"
          />
          <div v-if="emailChecking" class="email-status checking">
            正在检查邮箱可用性...
          </div>
          <div v-else-if="emailStatus === 'available'" class="email-status available">
            邮箱可用
          </div>
          <div v-else-if="emailStatus === 'taken'" class="email-status taken">
            邮箱已被使用
          </div>
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
        <button type="submit" class="register-button" :disabled="!isFormValid">注册</button>
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
import { ref, computed } from 'vue'
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

// 计算表单是否有效
const isFormValid = computed(() => {
  return form.value.username.trim() &&
         form.value.email.trim() &&
         form.value.password.trim() &&
         form.value.verificationCode.trim() &&
         usernameStatus.value === 'available' &&
         emailStatus.value === 'available'
})

// 用户名检查状态
const usernameStatus = ref('') // available, taken
const usernameChecking = ref(false)
let usernameCheckTimer = null

// 邮箱检查状态
const emailStatus = ref('') // available, taken
const emailChecking = ref(false)
let emailCheckTimer = null

// 处理用户名输入，添加防抖
const handleUsernameInput = () => {
  // 清除之前的定时器
  if (usernameCheckTimer) {
    clearTimeout(usernameCheckTimer)
  }
  
  // 如果用户名不为空，设置新的定时器
  if (form.value.username.trim()) {
    usernameStatus.value = ''
    usernameCheckTimer = setTimeout(() => {
      checkUsername(form.value.username)
    }, 500) // 500毫秒防抖
  } else {
    usernameStatus.value = ''
  }
}

// 处理邮箱输入，添加防抖
const handleEmailInput = () => {
  // 清除之前的定时器
  if (emailCheckTimer) {
    clearTimeout(emailCheckTimer)
  }
  
  // 如果邮箱不为空，设置新的定时器
  if (form.value.email.trim()) {
    emailStatus.value = ''
    emailCheckTimer = setTimeout(() => {
      checkEmail(form.value.email)
    }, 500) // 500毫秒防抖
  } else {
    emailStatus.value = ''
  }
}

// 检查用户名是否可用
const checkUsername = async (username) => {
  if (!username.trim()) {
    return
  }
  
  usernameChecking.value = true
  try {
    const response = await authAPI.checkUsername(username)
    if (response.success) {
      usernameStatus.value = 'available'
    } else {
      usernameStatus.value = 'taken'
    }
  } catch (err) {
    console.error('检查用户名失败:', err)
    usernameStatus.value = 'taken'
  } finally {
    usernameChecking.value = false
  }
}

// 检查邮箱是否可用
const checkEmail = async (email) => {
  if (!email.trim()) {
    return
  }
  
  emailChecking.value = true
  try {
    const response = await authAPI.checkEmail(email)
    if (response.success) {
      emailStatus.value = 'available'
    } else {
      emailStatus.value = 'taken'
    }
  } catch (err) {
    console.error('检查邮箱失败:', err)
    emailStatus.value = 'taken'
  } finally {
    emailChecking.value = false
  }
}

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
      // 特定处理邮箱已被注册的错误
      if (response.message === '邮箱已被注册') {
        notificationTitle.value = '邮箱已被使用'
        notificationMessage.value = '该邮箱已被使用，请尝试其他邮箱。'
        notificationType.value = 'emailDuplicate'
      } else {
        notificationTitle.value = '获取验证码失败'
        notificationMessage.value = response.message
        notificationType.value = 'error'
      }
      showNotification.value = true
    }
  } catch (err) {
    notificationTitle.value = '获取验证码失败'
    // 尝试显示err对象中的message属性，如果存在
    notificationMessage.value = err && err.message ? err.message : '获取验证码失败，请检查网络或服务器状态'
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
        if (response.message === '用户名已被使用') {
          notificationTitle.value = '用户名重复'
          notificationMessage.value = '该用户名已被注册，请尝试其他用户名。'
          notificationType.value = 'usernameDuplicate'
        } else if (response.message === '邮箱已被使用') {
          notificationTitle.value = '邮箱已被使用'
          notificationMessage.value = '该邮箱已被使用，请尝试其他邮箱。'
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

.username-status {
  font-size: 12px;
  margin-top: 4px;
}

.username-status.checking {
  color: #999;
}

.username-status.available {
  color: #4CAF50;
}

.username-status.taken {
  color: #f44336;
}

.email-status {
  font-size: 12px;
  margin-top: 4px;
}

.email-status.checking {
  color: #999;
}

.email-status.available {
  color: #4CAF50;
}

.email-status.taken {
  color: #f44336;
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