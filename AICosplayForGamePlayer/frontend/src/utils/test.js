import { conversationAPI } from './api.js'

// 测试创建新对话
export const testCreateConversation = async () => {
  try {
    console.log('开始测试创建新对话...');
    const newConversation = await conversationAPI.create('测试对话');
    console.log('创建新对话成功:', newConversation);
    return newConversation;
  } catch (error) {
    console.error('创建新对话失败:', error);
    // 打印完整的错误对象，包括响应头、状态码等
    console.error('完整错误信息:', error.response ? error.response : error);
    throw error;
  }
}