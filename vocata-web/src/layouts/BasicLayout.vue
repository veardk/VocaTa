<template>
  <div class="main-layout" :class="{ mobile: isMobile, 'sidebar-collapsed': sidebarCollapsed }">
    <SliderBar :sidebarCollapsed="sidebarCollapsed" @toggleSidebar="toggleSidebar" />

    <main class="main-content">
      <div class="mobile-header" v-if="isM && sidebarCollapsed">
        <div class="toggle-btn" @click="toggleSidebar" aria-label="展开侧边栏">
          <el-icon>
            <Menu />
          </el-icon>
        </div>
      </div>
      <router-view class="router-view-content" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, onMounted, ref } from 'vue'
import SliderBar from './SliderBar.vue'
const isM = computed(() => isMobile())
const sidebarCollapsed = ref(false)

onMounted(() => {
  // 移动端默认收起侧边栏
  if (isM.value) {
    sidebarCollapsed.value = true
  }
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}
</script>

<style lang="scss">
.main-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: auto;
  background-color: #f3f3f3;
  font-size: 0.2rem;
  transition: all 0.3s ease;
}
.main-content {
  flex: 1;
  height: 100%;
  overflow: auto;
  transition: margin-left 0.3s ease;

  .router-view-content {
    height: 100%;
    overflow-y: auto;
  }
}

.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.12rem 0.15rem;

  .toggle-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 0.35rem;
    height: 0.35rem;
    border-radius: 50%;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f5f5;
    }
  }
}
</style>
