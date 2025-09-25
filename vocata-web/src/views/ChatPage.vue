<template>
  <div>
    <div :class="isM ? 'mobile' : 'pc'">chat</div>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
const isM = computed(() => isMobile())
// 这里可以放全局逻辑
</script>

<style lang="scss" scoped>
.pc {
  font-size: 1rem;
}
</style>
