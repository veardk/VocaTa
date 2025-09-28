<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
// 这里可以放全局逻辑
</script>

<style lang="scss">
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: 'CustomFont', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

html {
  font-size: 16px; /* 设置根字体大小为标准的16px */
}

body {
  font-size: 14px; /* 设置body的基础字体大小 */
  line-height: 1.5;
  color: #333;
  background-color: #fff;
}

#app {
  min-height: 100vh;
}
</style>
