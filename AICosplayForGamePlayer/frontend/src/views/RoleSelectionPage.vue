<template>
  <div class="app-container">
    <div class="role-selection-container">
      <div class="header">
        <h1>选择角色</h1>
        <button class="back-button" @click="navigateBack">← 返回</button>
      </div>
    
    <!-- 预设角色选择区 -->
    <div class="section">
      <h2>预设角色</h2>
      <div class="role-grid">
        <div 
          v-for="presetRole in presetRoles" 
          :key="presetRole.id"
          class="role-card"
          @click="selectPresetRole(presetRole.id, presetRole.name)"
        >
        <div class="role-avatar">
            <img v-if="presetRole.avatar" :src="presetRole.avatar" :alt="presetRole.name" @error="handleAvatarError">
            <span v-else>👤</span>
          </div>
          <div class="role-info">
            <div class="role-name">{{ presetRole.name }}</div>
            <div class="role-description">{{ presetRole.description }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 自定义角色选择区 -->
    <div class="section">
      <h2>自定义角色</h2>
      <div v-if="customRoles.length > 0" class="role-grid">
        <div 
          v-for="customRole in customRoles" 
          :key="customRole.id"
          class="role-card"
          @click="selectCustomRole(customRole.id, customRole.name)"
        >
          <div class="role-avatar">
            <img v-if="customRole.avatar" :src="customRole.avatar" :alt="customRole.name" @error="handleAvatarError">
            <span v-else>👤</span>
          </div>
          <div class="role-info">
            <div class="role-name">{{ customRole.name }}</div>
            <div class="role-description">{{ customRole.description || '自定义角色' }}</div>
          </div>
        </div>
      </div>
      <div v-else class="empty-custom-roles">
        <p>暂无自定义角色</p>
      </div>
    </div>
    
    <!-- 创建新角色按钮 -->
    <div class="create-role-section">
      <button class="create-role-button" @click="showCreateForm = true">
        <span class="plus-icon">+</span> 创建新角色
      </button>
    </div>
    
    <!-- 全局页脚 -->
    <footer class="global-footer">
      <div class="footer-content">
        <div class="footer-links">
          <a href="/about" class="footer-link">关于我们</a>
        </div>
        <p class="footer-copyright">© 2025 AICosplayForGamePlayer</p>
      </div>
    </footer>
    
    <!-- 创建新角色表单（弹窗） -->
    <div v-if="showCreateForm" class="modal-overlay" @click="closeCreateForm">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>创建新角色</h2>
          <button class="close-button" @click="closeCreateForm">×</button>
        </div>
        <form @submit.prevent="createNewRole" class="role-form">
          <div class="form-group">
            <label for="roleName">角色名称</label>
            <input 
              type="text" 
              id="roleName" 
              v-model="newRole.name" 
              required 
              placeholder="请输入角色名称"
            >
          </div>
          <div class="form-group">
            <label for="roleDescription">角色描述</label>
            <textarea 
              id="roleDescription" 
              v-model="newRole.description" 
              rows="3" 
              placeholder="请输入角色描述"
            ></textarea>
          </div>
          <div class="form-group">
            <label for="rolePersonality">角色性格</label>
            <textarea 
              id="rolePersonality" 
              v-model="newRole.personality" 
              rows="2" 
              placeholder="请输入角色性格特点"
            ></textarea>
          </div>
          <div class="form-group">
            <label for="roleAvatarUrl">头像URL</label>
            <input 
              type="text" 
              id="roleAvatarUrl" 
              v-model="newRole.avatarUrl" 
              placeholder="请输入头像URL（可选）"
            >
          </div>
          <div class="form-actions">
            <button type="button" class="cancel-button" @click="closeCreateForm">取消</button>
            <button type="submit" class="submit-button" :disabled="isCreating">
              {{ isCreating ? '创建中...' : '创建角色' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { gameCharacterAPI } from '../utils/api.js'
import { addAvatarPathsToCharacters, preloadAvatars } from '../utils/characterUtils.js'

const router = useRouter()
const presetRoles = ref([])
const customRoles = ref([])
const showCreateForm = ref(false)
const newRole = ref({
  name: '',
  description: '',
  personality: '',
  avatarUrl: ''
})

// 加载所有角色并根据is_preset分类
const loadRoles = async () => {
  try {
    const response = await gameCharacterAPI.getAll()
    if (response && response.code === 200 && response.data) {
      // 根据isPreset属性分类（Spring Boot返回的是驼峰命名）
      presetRoles.value = response.data.filter(role => role.isPreset === 1)
      customRoles.value = response.data.filter(role => role.isPreset === 0)
      
      // 使用工具函数为每个角色设置头像路径（基于名称匹配）
      presetRoles.value = addAvatarPathsToCharacters(presetRoles.value)
      customRoles.value = addAvatarPathsToCharacters(customRoles.value)
      
      // 预加载所有角色的头像到缓存中
      const allRoles = [...presetRoles.value, ...customRoles.value]
      await preloadAvatars(allRoles)
      console.log('所有角色头像预加载完成')
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
    alert('加载角色列表失败，请稍后重试')
  }
}

// 选择预设角色
const selectPresetRole = (roleId, roleName) => {
  // 存储选择的角色信息
  localStorage.setItem('selectedRoleId', roleId)
  localStorage.setItem('selectedRoleName', roleName)
  
  // 创建新对话
  createNewConversation(roleId, roleName)
}

// 创建新对话
const createNewConversation = async (roleId, roleName) => {
  try {
    // 导航到聊天页面
    router.push('/chat')
  } catch (error) {
    console.error('创建对话失败:', error)
    alert('创建对话失败，请稍后重试')
  }
}

// 创建自定义角色
const createCustomRole = async () => {
  try {
    if (!newRole.value.name || !newRole.value.description || !newRole.value.personality) {
      alert('请填写所有必填字段')
      return
    }
    
    // 发送请求创建角色
    const response = await gameCharacterAPI.create({
      name: newRole.value.name,
      description: newRole.value.description,
      personality: newRole.value.personality,
      avatarUrl: newRole.value.avatarUrl || '',
      isPreset: 0 // 0表示自定义角色
    })
    
    if (response.code === 200) {
      // 创建成功后，重新加载角色列表
      await loadRoles()
      // 关闭创建表单
      closeCreateForm()
      alert('角色创建成功')
    } else {
      alert('角色创建失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('创建角色失败:', error)
    alert('创建角色失败，请稍后重试')
  }
}

// 打开创建表单
const openCreateForm = () => {
  showCreateForm.value = true
}

// 关闭创建表单
const closeCreateForm = () => {
  showCreateForm.value = false
  // 重置表单
  newRole.value = {
    name: '',
    description: '',
    personality: '',
    avatarUrl: ''
  }
}

// 返回上一页
const navigateBack = () => {
  router.back()
}

// 处理头像加载错误
const handleAvatarError = (event) => {
  if (event && event.target) {
    event.target.style.display = 'none'
    const sibling = event.target.nextElementSibling
    if (sibling) {
      sibling.style.display = 'block'
    }
  }
}

// 组件挂载时加载数据
onMounted(() => {
  loadRoles()
})
</script>

<style scoped>
.app-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.role-selection-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* 全局页脚样式 */
.global-footer {
  background-color: #f8f9fa;
  border-top: 1px solid #e9ecef;
  padding: 16px 20px;
  text-align: center;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-links {
  margin-bottom: 8px;
}

.footer-link {
  color: #6c757d;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s ease;
}

.footer-link:hover {
  color: #007bff;
}

.footer-copyright {
  color: #adb5bd;
  font-size: 12px;
  margin: 0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.back-button {
  background: none;
  border: 1px solid #ddd;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
}

.back-button:hover {
  background-color: #f5f5f5;
  border-color: #bbb;
  color: #333;
}

.section {
  margin-bottom: 40px;
}

.section h2 {
  font-size: 20px;
  font-weight: 500;
  margin-bottom: 20px;
  color: #444;
  border-bottom: 2px solid #eee;
  padding-bottom: 10px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.role-card {
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 16px;
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
  border-color: #007bff;
}

.role-avatar {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background-color: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
}

.role-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.role-avatar span {
  font-size: 30px;
}

.role-info {
  flex: 1;
  min-width: 0;
}

.role-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.role-description {
  font-size: 14px;
  color: #666;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.empty-custom-roles {
  text-align: center;
  padding: 40px;
  color: #999;
  background-color: #f9f9f9;
  border-radius: 8px;
  border: 1px dashed #ddd;
}

.create-role-section {
  margin-top: auto;
  text-align: center;
  padding: 20px 0;
}

.create-role-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  padding: 14px 32px;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.create-role-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.plus-icon {
  font-size: 18px;
  font-weight: bold;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: #333;
}

.close-button {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.3s ease;
}

.close-button:hover {
  background-color: #f5f5f5;
  color: #666;
}

.role-form {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #444;
  margin-bottom: 8px;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.3s ease;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #007bff;
  box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.cancel-button {
  background: none;
  border: 1px solid #ddd;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.3s ease;
}

.cancel-button:hover {
  background-color: #f5f5f5;
  border-color: #bbb;
}

.submit-button {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: white;
  padding: 10px 24px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.submit-button:hover:not(:disabled) {
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.submit-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .role-selection-container {
    padding: 15px;
  }
  
  .global-footer {
    padding: 12px 15px;
  }
  
  .header h1 {
    font-size: 24px;
  }
  
  .role-grid {
    grid-template-columns: 1fr;
  }
  
  .modal-content {
    width: 95%;
    margin: 20px;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .cancel-button,
  .submit-button {
    width: 100%;
  }
}
</style>