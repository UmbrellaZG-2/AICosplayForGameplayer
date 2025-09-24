# AICosplayForGameplayer

## 项目介绍
AICosplayForGameplayer是一款基于大语言模型的游戏角色角色扮演聊天应用，允许用户与各种游戏角色进行沉浸式对话交流。

## 技术栈

### 前端
- **框架**：Vue 3 + Vite
- **路由**：Vue Router 4
- **HTTP客户端**：Axios
- **音频处理**：wavesurfer.js
- **构建工具**：Vite

### 后端
- **框架**：Spring Boot
- **ORM**：Spring Data JPA
- **AI集成**：Spring AI
- **安全过滤**：自定义安全过滤链
- **数据库**：MySQL
- **大语言模型**：deepseek-V3

## 项目结构

### 前端结构
```
frontend/
├── index.html                # 入口HTML文件
├── package.json              # 前端依赖配置
├── src/
│   ├── App.vue               # 根组件
│   ├── main.js               # 应用入口文件
│   ├── components/           # 通用组件
│   │   └── Notification.vue  # 通知组件
│   ├── router/               # 路由配置
│   │   └── index.js          # 路由定义和守卫
│   ├── utils/                # 工具函数
│   │   ├── api.js            # API请求封装
│   │   └── test.js           # 测试工具
│   └── views/                # 页面组件
│       ├── AboutPage.vue     # 关于页面
│       ├── ChatPage.vue      # 聊天页面（核心功能）
│       ├── LoginPage.vue     # 登录页面
│       └── RegisterPage.vue  # 注册页面
└── vite.config.js            # Vite配置文件
```

### 后端结构
```
backend/
├── pom.xml                   # Maven依赖配置
└── src/main/
    ├── java/com/aicosplay/   # Java源码目录
    │   ├── BackendApplication.java  # 应用主入口
    │   ├── config/           # 配置类
    │   ├── controller/       # 控制器
    │   ├── entity/           # 实体类
    │   ├── repository/       # 数据访问层
    │   ├── security/         # 安全相关
    │   └── service/          # 服务层
    └── resources/            # 资源文件
        └── application.properties  # 应用配置
```

## 核心功能

### 1. 用户认证与管理
- 用户注册：创建账号、设置密码、邮箱验证
- 用户登录：账号密码登录、JWT身份验证
- 用户信息管理：昵称、头像等个人信息修改

### 2. 游戏角色系统
- 预设角色：内置多种热门游戏角色（如赛马娘无声铃鹿、魔女多萝西、魔法少女莱万提亚）
- 自定义角色：用户可创建个性化游戏角色，设置名称、描述和头像
- 角色管理：查看、使用和删除角色

### 3. 对话系统
- 对话创建：选择角色开始新对话
- 消息发送：支持文本输入和语音输入
- 对话历史：保存和管理历史对话记录
- 上下文理解：AI能够根据对话历史提供连贯的回复

### 4. 语音交互
- 语音录制：支持用户录制语音消息
- 语音识别：将语音转换为文本进行处理
- 语音可视化：使用wavesurfer.js提供音频波形可视化

### 5. 安全过滤
- 输入安全检查：防止提示词注入和不良内容
- 输出安全检查：检测模型幻觉和确保回答符合规范
- 多级过滤链：实现全方位的内容安全保障

## 数据库设计

### 主要数据表

1. **user表**：存储用户信息
   - id：用户ID
   - username：用户名
   - password：密码（加密存储）
   - email：邮箱
   - nickname：昵称
   - avatar：头像URL
   - created_at：创建时间
   - updated_at：更新时间
   - status：状态

2. **conversation表**：存储对话信息
   - id：对话ID
   - user_id：所属用户ID
   - title：对话标题
   - character_name：角色名称
   - created_at：创建时间
   - updated_at：最后更新时间
   - is_deleted：是否已删除

3. **message表**：存储具体的消息内容
   - id：消息ID
   - conversation_id：所属对话ID
   - sender_type：发送者类型（用户/AI）
   - content：消息内容
   - created_at：发送时间

4. **game_character表**：存储可选择的游戏角色信息
   - id：角色ID
   - name：角色名称
   - prompt：角色对话提示词
   - is_preset：是否为预设角色
   - user_id：创建者用户ID
   - image_path：角色图片文件路径
   - image_data：角色图片二进制数据
   - created_at：创建时间
   - updated_at：更新时间

5. **user_character表**：存储用户与游戏角色的关联关系
   - id：关联ID
   - user_id：用户ID
   - character_id：角色ID
   - created_at：关联时间

## API接口

### 认证相关API
- POST /api/auth/login：用户登录
- POST /api/auth/register：用户注册

### 对话相关API
- POST /api/conversations：创建新对话
- GET /api/conversations：获取所有对话
- GET /api/conversations/{id}：获取对话详情
- DELETE /api/conversations/{id}：删除对话
- GET /api/conversations/{id}/messages：获取对话消息
- POST /api/conversations/{id}/messages：发送消息

### 游戏角色相关API
- GET /api/characters：获取所有角色
- POST /api/characters：创建新角色

### 语音识别相关API
- POST /api/speech/recognize：语音识别

## 安全机制

项目实现了完善的安全过滤链，包括以下过滤器：

1. **PromptInjectionFilter**：检测并防止提示词注入攻击
2. **RiskContentFilter**：过滤敏感和不良内容
3. **ModelHallucinationFilter**：检测并纠正模型生成的幻觉内容
4. **AnswerAlignmentFilter**：确保AI回答与角色设定一致

## 角色介绍

### 预设角色

1. **赛马娘无声铃鹿**
   - 来自《赛马娘 Pretty Derby》
   - 温柔优雅的赛马娘，对跑步充满热情
   - 标志性台词："我只是想看到前方空无一人的景色"

2. **魔女多萝西**
   - 基于《绿野仙踪》的暗黑重构版本
   - 高傲毒舌，内心复杂矛盾
   - 将对话者视为"笨蛋弟子"

3. **魔法少女莱万提亚**
   - 拥有双重人格（日常模式/直播模式）
   - "人气即力量"是其核心机制
   - 直播数据直接影响魔法强弱

© 2025 AICosplayForGamePlayer Team