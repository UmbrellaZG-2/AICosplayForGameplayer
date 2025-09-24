<template>
  <div class="login-container">
    <div class="login-form">
      <h2>AICosplayForGamePlayer</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            required
            placeholder="请输入用户名"
          />
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            required
            placeholder="请输入密码"
          />
        </div>
        <button type="submit" class="login-button">登录</button>
      </form>
      <div class="register-link">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
      <div class="forgot-password-link">
        <router-link to="/forgot-password">忘记密码？</router-link>
      </div>
      <div v-if="error" class="error-message">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authAPI } from '../utils/api.js'

const router = useRouter()
const form = ref({
  username: '',
  password: ''
})
const error = ref('')

const handleLogin = async () => {
  try {
    const response = await authAPI.login(form.value)
    if (response.success) {
      localStorage.setItem('isLoggedIn', 'true')
      router.push('/chat')
    } else {
      error.value = response.message
    }
  } catch (err) {
    error.value = '登录失败，请检查网络或服务器状态'
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.login-form {
  background: white;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  width: 360px;
}

.login-form h2 {
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

.login-button {
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

.login-button:hover {
  background-color: #45a049;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
}

.register-link a {
  color: #4CAF50;
  text-decoration: none;
}

.register-link a:hover {
  text-decoration: underline;
}

.error-message {
    margin-top: 15px;
    color: #f44336;
    text-align: center;
  }
  
  .forgot-password-link {
    text-align: center;
    margin-top: 10px;
  }
  
  .forgot-password-link a {
    color: #007bff;
    text-decoration: none;
    font-size: 14px;
  }
  
  .forgot-password-link a:hover {
    text-decoration: underline;
  }
</style>