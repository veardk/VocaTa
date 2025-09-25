<template>
  <div :class="pc ? 'mobile' : 'pc'" class="main-container">功能待开发</div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
const pc = computed(() => isMobile())
// 这里可以放全局逻辑
</script>

<style lang="scss" scoped>
.main-container {
  padding: 0.3rem 0.5rem;
}
</style>
