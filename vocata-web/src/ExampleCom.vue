<template>
  <div>
    <div :class="pc ? 'mobile' : 'pc'"></div>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
const pc = computed(() => isMobile())
// 这里可以放全局逻辑
</script>

<style lang="scss">
.pc {
  font-size: 1rem;
}
</style>
