<template>
  <div class="about-container">
    <!-- 页面头部 -->
    <header class="about-header">
      <div class="header-content">
        <h1>AICosplayForGamePlayer</h1>
        <p>游戏玩家的智能角色扮演助手</p>
      </div>
    </header>

    <!-- 主要内容区域 -->
    <main class="about-main">
      <!-- 项目介绍 -->
      <section class="about-section">
        <h2>项目介绍</h2>
        <div class="section-content">
          <p>AICosplayForGamePlayer 是一款专为游戏玩家设计的智能角色扮演助手，它能够帮助玩家在游戏中创造丰富的角色互动体验。</p>
          <p>通过先进的人工智能技术，我们的系统能够理解游戏场景、角色背景和对话上下文，生成符合角色设定的自然语言回复，让游戏体验更加沉浸和丰富。</p>
        </div>
      </section>

      <!-- 核心功能 -->
      <section class="about-section features">
        <h2>核心功能</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">🎭</div>
            <h3>智能角色扮演</h3>
            <p>根据游戏角色设定生成符合身份的对话内容，支持多轮对话和上下文理解</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">💬</div>
            <h3>自然语言交互</h3>
            <p>支持自然流畅的中文对话，理解玩家意图并提供恰当的回应</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">📝</div>
            <h3>对话历史管理</h3>
            <p>自动保存和管理对话历史，支持创建、切换和删除不同的对话场景</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🛡️</div>
            <h3>安全内容过滤</h3>
            <p>内置多层安全过滤机制，确保生成内容符合规范</p>
          </div>
        </div>
      </section>

      <!-- 技术架构 -->
      <section class="about-section">
        <h2>技术架构</h2>
        <div class="section-content">
          <div class="architecture-grid">
            <div class="architecture-item">
              <h3>前端</h3>
              <ul>
                <li>Vue 3 + Composition API</li>
                <li>Vue Router 4</li>
                <li>Vite 构建工具</li>
                <li>Axios 网络请求</li>
              </ul>
            </div>
            <div class="architecture-item">
              <h3>后端</h3>
              <ul>
                <li>Spring Boot</li>
                <li>Spring Security</li>
                <li>安全过滤链系统</li>
              </ul>
            </div>
            <div class="architecture-item">
              <h3>AI 能力</h3>
              <ul>
                <li>大语言模型集成</li>
                <li>上下文理解</li>
                <li>角色扮演优化</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <!-- 安全特性 -->
      <section class="about-section security">
        <h2>安全特性</h2>
        <div class="section-content">
          <p>我们高度重视用户体验和内容安全，实现了一套完整的安全过滤链系统：</p>
          <div class="security-features">
            <div class="security-item">
              <h4>输入安全检查</h4>
              <p>检测并过滤提示词注入、敏感内容和有害请求</p>
            </div>
            <div class="security-item">
              <h4>输出安全检查</h4>
              <p>识别并处理模型幻觉、不当内容和不符合规范的回复</p>
            </div>
            <div class="security-item">
              <h4>多层过滤机制</h4>
              <p>采用责任链模式，实现可扩展的多层安全过滤</p>
            </div>
          </div>
        </div>
      </section>

      <!-- 团队介绍 -->
      <section class="about-section team">
        <h2>开发团队</h2>
        <div class="team-members">
          <div class="team-member">
            <div class="member-avatar">👨‍💻</div>
            <h3>架构师</h3>
            <p>负责系统整体架构设计和技术选型</p>
          </div>
          <div class="team-member">
            <div class="member-avatar">👩‍💻</div>
            <h3>前端开发</h3>
            <p>构建用户友好的交互界面和流畅的用户体验</p>
          </div>
          <div class="team-member">
            <div class="member-avatar">👨‍🔧</div>
            <h3>后端开发</h3>
            <p>实现高性能、可靠的服务端系统</p>
          </div>
          <div class="team-member">
            <div class="member-avatar">🧠</div>
            <h3>AI 研究员</h3>
            <p>优化语言模型和角色扮演能力</p>
          </div>
        </div>
      </section>
    </main>

    <!-- 底部导航 -->
    <footer class="about-footer">
      <div class="footer-links">
        <router-link to="/" class="footer-link">登录</router-link>
        <router-link to="/register" class="footer-link">注册</router-link>
        <router-link to="/chat" class="footer-link">开始聊天</router-link>
      </div>
      <p class="copyright">© 2025 AICosplayForGamePlayer. 保留所有权利。</p>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
