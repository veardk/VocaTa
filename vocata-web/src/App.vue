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
  font-family: 'CustomFont';
  font-size: 0.2rem;
}

#app {
  min-height: 100vh;
}
</style>
