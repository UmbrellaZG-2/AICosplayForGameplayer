import { mount } from '@vue/test-utils';
import { createTestingPinia } from '@pinia/testing';
import { useRouter } from 'vue-router';
import NewChatPage from '../../src/views/NewChatPage.vue';
import { speechAPI, conversationAPI, gameCharacterAPI } from '../../src/utils/api';
import { addAvatarPathsToCharacters } from '../../src/utils/characterUtils';

// 模拟API
jest.mock('../../src/utils/api', () => ({
  speechAPI: {
    recognize: jest.fn(),
    getSpeechData: jest.fn()
  },
  conversationAPI: {
    getAll: jest.fn(),
    addMessage: jest.fn()
  },
  gameCharacterAPI: {
    getAll: jest.fn(),
    getById: jest.fn(),
    search: jest.fn()
  }
}));

// 模拟角色工具函数
jest.mock('../../src/utils/characterUtils', () => ({
  addAvatarPathsToCharacters: jest.fn(characters => characters) // 简单地返回输入的角色数据
}));

// 模拟导航
jest.mock('vue-router', () => ({
  useRouter: jest.fn(() => ({
    push: jest.fn()
  }))
}));

describe('NewChatPage 功能测试', () => {
  let wrapper;
  let mockSpeechRecognize;
  let mockConversationAddMessage;
  let mockGetSpeechData;

  beforeEach(() => {
    mockSpeechRecognize = speechAPI.recognize;
    mockConversationAddMessage = conversationAPI.addMessage;
    mockGetSpeechData = speechAPI.getSpeechData;
    
    // 重置mock
    mockSpeechRecognize.mockReset();
    mockConversationAddMessage.mockReset();
    mockGetSpeechData.mockReset();
    
    // 清除localStorage
    localStorage.clear();
    
    // 创建测试组件实例
    wrapper = mount(NewChatPage, {
      global: {
        plugins: [createTestingPinia()]
      }
    });
    
    // 模拟getCharacterAvatar函数，避免在测试环境中抛出错误
    wrapper.vm.getCharacterAvatar = jest.fn(() => '/mock/default-avatar.jpg');
  });

  afterEach(() => {
    wrapper.unmount();
  });

  describe('头像显示功能测试', () => {
    beforeEach(() => {
      // 清除localStorage
      localStorage.clear();
    });

    it('当角色有头像路径时，应该在HTML中显示头像图片', () => {
      // 创建一个模拟的有头像的角色
      const avatarPath = '/Character/existent-avatar.jpg';
      const mockCharacter = {
        id: 'test-character-id',
        name: '测试角色',
        avatar: avatarPath
      };
      
      // 模拟getAll返回包含此角色的数据
      gameCharacterAPI.getAll.mockResolvedValue({
        code: 200,
        data: [mockCharacter]
      });
      
      // 模拟角色工具函数
      addAvatarPathsToCharacters.mockReturnValue([mockCharacter]);
      
      // 挂载组件
      const customWrapper = mount(NewChatPage, {
        global: {
          plugins: [createTestingPinia()]
        }
      });
      
      // 获取组件实例
      const vm = customWrapper.vm;
      
      // 模拟当前角色和对话
      vm.currentConversation = { characterId: 'test-character-id' };
      vm.characters = [mockCharacter];
      vm.currentCharacterAvatar = avatarPath;
      
      // 验证getCharacterAvatar方法返回正确的头像路径
      expect(vm.getCharacterAvatar()).toBe(avatarPath);
    });

    it('当头像加载失败且路径需要修复时，应该尝试修复路径并设置data-fixed属性', () => {
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 创建模拟事件对象 - 注意：这里使用相对路径(不以http或/开头)来触发路径修复逻辑
      const avatarPath = 'Character/broken-avatar.jpg';
      const mockEvent = {
        target: {
          src: avatarPath,
          style: {},
          nextElementSibling: {
            style: {}
          },
          hasAttribute: jest.fn(() => false),
          setAttribute: jest.fn(),
          onload: null,
          onerror: null
        }
      };
      
      // 调用handleAvatarError函数
      vm.handleAvatarError(mockEvent);
      
      // 验证是否尝试修复路径
      expect(mockEvent.target.setAttribute).toHaveBeenCalledWith('data-fixed', 'true');
      // 验证是否尝试设置新的src路径
      expect(mockEvent.target.src).not.toBe(avatarPath);
      // 验证设置了新的onerror处理函数
      expect(typeof mockEvent.target.onerror).toBe('function');
    });
    
    it('当头像加载失败且不需要修复路径时，应该抛出错误', () => {
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 创建模拟事件对象 - 以/开头的路径不会触发路径修复逻辑
      const avatarPath = '/Character/broken-avatar.jpg';
      const mockEvent = {
        target: {
          src: avatarPath,
          style: {},
          nextElementSibling: {
            style: {}
          },
          hasAttribute: jest.fn(() => false),
          setAttribute: jest.fn(),
          onload: null,
          onerror: null
        }
      };
      
      // 验证调用handleAvatarError函数时会抛出错误
      expect(() => {
        vm.handleAvatarError(mockEvent);
      }).toThrow(`Failed to load avatar: ${avatarPath}`);
    });
    
    it('当头像加载失败且已经尝试过修复路径时，应该抛出错误', () => {
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 创建模拟事件对象 - 已经有data-fixed属性
      const avatarPath = 'Character/broken-avatar.jpg';
      const mockEvent = {
        target: {
          src: avatarPath,
          style: {},
          nextElementSibling: {
            style: {}
          },
          hasAttribute: jest.fn(() => true), // 已经尝试过修复
          setAttribute: jest.fn(),
          onload: null,
          onerror: null
        }
      };
      
      // 验证调用handleAvatarError函数时会抛出错误
      expect(() => {
        vm.handleAvatarError(mockEvent);
      }).toThrow(/Failed to load avatar: .+/); // 只验证错误消息包含前缀，不关心具体路径格式
    });
  });

  describe('录音功能测试', () => {
    it('当录音内容为空时，应该弹出提示并移除临时消息', async () => {
      // 模拟alert
      window.alert = jest.fn();
      
      // 模拟语音识别返回空结果
      mockSpeechRecognize.mockResolvedValue({
        data: { text: '' }
      });
      
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 模拟当前对话ID
      vm.currentConversationId = 'test-conversation-id';
      vm.messages = [];
      
      // 创建一个模拟的音频blob
      const mockBlob = new Blob(['test'], { type: 'audio/wav' });
      
      // 调用transcribeAudio函数
      await vm.transcribeAudio(mockBlob);
      
      // 验证alert被调用，显示无法识别语音内容的提示
      expect(window.alert).toHaveBeenCalledWith('无法识别语音内容，请重试');
      
      // 验证临时消息被移除
      expect(vm.messages).toHaveLength(0);
    });

    it('当录音内容不为空时，应该更新临时消息并发送到服务器', async () => {
      // 模拟语音识别返回有内容的结果
      const recognizedText = '测试语音内容';
      mockSpeechRecognize.mockResolvedValue({
        data: { text: recognizedText }
      });
      
      // 模拟对话API返回成功
      mockConversationAddMessage.mockResolvedValue({
        code: 200,
        data: {
          id: 'server-message-id',
          content: recognizedText,
          sender: 'user',
          type: 'voice',
          createdAt: new Date().toISOString()
        }
      });
      
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 模拟当前对话ID
      vm.currentConversationId = 'test-conversation-id';
      vm.messages = [];
      vm.recordingDuration = 5;
      
      // 创建一个模拟的音频blob
      const mockBlob = new Blob(['test'], { type: 'audio/wav' });
      
      // 调用transcribeAudio函数
      await vm.transcribeAudio(mockBlob);
      
      // 验证临时消息被添加并更新
      expect(vm.messages).toHaveLength(1);
      expect(vm.messages[0].recognizedText).toBe(recognizedText);
      
      // 验证对话API被调用
      expect(mockConversationAddMessage).toHaveBeenCalledWith(
        'test-conversation-id',
        {
          content: recognizedText,
          sender: 'user',
          type: 'voice'
        }
      );
    });

    it('当语音识别API调用失败时，应该弹出错误提示并移除临时消息', async () => {
      // 模拟alert
      window.alert = jest.fn();
      
      // 模拟语音识别API调用失败
      const error = new Error('语音识别失败');
      mockSpeechRecognize.mockRejectedValue(error);
      
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 模拟当前对话ID
      vm.currentConversationId = 'test-conversation-id';
      vm.messages = [];
      
      // 创建一个模拟的音频blob
      const mockBlob = new Blob(['test'], { type: 'audio/wav' });
      
      // 调用transcribeAudio函数
      await vm.transcribeAudio(mockBlob);
      
      // 验证alert被调用，显示语音处理失败的提示
      expect(window.alert).toHaveBeenCalledWith('语音处理失败，请重试');
      
      // 验证临时消息被移除
      expect(vm.messages).toHaveLength(0);
    });
  });

  describe('语音播放功能测试', () => {
    it('当播放AI语音消息时，应该从服务器获取语音数据', async () => {
      // 模拟音频数据
      const mockAudioData = new ArrayBuffer(10);
      mockGetSpeechData.mockResolvedValue(mockAudioData);
      
      // 模拟Audio对象
      global.Audio = jest.fn().mockImplementation(() => ({
        play: jest.fn().mockResolvedValue(),
        pause: jest.fn(),
        onended: null
      }));
      
      global.URL = {
        createObjectURL: jest.fn(() => 'blob:url'),
        revokeObjectURL: jest.fn()
      };
      
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 模拟AI语音消息
      const mockMessage = {
        id: 'test-message-id',
        voiceId: 'test-voice-id',
        senderType: 0 // 0表示AI
      };
      
      // 调用playVoiceMessage函数
      await vm.playVoiceMessage(mockMessage);
      
      // 验证getSpeechData被调用
      expect(mockGetSpeechData).toHaveBeenCalledWith('test-voice-id');
      
      // 验证playingVoiceId被设置
      expect(vm.playingVoiceId).toBe('test-message-id');
    });

    it('当语音播放失败时，应该弹出错误提示', async () => {
      // 模拟alert
      window.alert = jest.fn();
      
      // 模拟getSpeechData调用失败
      const error = new Error('获取语音数据失败');
      mockGetSpeechData.mockRejectedValue(error);
      
      // 获取组件实例
      const vm = wrapper.vm;
      
      // 模拟AI语音消息
      const mockMessage = {
        id: 'test-message-id',
        voiceId: 'test-voice-id',
        senderType: 0 // 0表示AI
      };
      
      // 调用playVoiceMessage函数
      await vm.playVoiceMessage(mockMessage);
      
      // 验证alert被调用
      expect(window.alert).toHaveBeenCalledWith('播放语音失败，请重试');
      
      // 验证playingVoiceId被重置为null
      expect(vm.playingVoiceId).toBeNull();
    });
  });

  describe('角色加载功能测试', () => {
      let mockGameCharacterGetAll;
      let mockGameCharacterGetById;
      let mockGameCharacterSearch;

      beforeEach(() => {
        mockGameCharacterGetAll = gameCharacterAPI.getAll;
        mockGameCharacterGetById = gameCharacterAPI.getById;
        mockGameCharacterSearch = gameCharacterAPI.search;
        
        // 重置mock
        mockGameCharacterGetAll.mockReset();
        mockGameCharacterGetById.mockReset();
        mockGameCharacterSearch.mockReset();
        addAvatarPathsToCharacters.mockReset();
      });

      it('当localStorage中有选择的角色ID和角色名时，应该从所有角色中找到对应的角色', async () => {
        // 模拟localStorage中的角色信息
        const mockSelectedRoleId = 'test-role-id';
        const mockSelectedRoleName = '测试角色';
        localStorage.setItem('selectedRoleId', mockSelectedRoleId);
        localStorage.setItem('selectedRoleName', mockSelectedRoleName);
        
        // 模拟getAll返回所有角色，其中包含选择的角色
        const mockAllCharacters = [
          { id: 'role-1', name: '角色1', avatar: null },
          { id: mockSelectedRoleId, name: mockSelectedRoleName, avatar: null },
          { id: 'role-2', name: '角色2', avatar: null }
        ];
        mockGameCharacterGetAll.mockResolvedValue({
          code: 200,
          data: mockAllCharacters
        });
        
        // 模拟addAvatarPathsToCharacters返回原数据
        addAvatarPathsToCharacters.mockImplementation(data => data);
        
        // 获取组件实例
        const vm = wrapper.vm;
        
        // 调用loadCharacters函数
        await vm.loadCharacters();
        
        // 验证API调用
        expect(mockGameCharacterGetAll).toHaveBeenCalled();
        expect(mockGameCharacterGetById).not.toHaveBeenCalled();
        expect(mockGameCharacterSearch).not.toHaveBeenCalled();
        
        // 验证characters数组被正确设置（只包含选择的角色）
        const expectedCharacter = [{ id: mockSelectedRoleId, name: mockSelectedRoleName, avatar: null }];
        expect(vm.characters).toEqual(expectedCharacter);
      });

      it('当localStorage中只有角色名没有ID时，应该从所有角色中找到对应的角色', async () => {
        // 模拟localStorage中的角色信息
        const mockSelectedRoleName = '测试角色';
        localStorage.setItem('selectedRoleName', mockSelectedRoleName);
        
        // 模拟getAll返回所有角色，其中包含选择的角色
        const mockAllCharacters = [
          { id: 'role-1', name: '角色1', avatar: null },
          { id: 'search-result-id', name: mockSelectedRoleName, avatar: null },
          { id: 'role-2', name: '角色2', avatar: null }
        ];
        mockGameCharacterGetAll.mockResolvedValue({
          code: 200,
          data: mockAllCharacters
        });
        
        // 模拟addAvatarPathsToCharacters返回原数据
        addAvatarPathsToCharacters.mockImplementation(data => data);
        
        // 获取组件实例
        const vm = wrapper.vm;
        
        // 调用loadCharacters函数
        await vm.loadCharacters();
        
        // 验证API调用
        expect(mockGameCharacterGetAll).toHaveBeenCalled();
        expect(mockGameCharacterGetById).not.toHaveBeenCalled();
        expect(mockGameCharacterSearch).not.toHaveBeenCalled();
        
        // 验证characters数组被正确设置（只包含选择的角色）
        const expectedCharacter = [{ id: 'search-result-id', name: mockSelectedRoleName, avatar: null }];
        expect(vm.characters).toEqual(expectedCharacter);
      });

      it('当搜索结果为空时，应该使用所有角色', async () => {
        // 模拟localStorage中的角色信息
        const mockSelectedRoleId = 'non-existent-id';
        const mockSelectedRoleName = '不存在的角色';
        localStorage.setItem('selectedRoleId', mockSelectedRoleId);
        localStorage.setItem('selectedRoleName', mockSelectedRoleName);
        
        // 模拟getAll返回所有角色，但不包含选择的角色
        const mockAllCharacters = [
          { id: 'role-1', name: '角色1', avatar: null },
          { id: 'role-2', name: '角色2', avatar: null }
        ];
        mockGameCharacterGetAll.mockResolvedValue({
          code: 200,
          data: mockAllCharacters
        });
        
        // 模拟addAvatarPathsToCharacters返回原数据
        addAvatarPathsToCharacters.mockImplementation(data => data);
        
        // 获取组件实例
        const vm = wrapper.vm;
        
        // 调用loadCharacters函数
        await vm.loadCharacters();
        
        // 验证API调用
        expect(mockGameCharacterGetAll).toHaveBeenCalled();
        
        // 验证characters数组被正确设置（使用所有角色）
        expect(vm.characters).toEqual(mockAllCharacters);
      });

      it('当localStorage中没有选择的角色信息时，应该获取所有角色', async () => {
        // 确保localStorage中没有角色信息
        localStorage.removeItem('selectedRoleName');
        localStorage.removeItem('selectedRoleId');
        
        // 模拟getAll返回所有角色
        const mockAllCharacters = [
          { id: 'role-1', name: '角色1', avatar: null },
          { id: 'role-2', name: '角色2', avatar: null }
        ];
        mockGameCharacterGetAll.mockResolvedValue({
          code: 200,
          data: mockAllCharacters
        });
        
        // 模拟addAvatarPathsToCharacters返回原数据
        addAvatarPathsToCharacters.mockImplementation(data => data);
        
        // 获取组件实例
        const vm = wrapper.vm;
        
        // 调用loadCharacters函数
        await vm.loadCharacters();
        
        // 验证API调用
        expect(mockGameCharacterGetAll).toHaveBeenCalled();
        expect(mockGameCharacterGetById).not.toHaveBeenCalled();
        expect(mockGameCharacterSearch).not.toHaveBeenCalled();
        
        // 验证characters数组被正确设置
        expect(vm.characters).toEqual(mockAllCharacters);
      });
    });
});