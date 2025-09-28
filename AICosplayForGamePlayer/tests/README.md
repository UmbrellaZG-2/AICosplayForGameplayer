# 语音识别功能测试说明

本目录包含语音识别功能的完整测试套件，覆盖前端用户交互、API调用到后端服务处理的全流程。

## 测试文件结构

### 前端测试
- `frontend/tests/unit/SpeechRecognitionFlow.spec.js` - 测试前端录音和语音识别交互流程

### 后端测试
- `backend/src/test/java/com/aicosplay/service/impl/VoskOfflineSpeechServiceTest.java` - 测试离线语音识别服务
- `backend/src/test/java/com/aicosplay/service/impl/SpeechServiceImplTest.java` - 测试在线语音识别服务
- `backend/src/test/java/com/aicosplay/controller/SpeechControllerTest.java` - 测试语音识别接口处理
- `backend/src/test/java/com/aicosplay/service/SpeechRecognitionDispatcherTest.java` - 测试语音识别调度逻辑

## 测试内容说明

### 前端测试覆盖
1. 录音按钮交互功能
2. 语音识别API调用
3. 语音消息显示
4. 完整录音流程（开始、停止、转文本）

### 后端测试覆盖
1. 离线语音识别功能（Vosk）
2. 在线语音识别功能（讯飞API）
3. 语音识别调度逻辑（离线优先，失败切换在线）
4. API接口处理和异常情况
5. 服务健康检查和状态管理

## 运行测试

### 前端测试
```bash
# 在frontend目录下运行
npm run test:unit
```

### 后端测试
```bash
# 在backend目录下运行
mvn test
```

## 测试技术栈

### 前端
- Jest - JavaScript测试框架
- Vue Test Utils - Vue组件测试工具

### 后端
- JUnit 5 - Java测试框架
- Mockito - Java模拟框架
- Spring Boot Test - Spring Boot测试支持

## 测试注意事项

1. 后端测试使用模拟对象避免实际API调用和模型加载
2. 确保测试环境中的配置文件包含必要的测试参数
3. 运行测试前请确保所有依赖已正确安装

## 测试目标

1. 确保语音识别功能在各种场景下正常工作
2. 验证服务切换和错误处理机制的正确性
3. 保障前端用户体验的一致性和稳定性
4. 为功能迭代和代码重构提供安全保障