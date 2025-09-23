<template>
  <div class="app-container">
    <router-view />
    <!-- 全局页脚 -->
    <footer class="global-footer">
      <div class="footer-content">
        <div class="footer-links">
          <router-link to="/about" class="footer-link">关于我们</router-link>
        </div>
        <p class="footer-copyright">© 2023 AICosplayForGamePlayer</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
// App.vue作为根组件，使用router-view渲染路由组件
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

.app-container {
  min-height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
}

/* 全局页脚样式 */
.global-footer {
  background-color: #333;
  color: white;
  padding: 20px 0;
  text-align: center;
  margin-top: auto;
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.footer-links {
  margin-bottom: 10px;
}

.footer-link {
  color: white;
  text-decoration: none;
  margin: 0 10px;
  font-size: 14px;
  transition: color 0.3s;
}

.footer-link:hover {
  color: #4CAF50;
}

.footer-copyright {
  color: #ccc;
  font-size: 12px;
  margin: 0;
}
</style>