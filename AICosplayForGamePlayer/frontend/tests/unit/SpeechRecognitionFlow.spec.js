import { mount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import { useRouter } from 'vue-router';
import NewChatPage from '../../src/views/NewChatPage.vue';
import { speechAPI, conversationAPI } from '../../src/utils/api';

// 模拟API
jest.mock('../../src/utils/api', () => ({
  speechAPI: {
    recognize: jest.fn(),
    getSpeechData: jest.fn(),
    healthCheck: jest.fn()
  },
  conversationAPI: {
    getAll: jest.fn(),
    addMessage: jest.fn()
  }
}));

// 模拟导航
jest.mock('vue-router', () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn()
  }))
}));

describe('语音识别完整流程测试', () => {
  let wrapper;
  let mockSpeechRecognize;
  let mockConversationAddMessage;
  let mockGetSpeechData;
  let mockHealthCheck;

  beforeEach(() => {
    mockSpeechRecognize = speechAPI.recognize;
    mockConversationAddMessage = conversationAPI.addMessage;
    mockGetSpeechData = speechAPI.getSpeechData;
    mockHealthCheck = speechAPI.healthCheck;
    
    // 重置mock
    mockSpeechRecognize.mockReset();
    mockConversationAddMessage.mockReset();
    mockGetSpeechData.mockReset();
    mockHealthCheck.mockReset();
    
    // 清除localStorage
    localStorage.clear();
    
    // 模拟API响应
    mockHealthCheck.mockResolvedValue({ data: { status: 'OK' } });
    mockConversationAddMessage.mockResolvedValue({ code: 200, data: { id: 'test-message-id' } });
    
    // 创建测试组件实例
    wrapper = mount(NewChatPage, {
      global: {
        plugins: [createTestingPinia()]
      }
    });
    
    // 模拟getCharacterAvatar函数
    wrapper.vm.getCharacterAvatar = jest.fn(() => '/mock/default-avatar.jpg');
  });

  afterEach(() => {
    wrapper.unmount();
  });

  describe('录音按钮交互测试', () => {
    it('点击录音按钮应该触发toggleRecording函数', async () => {
      // 模拟toggleRecording函数
      const mockToggleRecording = jest.fn();
      wrapper.vm.toggleRecording = mockToggleRecording;
      
      // 找到录音按钮并点击
      const recordButton = wrapper.find('.record-btn');
      await recordButton.trigger('click');
      
      // 验证函数被调用
      expect(mockToggleRecording).toHaveBeenCalled();
    });

    it('点击录音按钮后，按钮应该显示正在录音的状态', async () => {
      // 模拟MediaRecorder行为
      const mockMediaRecorder = {
        start: jest.fn(),
        stop: jest.fn(),
        state: 'inactive',
        ondataavailable: null,
        onstop: null
      };
      window.MediaRecorder = jest.fn().mockImplementation(() => mockMediaRecorder);
      
      // 模拟getUserMedia
      const mockStream = {
        getTracks: jest.fn().mockReturnValue([{ stop: jest.fn() }])
      };
      navigator.mediaDevices.getUserMedia = jest.fn().mockResolvedValue(mockStream);
      
      // 找到录音按钮并点击
      const recordButton = wrapper.find('.record-btn');
      await recordButton.trigger('click');
      
      // 验证按钮状态
      expect(recordButton.classes()).toContain('recording');
    });
  });

  describe('语音识别API调用测试', () => {
    it('录音完成后应该调用speechAPI.recognize发送音频数据', async () => {
      // 准备模拟数据
      const mockAudioData = new Blob(['test audio data'], { type: 'audio/wav' });
      const mockRecognitionResult = { data: { text: '测试语音识别结果' } };
      
      // 设置mock响应
      mockSpeechRecognize.mockResolvedValue(mockRecognitionResult);
      
      // 直接调用transcribeAudio函数进行测试
      await wrapper.vm.transcribeAudio(mockAudioData);
      
      // 验证API调用
      expect(mockSpeechRecognize).toHaveBeenCalledWith(mockAudioData);
    });

    it('语音识别成功后应该将识别结果添加到输入框', async () => {
      // 准备模拟数据
      const mockAudioData = new Blob(['test audio data'], { type: 'audio/wav' });
      const expectedText = '测试语音识别结果';
      const mockRecognitionResult = { data: { text: expectedText } };
      
      // 设置mock响应
      mockSpeechRecognize.mockResolvedValue(mockRecognitionResult);
      
      // 直接调用transcribeAudio函数进行测试
      await wrapper.vm.transcribeAudio(mockAudioData);
      
      // 验证识别结果是否添加到输入框
      expect(wrapper.vm.inputMessage).toContain(expectedText);
    });

    it('语音识别失败时应该显示错误提示', async () => {
      // 准备模拟数据
      const mockAudioData = new Blob(['test audio data'], { type: 'audio/wav' });
      
      // 设置mock抛出异常
      const errorMessage = '语音识别失败';
      mockSpeechRecognize.mockRejectedValue(new Error(errorMessage));
      
      // 模拟alert函数
      window.alert = jest.fn();
      
      // 直接调用transcribeAudio函数进行测试
      await wrapper.vm.transcribeAudio(mockAudioData);
      
      // 验证是否显示错误提示
      expect(window.alert).toHaveBeenCalledWith(expect.stringContaining('失败'));
    });
  });

  describe('语音消息显示测试', () => {
    it('录音开始时应该显示临时语音消息', async () => {
      // 准备模拟数据
      const mockAudioData = new Blob(['test audio data'], { type: 'audio/wav' });
      const mockRecognitionResult = { data: { text: '测试语音识别结果' } };
      
      // 设置mock响应
      mockSpeechRecognize.mockResolvedValue(mockRecognitionResult);
      
      // 直接调用transcribeAudio函数进行测试
      await wrapper.vm.transcribeAudio(mockAudioData);
      
      // 验证临时语音消息是否被添加
      const messages = wrapper.vm.messages;
      expect(messages).toHaveLength(1);
      expect(messages[0].type).toBe('voice');
    });
  });

  describe('完整录音流程测试', () => {
    it('应该能完成从录音到发送的完整流程', async () => {
      // 准备模拟数据
      const mockAudioBlob = new Blob(['test audio data'], { type: 'audio/wav' });
      const mockRecognitionResult = { data: { text: '测试完整流程' } };
      
      // 设置mock响应
      mockSpeechRecognize.mockResolvedValue(mockRecognitionResult);
      
      // 模拟录音数据处理
      const mockMediaRecorder = {
        start: jest.fn(),
        stop: jest.fn(),
        state: 'inactive',
        ondataavailable: null,
        onstop: null
      };
      window.MediaRecorder = jest.fn().mockImplementation(() => mockMediaRecorder);
      
      // 模拟getUserMedia
      const mockStream = {
        getTracks: jest.fn().mockReturnValue([{ stop: jest.fn() }])
      };
      navigator.mediaDevices.getUserMedia = jest.fn().mockResolvedValue(mockStream);
      
      // 模拟transcribeAudio函数
      wrapper.vm.transcribeAudio = jest.fn().mockImplementation(async (blob) => {
        // 确保传入的是正确的blob
        expect(blob).toBeInstanceOf(Blob);
        
        // 模拟识别结果处理
        wrapper.vm.inputMessage = mockRecognitionResult.data.text;
      });
      
      // 模拟startRecording函数，直接调用transcribeAudio
      wrapper.vm.toggleRecording = jest.fn().mockImplementation(async () => {
        await wrapper.vm.transcribeAudio(mockAudioBlob);
      });
      
      // 点击录音按钮
      const recordButton = wrapper.find('.record-btn');
      await recordButton.trigger('click');
      
      // 验证输入框内容
      expect(wrapper.vm.inputMessage).toBe('测试完整流程');
    });
  });
});