</script>

<style scoped>
/* 容器样式 */
.about-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

/* 头部样式 */
.about-header {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  padding: 60px 20px;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.header-content h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
  font-weight: 600;
}

.header-content p {
  font-size: 1.2rem;
  opacity: 0.9;
  max-width: 600px;
  margin: 0 auto;
}

/* 主内容区域样式 */
.about-main {
  flex: 1;
  padding: 40px 20px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}

/* 通用区块样式 */
.about-section {
  background-color: white;
  border-radius: 12px;
  padding: 40px;
  margin-bottom: 30px;
  box-shadow: 0 2px 15px rgba(0, 0, 0, 0.05);
}

.about-section h2 {
  color: #333;
  margin-bottom: 20px;
  font-size: 1.8rem;
  font-weight: 600;
  position: relative;
  padding-bottom: 10px;
}

.about-section h2::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 60px;
  height: 4px;
  background-color: #4CAF50;
  border-radius: 2px;
}

.section-content {
  color: #666;
  line-height: 1.6;
}

.section-content p {
  margin-bottom: 15px;
}

/* 功能特性网格 */
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 30px;
}

.feature-card {
  background-color: #f8f9fa;
  padding: 25px;
  border-radius: 8px;
  text-align: center;
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid #e0e0e0;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08);
}

.feature-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.feature-card h3 {
  color: #333;
  margin-bottom: 10px;
  font-size: 1.2rem;
}

.feature-card p {
  color: #666;
  font-size: 0.95rem;
}

/* 技术架构网格 */
.architecture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.architecture-item {
  background-color: #f0f2f5;
  padding: 20px;
  border-radius: 8px;
}

.architecture-item h3 {
  color: #333;
  margin-bottom: 15px;
  font-size: 1.1rem;
}

.architecture-item ul {
  list-style: none;
  padding: 0;
}

.architecture-item li {
  color: #666;
  margin-bottom: 8px;
  padding-left: 20px;
  position: relative;
}

.architecture-item li::before {
  content: '•';
  color: #4CAF50;
  font-weight: bold;
  position: absolute;
  left: 0;
}

/* 安全特性 */
.security-features {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-top: 20px;
}

.security-item {
  background-color: #e8f5e8;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #4CAF50;
}

.security-item h4 {
  color: #333;
  margin-bottom: 5px;
  font-size: 1rem;
}

.security-item p {
  color: #666;
  font-size: 0.9rem;
  margin: 0;
}

/* 团队成员 */
.team-members {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 30px;
}

.team-member {
  text-align: center;
  padding: 20px;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.member-avatar {
  font-size: 3.5rem;
  margin-bottom: 15px;
}

.team-member h3 {
  color: #333;
  margin-bottom: 5px;
  font-size: 1.1rem;
}

.team-member p {
  color: #666;
  font-size: 0.9rem;
  margin: 0;
}

/* 底部样式 */
.about-footer {
  background-color: #333;
  color: white;
  padding: 30px 20px;
  text-align: center;
}

.footer-links {
  margin-bottom: 20px;
}

.footer-link {
  color: white;
  text-decoration: none;
  margin: 0 15px;
  font-size: 1rem;
  transition: color 0.3s;
}

.footer-link:hover {
  color: #4CAF50;
}

.copyright {
  color: #ccc;
  font-size: 0.9rem;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .about-header {
    padding: 40px 20px;
  }
  
  .header-content h1 {
    font-size: 2rem;
  }
  
  .about-main {
    padding: 20px 15px;
  }
  
  .about-section {
    padding: 30px 20px;
  }
  
  .features-grid,
  .architecture-grid,
  .team-members {
    grid-template-columns: 1fr;
  }
  
  .footer-link {
    display: block;
    margin: 10px 0;
  }
}
</style>