<template>
  <div class="conversation-list">
    <!-- 顶部操作栏 -->
    <div class="conversation-header">
      <h2>对话列表</h2>
      <div class="header-actions">
        <button 
          class="new-chat-btn" 
          @click="createNewConversation"
          title="创建新对话"
        >
          🆕 新建对话
        </button>
      </div>
    </div>
    
    <!-- 搜索框 -->
    <div class="search-container">
      <input 
        type="text" 
        class="search-input" 
        placeholder="搜索对话..." 
        v-model="searchQuery"
        @input="handleSearch"
      >
      <button 
        class="clear-search-btn" 
        v-if="searchQuery"
        @click="clearSearch"
        title="清除搜索"
      >
        ✕
      </button>
    </div>
    
    <!-- 对话列表 -->
    <div class="conversations-container">
      <div 
        v-if="filteredConversations.length === 0" 
        class="empty-state"
      >
        <p>{{ searchQuery ? '没有找到匹配的对话' : '暂无对话，点击上方按钮创建新对话' }}</p>
      </div>
      
      <div 
        v-for="conversation in filteredConversations" 
        :key="conversation.id"
        class="conversation-item"
        :class="{ 'active': activeConversationId === conversation.id }"
        @click="selectConversation(conversation)"
      >
        <!-- 对话内容预览 -->
        <div class="conversation-content">
          <!-- 对话标题/最近消息 -->
          <div class="conversation-preview">
            <h3 class="conversation-title">{{ conversation.title || getPreviewTitle(conversation) }}</h3>
            <p class="conversation-snippet">{{ getLastMessageSnippet(conversation) }}</p>
          </div>
          
          <!-- 对话元信息 -->
          <div class="conversation-meta">
            <span class="conversation-time">{{ formatLastMessageTime(conversation) }}</span>
            <span 
              v-if="conversation.unreadCount > 0" 
              class="unread-count"
            >
              {{ conversation.unreadCount }}
            </span>
          </div>
        </div>
        
        <!-- 操作菜单 -->
        <div class="conversation-actions">
          <button 
            class="action-btn delete-btn"
            @click.stop="deleteConversation(conversation.id)"
            title="删除对话"
          >
            🗑️
          </button>
          <button 
            class="action-btn rename-btn"
            @click.stop="renameConversation(conversation.id)"
            title="重命名对话"
          >
            ✏️
          </button>
        </div>
      </div>
    </div>
    
    <!-- 底部统计信息 -->
    <div class="conversation-footer">
      <span class="total-conversations">共 {{ conversations.length }} 条对话</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// Props
const props = defineProps({
  conversations: {
    type: Array,
    required: true,
    default: () => []
  },
  activeConversationId: {
    type: String,
    default: null
  }
})

// Emits
const emit = defineEmits(['selectConversation', 'createNewConversation', 'deleteConversation', 'renameConversation', 'loadConversations'])

// 状态管理
const searchQuery = ref('')

// 计算过滤后的对话列表
const filteredConversations = computed(() => {
  if (!searchQuery.value.trim()) {
    // 如果没有搜索词，直接返回按最后活跃时间排序的对话列表
    return [...props.conversations].sort((a, b) => {
      const timeA = getLastMessageTime(a)
      const timeB = getLastMessageTime(b)
      return new Date(timeB) - new Date(timeA)
    })
  }
  
  // 否则根据搜索词过滤对话
  const query = searchQuery.value.toLowerCase()
  return props.conversations.filter(conversation => {
    // 检查对话标题是否包含搜索词
    if (conversation.title && conversation.title.toLowerCase().includes(query)) {
      return true
    }
    
    // 检查最近消息内容是否包含搜索词
    const lastMessage = getLastMessage(conversation)
    if (lastMessage && lastMessage.content && lastMessage.content.toLowerCase().includes(query)) {
      return true
    }
    
    return false
  }).sort((a, b) => {
    const timeA = getLastMessageTime(a)
    const timeB = getLastMessageTime(b)
    return new Date(timeB) - new Date(timeA)
  })
})

// 获取对话的最后一条消息
const getLastMessage = (conversation) => {
  if (conversation.messages && conversation.messages.length > 0) {
    return conversation.messages[conversation.messages.length - 1]
  }
  return null
}

// 获取对话的最后一条消息时间
const getLastMessageTime = (conversation) => {
  const lastMessage = getLastMessage(conversation)
  return lastMessage ? lastMessage.createdAt : conversation.createdAt || ''
}

