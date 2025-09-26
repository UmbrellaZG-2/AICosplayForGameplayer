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
  
  // 构建基础路径
  const basePath = '/Character/';
  
  // 返回基于名称的图像路径
  // 注意：这里不添加扩展名，让浏览器自动匹配存在的文件
  // 在实际使用时，需要处理图像加载失败的情况
  return `${basePath}${characterName}`;
};

/**
 * 检查角色图像是否存在
 * 由于浏览器安全限制，无法直接检查文件是否存在
 * 此函数提供了一种异步方式来验证图像路径
 * @param {string} imagePath - 图像路径
 * @returns {Promise<boolean>} - 图像是否存在
 */
export const checkCharacterImageExists = (imagePath) => {
  return new Promise((resolve) => {
    const img = new Image();
    img.onload = () => resolve(true);
    img.onerror = () => resolve(false);
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
 * 为角色数组添加头像路径
 * @param {Array} characters - 角色对象数组
 * @returns {Array} - 添加了头像路径的角色对象数组
 */
export const addAvatarPathsToCharacters = (characters) => {
  if (!Array.isArray(characters)) {
    return characters;
  }
  
  return characters.map(character => ({
    ...character,
    avatar: getCharacterAvatarPath(character.name) || character.avatar
  }));
};