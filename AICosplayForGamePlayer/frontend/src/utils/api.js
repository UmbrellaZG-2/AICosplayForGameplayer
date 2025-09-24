import axios from 'axios'

// 创建axios实例
const api = axios.create({
  baseURL: '',
  timeout: 10000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    // 处理错误响应
    if (error.response) {
      // 服务器返回错误状态码
      console.error('API Error:', error.response.data)
      if (error.response.status === 401) {
        // 未授权，跳转到登录页
        localStorage.removeItem('isLoggedIn')
        window.location.href = '/'
      }
    } else if (error.request) {
      // 请求发出但没有收到响应
      console.error('Network Error:', error.request)
    } else {
      // 请求配置出错
      console.error('Request Error:', error.message)
    }
    return Promise.reject(error)
  }
)

// 认证相关API
export const authAPI = {
  login: (credentials) => api.post('/api/auth/login', credentials),
  register: (userData) => api.post('/api/auth/register', userData),
  generateVerificationCode: (email) => api.post('/api/auth/generate-code', { email })
}

// 对话相关API
export const conversationAPI = {
  create: (title, characterId) => api.post('/api/conversations', { title, characterId }),
  getAll: () => api.get('/api/conversations'),
  getById: (id) => api.get(`/api/conversations/${id}`),
  delete: (id) => api.delete(`/api/conversations/${id}`),
  getMessages: (id) => api.get(`/api/conversations/${id}/messages`),
  addMessage: (id, message) => api.post(`/api/conversations/${id}/messages`, message)
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
  }
}

export default api