// 获取预览标题
const getPreviewTitle = (conversation) => {
  const lastMessage = getLastMessage(conversation)
  if (lastMessage && lastMessage.content) {
    // 截取前30个字符作为预览标题
    return lastMessage.content.length > 30 
      ? lastMessage.content.substring(0, 30) + '...' 
      : lastMessage.content
  }
  return '新对话'
}

// 获取最后一条消息的摘要
const getLastMessageSnippet = (conversation) => {
  const lastMessage = getLastMessage(conversation)
  if (!lastMessage) {
    return '暂无消息'
  }
  
  if (lastMessage.type === 'voice') {
    return '[语音消息]'
  }
  
  return lastMessage.content && lastMessage.content.length > 50 
    ? lastMessage.content.substring(0, 50) + '...' 
    : lastMessage.content || '消息为空'
}

// 格式化最后一条消息的时间
const formatLastMessageTime = (conversation) => {
  const time = getLastMessageTime(conversation)
  if (!time) return ''
  
  const date = new Date(time)
  const now = new Date()
  const diffInMs = now - date
  const diffInMinutes = Math.floor(diffInMs / (1000 * 60))
  const diffInHours = Math.floor(diffInMs / (1000 * 60 * 60))
  const diffInDays = Math.floor(diffInMs / (1000 * 60 * 60 * 24))
  
  if (diffInMinutes < 1) {
    return '刚刚'
  } else if (diffInMinutes < 60) {
    return `${diffInMinutes}分钟前`
  } else if (diffInHours < 24) {
    return `${diffInHours}小时前`
  } else if (diffInDays < 7) {
    return `${diffInDays}天前`
  } else {
    // 超过一周显示具体日期
    const month = date.getMonth() + 1
    const day = date.getDate()
    return `${month}月${day}日`
  }
}

// 处理搜索
const handleSearch = () => {
  // 搜索逻辑已经在computed属性中实现
  console.log('搜索对话:', searchQuery.value)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
}

// 选择对话
const selectConversation = (conversation) => {
  emit('selectConversation', conversation)
}

// 创建新对话
const createNewConversation = () => {
  emit('createNewConversation')
}

// 删除对话
const deleteConversation = (conversationId) => {
  if (confirm('确定要删除这条对话吗？删除后无法恢复。')) {
    emit('deleteConversation', conversationId)
  }
}

// 重命名对话
const renameConversation = (conversationId) => {
  const newTitle = prompt('请输入新的对话标题：')
  if (newTitle !== null && newTitle.trim() !== '') {
    emit('renameConversation', { conversationId, newTitle: newTitle.trim() })
  }
}

// 组件挂载时加载对话列表
onMounted(() => {
  emit('loadConversations')
})
</script>

<style scoped>
.conversation-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: #f5f5f5;
  border-right: 1px solid #e0e0e0;
}

.conversation-header {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: white;
}

.conversation-header h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.new-chat-btn {
  background-color: #2196F3;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.new-chat-btn:hover {
  background-color: #1976D2;
}

.search-container {
  padding: 12px;
  background-color: white;
  border-bottom: 1px solid #e0e0e0;
  position: relative;
}

.search-input {
  width: 100%;
  padding: 8px 12px 8px 32px;
  border: 1px solid #ddd;
  border-radius: 20px;
  font-size: 14px;
  background-color: #f9f9f9;
  transition: all 0.2s;
}

.search-input:focus {
  outline: none;
  border-color: #2196F3;
  background-color: white;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.clear-search-btn {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  color: #999;
  padding: 4px;
}

.clear-search-btn:hover {
  color: #666;
}

.conversations-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
  color: #999;
}

.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  margin: 0 8px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}

.conversation-item:hover {
  background-color: #e3f2fd;
}

.conversation-item.active {
  background-color: #bbdefb;
}

.conversation-content {
  flex: 1;
  min-width: 0;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.conversation-preview {
  flex: 1;
  min-width: 0;
}

.conversation-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-snippet {
  margin: 0;
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  margin-left: 8px;
}

.conversation-time {
  font-size: 11px;
  color: #999;
}

.unread-count {
  background-color: #f44336;
  color: white;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.conversation-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.conversation-item:hover .conversation-actions {
  opacity: 1;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  font-size: 12px;
  transition: background-color 0.2s;
}

.action-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.conversation-footer {
  padding: 12px 16px;
  border-top: 1px solid #e0e0e0;
  background-color: white;
  font-size: 12px;
  color: #666;
}

.total-conversations {
  font-weight: 500;
}

/* 滚动条样式 */
.conversations-container::-webkit-scrollbar {
  width: 6px;
}

.conversations-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.conversations-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.conversations-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>