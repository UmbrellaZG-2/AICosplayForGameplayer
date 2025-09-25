import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 详细的请求日志
    console.log('API请求开始:', {
      url: config.url,
      method: config.method,
      baseURL: config.baseURL,
      headers: config.headers,
      data: config.data,
      timeout: config.timeout,
      withCredentials: config.withCredentials
    })
    
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('添加Token到请求头:', token)
    }
    
    return config
  },
  error => {
    console.error('API请求配置错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    console.log('API响应成功:', {
      url: response.config.url,
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
      data: response.data
    })
    return response.data
  },
  error => {
    // 处理错误响应
    console.error('API响应错误 - 完整错误对象:', error)
    
    if (error.response) {
      // 服务器返回错误状态码
      console.error('API错误 - 服务器响应:', {
        url: error.config?.url,
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      })
      
      if (error.response.status === 401) {
        // 未授权，跳转到登录页
        console.error('未授权访问，重定向到登录页')
        localStorage.removeItem('isLoggedIn')
        window.location.href = '/'
      }
      // 将后端返回的错误数据返回给调用方
      return Promise.reject(error.response.data)
    } else if (error.request) {
      // 请求发出但没有收到响应
      console.error('API错误 - 网络错误:', {
        message: '请求发出但没有收到响应',
        request: error.request
      })
    } else {
      // 请求配置出错
      console.error('API错误 - 请求配置错误:', {
        message: error.message
      })
    }
    
    // 打印完整错误对象，方便调试
    console.error('API错误 - 完整错误信息:', JSON.stringify(error, null, 2))
    return Promise.reject(error)
  }
)

// 认证相关API
export const authAPI = {
  login: (credentials) => api.post('/api/auth/login', credentials),
  register: (userData) => api.post('/api/auth/register', userData),
  generateVerificationCode: (email) => api.post('/api/auth/generate-code', { email }),
  checkUsername: (username) => api.post('/api/auth/check-username', { username }),
  checkEmail: (email) => api.post('/api/auth/check-email', { email }),
  verifyUserEmail: (username, email) => api.post('/api/auth/verify-user-email', { username, email }),
  resetPassword: (data) => api.post('/api/auth/reset-password', data),
  // 获取当前用户信息
  getUserInfo: () => api.get('/api/auth/user'),
  // 更新用户信息
  updateUserInfo: (userData) => api.put('/api/auth/user', userData)
}

// 对话相关API
export const conversationAPI = {
  create: (title, characterId = null) => {
    // 确保向后兼容：如果只传递一个参数且不是字符串，则可能是旧版调用方式
    if (typeof title === 'object' && arguments.length === 1) {
      console.log('conversationAPI.create - 调用方式(旧版):', title)
      return api.post('/api/conversations', title);
    }
    console.log('conversationAPI.create - 调用方式(新版):', { title, characterId })
    return api.post('/api/conversations', { title, characterId });
  },
  getAll: () => {
    console.log('conversationAPI.getAll - 调用')
    return api.get('/api/conversations')
  },
  getById: (id) => {
    console.log('conversationAPI.getById - 调用:', id)
    return api.get(`/api/conversations/${id}`)
  },
  delete: (id) => {
    console.log('conversationAPI.delete - 调用:', id)
    return api.delete(`/api/conversations/${id}`)
  },
  getMessages: (id) => {
    console.log('conversationAPI.getMessages - 调用:', id)
    return api.get(`/api/conversations/${id}/messages`)
  },
  addMessage: (id, message) => {
    console.log('conversationAPI.addMessage - 调用开始:', id, message)
    const startTime = Date.now()
    
    // 修复Promise处理，确保返回正确的Promise链
    return api.post(`/api/conversations/${id}/messages`, message)
      .then(response => {
        const endTime = Date.now()
        console.log(`conversationAPI.addMessage - 成功响应 (${endTime - startTime}ms):`, response)
        return response
      }).catch(error => {
        const endTime = Date.now()
        console.error(`conversationAPI.addMessage - 请求失败 (${endTime - startTime}ms):`, error)
        // 重新抛出错误，确保调用者能够捕获到
        throw error
      })
  }
}

// 游戏角色相关API
export const gameCharacterAPI = {
  getAll: () => api.get('/api/characters'),
  create: (characterData) => api.post('/api/characters', characterData)
}

// 语音识别相关API
export const speechAPI = {
  // 发送音频文件进行语音识别
  recognize: (audioBlob) => {
    const formData = new FormData()
    formData.append('audio', audioBlob)
    return api.post('/api/speech/recognize', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 30000 // 语音识别可能需要更长时间
    })
  },
  // 检查语音识别服务的健康状态
  healthCheck: () => api.get('/api/speech/health'),
  // 切换语音识别服务类型
  switchServiceType: (type) => api.post('/api/speech/switch', null, {
    params: { type }
  }),
  // 获取语音数据
  getSpeechData: (voiceId) => api.get(`/api/speech/${voiceId}`, {
    responseType: 'arraybuffer',
    timeout: 30000
  })
}

// 用户相关API，与authAPI功能重叠，保留以兼容现有代码
export const userAPI = authAPI

// 导出axios实例，以便在其他地方直接使用
export { api }

export default api