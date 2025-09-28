// 角色图像加载工具

// 支持的图像文件扩展名
const IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.gif', '.svg'];

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
  const basePath = '/Character/';
  
  // 返回基于名称的图像路径
  return `${basePath}${characterName}`;
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
  
  // 使用Vite配置的别名路径
  const basePath = '/Character/';
  
  for (const ext of IMAGE_EXTENSIONS) {
    const path = `${basePath}${characterName}${ext}`;
    const exists = await checkCharacterImageExists(path);
    if (exists) {
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
  
  console.log('getCharacterAvatarPathSync: 处理角色名:', characterName);
  
  // 确保扩展名以点开头
  const ext = extension.startsWith('.') ? extension : `.${extension}`;
  console.log('getCharacterAvatarPathSync: 使用扩展名:', ext);
  
  // 检查角色名是否包含中文字符
  const hasChineseChars = /[\u4e00-\u9fa5]/.test(characterName);
  console.log('getCharacterAvatarPathSync: 角色名包含中文字符:', hasChineseChars);
  
  // 对包含中文字符的角色名进行URL编码
  const encodedCharacterName = hasChineseChars ? encodeURIComponent(characterName) : characterName;
  console.log('getCharacterAvatarPathSync: 编码后的角色名:', encodedCharacterName);
  
  // 直接使用相对于resource目录的路径
  const avatarPath = `/Character/${encodedCharacterName}${ext}`;
  console.log('getCharacterAvatarPathSync: 最终生成的头像路径:', avatarPath);
  
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