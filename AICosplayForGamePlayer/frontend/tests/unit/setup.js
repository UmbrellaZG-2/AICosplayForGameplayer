import { createPinia } from 'pinia';
import { config } from '@vue/test-utils';

// 模拟全局对象
Object.defineProperty(window, 'Audio', {
  writable: true,
  value: jest.fn().mockImplementation(() => ({
    play: jest.fn().mockResolvedValue(),
    pause: jest.fn(),
    onended: null,
    src: ''
  }))
});

Object.defineProperty(window, 'URL', {
  writable: true,
  value: {
    createObjectURL: jest.fn(() => 'blob:url'),
    revokeObjectURL: jest.fn()
  }
});

// 模拟getUserMedia
Object.defineProperty(navigator, 'mediaDevices', {
  writable: true,
  value: {
    getUserMedia: jest.fn().mockResolvedValue({
      getTracks: jest.fn().mockReturnValue([{ stop: jest.fn() }])
    })
  }
});

// 模拟MediaRecorder
window.MediaRecorder = jest.fn().mockImplementation(() => ({
  start: jest.fn(),
  stop: jest.fn(),
  state: 'inactive',
  ondataavailable: null,
  onstop: null
}));

// 模拟alert
window.alert = jest.fn();

// 设置默认的全局配置
config.global.plugins.push(createPinia());

// 设置定时器的模拟
jest.useFakeTimers();

afterEach(() => {
  jest.clearAllMocks();
  jest.restoreAllMocks();
});