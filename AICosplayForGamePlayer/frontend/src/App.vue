<template>
  <div class="app-container">
    <router-view />
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
  height: 100vh;
  width: 100vw;
}
</style>