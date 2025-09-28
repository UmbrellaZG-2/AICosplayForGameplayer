// 角色图像加载工具

// 支持的图像文件扩展名
const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.svg'];

// 头像路径缓存 - 用于存储已解析的头像路径
// 格式: { characterName: { extension: path } }
const AVATAR_CACHE = new Map();

/**
 * 根据角色名称获取头像路径
 * @param {string} characterName - 角色名称
 * @returns {string} - 头像路径
 */
export const getCharacterAvatarPath = (characterName) => {
  if (!characterName || typeof characterName !== 'string') {
    console.warn('getCharacterAvatarPath: 无效的角色名称');
    return null;
  }
  
  // 构建基础路径，使用Vite配置的别名路径
  const basePath = '/resource/Character/';
  
  // 返回基于名称的图像路径
  return `${basePath}${characterName}`;
};

/**
 * 根据角色名称获取头像路径（与用户修改匹配的函数名）
 * @param {string|Object} characterName - 角色名称或角色对象
 * @returns {string} - 头像路径
 */
export const getCharacterAvatarByName = (characterName) => {
  // 增强函数，更好地处理输入参数
  if (!characterName) {
    console.warn('getCharacterAvatarByName: 无效的角色名称或对象');
    return null;
  }
  
  // 如果传入的是对象，尝试从中提取name属性
  const name = typeof characterName === 'object' && characterName.name 
    ? characterName.name 
    : String(characterName);
    
  console.log('getCharacterAvatarByName: 处理角色名:', name);
  
  // 调用同步路径生成函数
  return getCharacterAvatarPathSync(name);
};

/**
 * 检查角色图像是否存在
 * @param {string} imagePath - 图像路径
 * @returns {Promise<boolean>} - 图像是否存在
 */
export const checkCharacterImageExists = async (imagePath) => {
  return new Promise((resolve) => {
    const img = new Image();
    
    // 图像加载成功
    img.onload = () => {
      resolve(true);
    };
    
    // 图像加载失败
    img.onerror = () => {
      resolve(false);
    };
    
    // 设置图像路径
    img.src = imagePath;
  });
};

/**
 * 获取带扩展名的角色头像路径
 * 尝试不同的文件扩展名，直到找到存在的图像
 * @param {string} characterName - 角色名称
 * @returns {Promise<string|null>} - 头像路径或null（如果没有找到）
 */
export const getCharacterAvatarPathWithExtension = async (characterName) => {
  if (!characterName) return null;
  
  // 首先检查缓存
  if (AVATAR_CACHE.has(characterName)) {
    console.log(`Avatar cache hit for ${characterName}`);
    return AVATAR_CACHE.get(characterName);
  }
  
  // 使用Vite配置的别名路径
  const basePath = '/resource/Character/';
  
  for (const ext of IMAGE_EXTENSIONS) {
    const path = `${basePath}${characterName}${ext}`;
    const exists = await checkCharacterImageExists(path);
    if (exists) {
      // 存储到缓存
      AVATAR_CACHE.set(characterName, path);
      console.log(`Avatar path cached for ${characterName}`);
      return path;
    }
  }
  
  console.warn(`getCharacterAvatarPathWithExtension: 未找到角色${characterName}的图像`);
  return null;
};

/**
 * 同步获取带扩展名的角色头像路径（不检查文件是否存在）
 * 直接构建路径，适合前端img标签的onerror机制处理加载失败
 * @param {string} characterName - 角色名称
 * @param {string} extension - 文件扩展名（可选）
 * @returns {string} - 头像路径
 */
export const getCharacterAvatarPathSync = (characterName, extension = 'jpg') => {
  if (!characterName) return null;
  
  // 首先检查缓存
  if (AVATAR_CACHE.has(characterName)) {
    console.log(`Avatar cache hit for ${characterName} (sync)`);
    return AVATAR_CACHE.get(characterName);
  }
  
  console.log('getCharacterAvatarPathSync: 处理角色名:', characterName);
  
  // 确保扩展名以点开头
  const ext = extension.startsWith('.') ? extension : `.${extension}`;
  console.log('getCharacterAvatarPathSync: 使用扩展名:', ext);
  
  // 检查角色名是否包含中文字符
  const hasChineseChars = /[一-龥]/.test(characterName);
  console.log('getCharacterAvatarPathSync: 角色名包含中文字符:', hasChineseChars);
  
  // 对包含中文字符的角色名进行URL编码
  const encodedCharacterName = hasChineseChars ? encodeURIComponent(characterName) : characterName;
  console.log('getCharacterAvatarPathSync: 编码后的角色名:', encodedCharacterName);
  
  // 直接使用相对于resource目录的路径
  const avatarPath = `/resource/Character/${encodedCharacterName}${ext}`;
  console.log('getCharacterAvatarPathSync: 最终生成的头像路径:', avatarPath);
  
  // 存储到缓存
  AVATAR_CACHE.set(characterName, avatarPath);
  console.log(`Avatar path cached for ${characterName} (sync)`);
  
  return avatarPath;
};

/**
 * 为角色数组添加头像路径
 * @param {Array} characters - 角色对象数组
 * @returns {Array} - 添加了头像路径的角色对象数组
 */
export const addAvatarPathsToCharacters = (characters) => {
  if (!Array.isArray(characters)) {
    return characters;
  }
  
  // 定义已知的角色头像文件映射，键为角色名称，值为文件扩展名
  const knownAvatarExtensions = {
    '赛马娘无声铃鹿': 'jpg',
    '魔女多萝西': 'png',
    '魔法少女莱万提亚': 'png'
  };
  
  return characters.map(character => ({
    ...character,
    // 使用同步函数添加带正确扩展名的头像路径
    avatar: getCharacterAvatarPathSync(character.name, knownAvatarExtensions[character.name] || 'jpg') || character.avatar
  }));
};

/**
 * 清除特定角色的头像缓存
 * @param {string} characterName - 角色名称
 */
export const clearAvatarCacheForCharacter = (characterName) => {
  if (characterName && AVATAR_CACHE.has(characterName)) {
    AVATAR_CACHE.delete(characterName);
    console.log(`Avatar cache cleared for ${characterName}`);
  }
};

/**
 * 清除所有头像缓存
 */
export const clearAllAvatarCache = () => {
  const size = AVATAR_CACHE.size;
  AVATAR_CACHE.clear();
  console.log(`All avatar cache cleared. Removed ${size} entries.`);
};

/**
 * 获取头像缓存状态信息
 * @returns {Object} - 缓存状态信息
 */
export const getAvatarCacheStatus = () => {
  return {
    size: AVATAR_CACHE.size,
    characters: Array.from(AVATAR_CACHE.keys())
  };
};

/**
 * 预加载指定角色的头像
 * @param {Array<string>} characterNames - 角色名称数组
 * @returns {Promise<Array<string>>} - 成功预加载的头像路径数组
 */
export const preloadAvatars = async (characterNames) => {
  if (!Array.isArray(characterNames)) {
    return [];
  }
  
  const promises = characterNames.map(async (name) => {
    try {
      const path = await getCharacterAvatarPathWithExtension(name);
      return path;
    } catch (error) {
      console.warn(`Failed to preload avatar for ${name}:`, error);
      return null;
    }
  });
  
  const results = await Promise.all(promises);
  return results.filter(Boolean); // 过滤掉null值
};