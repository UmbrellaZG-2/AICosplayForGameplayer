<template>
  <div class="character-selector">
    <!-- 角色选择器标题 -->
    <div class="selector-header">
      <h3>选择角色</h3>
      <button 
        class="close-btn" 
        @click="closeSelector"
        v-if="showCloseBtn"
        title="关闭"
      >
        ✕
      </button>
    </div>
    
    <!-- 搜索框 -->
    <div class="search-container">
      <input 
        type="text" 
        class="search-input" 
        placeholder="搜索角色..." 
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
    
    <!-- 角色列表 -->
    <div class="characters-container">
      <div 
        v-if="filteredCharacters.length === 0" 
        class="empty-state"
      >
        <p>{{ searchQuery ? '没有找到匹配的角色' : '暂无角色数据' }}</p>
      </div>
      
      <div 
        v-for="character in filteredCharacters" 
        :key="character.name"
        class="character-item"
        :class="{ 'selected': selectedCharacter?.name === character.name }"
        @click="selectCharacter(character)"
      >
        <!-- 角色头像 -->
        <div class="character-avatar">
          <img 
            :src="getCharacterAvatar(character)" 
            :alt="character.name" 
            class="avatar-image"
            @error="handleAvatarError($event, character)"
          >
          <div v-if="character.isNew" class="new-badge">新</div>
        </div>
        
        <!-- 角色信息 -->
        <div class="character-info">
          <h4 class="character-name">{{ character.name }}</h4>
          <p class="character-description">{{ character.description || '暂无描述' }}</p>
          
          <!-- 角色标签 -->
          <div class="character-tags" v-if="character.tags && character.tags.length > 0">
            <span v-for="tag in character.tags.slice(0, 3)" :key="tag" class="tag">
              {{ tag }}
            </span>
            <span v-if="character.tags.length > 3" class="tag more-tags">
              +{{ character.tags.length - 3 }}
            </span>
          </div>
        </div>
        
        <!-- 选择指示器 -->
        <div 
          v-if="selectedCharacter?.name === character.name" 
          class="selected-indicator"
        >
          ✓
        </div>
      </div>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载角色中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

// Props
const props = defineProps({
  characters: {
    type: Array,
    required: true,
    default: () => []
  },
  selectedCharacter: {
    type: Object,
    default: null
  },
  isLoading: {
    type: Boolean,
    default: false
  },
  showCloseBtn: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['selectCharacter', 'closeSelector', 'loadCharacters'])

// 状态管理
const searchQuery = ref('')

// 计算过滤后的角色列表
const filteredCharacters = computed(() => {
  if (!searchQuery.value.trim()) {
    // 如果没有搜索词，返回所有角色
    return props.characters
  }
  
  // 否则根据搜索词过滤角色
  const query = searchQuery.value.toLowerCase()
  return props.characters.filter(character => {
    // 检查角色名称是否包含搜索词
    if (character.name && character.name.toLowerCase().includes(query)) {
      return true
    }
    
    // 检查角色描述是否包含搜索词
    if (character.description && character.description.toLowerCase().includes(query)) {
      return true
    }
    
    // 检查角色标签是否包含搜索词
    if (character.tags && character.tags.some(tag => tag.toLowerCase().includes(query))) {
      return true
    }
    
    return false
  })
})

// 获取角色头像路径
const getCharacterAvatar = (character) => {
  if (!character) return '/default-character.png'
  
  // 尝试获取角色头像，优先使用jpg格式
  return `/Character/${character.name}.jpg`
}

// 处理头像加载失败
const handleAvatarError = (event, character) => {
  if (event && event.target) {
    const img = event.target;
    
    // 检查是否已经尝试过两种格式
    if (!img.dataset.triedBothFormats) {
      // 如果当前是jpg格式，尝试png格式
      const currentSrc = img.src;
      if (currentSrc.endsWith('.jpg')) {
        const newSrc = currentSrc.replace('.jpg', '.png');
        img.dataset.triedBothFormats = 'true';
        img.src = newSrc;
      }
    } else {
      // 如果两种格式都尝试过仍然失败，使用默认头像
      console.log(`角色 ${character.name} 的头像加载失败，使用默认头像`);
      img.style.border = '2px solid red'; // 添加红色边框表示加载失败
      img.src = '/default-character.png'; // 默认头像路径
    }
  }
}

// 处理搜索
const handleSearch = () => {
  console.log('搜索角色:', searchQuery.value)
}

// 清除搜索
const clearSearch = () => {
  searchQuery.value = ''
}

// 选择角色
const selectCharacter = (character) => {
  emit('selectCharacter', character)
}

// 关闭角色选择器
const closeSelector = () => {
  emit('closeSelector')
}

// 组件挂载时加载角色列表
onMounted(() => {
  // 只有当没有角色数据时才触发加载
  if (props.characters.length === 0) {
    emit('loadCharacters')
  }
})
</script>

<style scoped>
.character-selector {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  overflow: hidden;
}

.selector-header {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #f5f5f5;
}

.selector-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #666;
  padding: 4px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.search-container {
  padding: 12px;
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

.characters-container {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
  color: #999;
}

.character-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
  background-color: #f9f9f9;
}

.character-item:hover {
  background-color: #e3f2fd;
  transform: translateX(2px);
}

.character-item.selected {
  border-color: #2196F3;
  background-color: #bbdefb;
}

.character-avatar {
  width: 60px;
  height: 60px;
  border-radius: 10px;
  overflow: hidden;
  margin-right: 12px;
  flex-shrink: 0;
  position: relative;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border: 2px solid #ddd;
  transition: border-color 0.2s;
}

.character-item:hover .avatar-image {
  border-color: #2196F3;
}

.new-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  background-color: #f44336;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 10px;
  border: 2px solid white;
}

.character-info {
  flex: 1;
  min-width: 0;
}

.character-name {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.character-description {
  margin: 0 0 6px 0;
  font-size: 12px;
  color: #666;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.character-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag {
  background-color: #e0e0e0;
  color: #666;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
}

.tag.more-tags {
  background-color: #f5f5f5;
  color: #999;
}

.selected-indicator {
  font-size: 20px;
  color: #2196F3;
  font-weight: bold;
  margin-left: 8px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #666;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #2196F3;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 滚动条样式 */
.characters-container::-webkit-scrollbar {
  width: 6px;
}

.characters-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.characters-container::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 3px;
}

.characters-container::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>