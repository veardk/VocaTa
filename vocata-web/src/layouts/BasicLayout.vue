<template>
  <div
    class="main-layout"
    :class="{
      mobile: isMobileDevice,
      'sidebar-collapsed': isSidebarCollapsed,
    }"
  >
    <!-- 侧边栏组件 -->
    <SliderBar :sidebarCollapsed="isSidebarCollapsed" @toggleSidebar="handleToggleSidebar" />

    <!-- 主内容区域 -->
    <main class="main-content">
      <!-- 移动端侧边栏收起时显示的头部 -->
      <div v-if="shouldShowMobileHeader" class="mobile-header">
        <button class="toggle-btn" @click="handleOpenSidebar" aria-label="展开侧边栏">
          <el-icon><Menu /></el-icon>
        </button>
        <button class="toggle-btn" @click="handleExplore" aria-label="探索">探索</button>
      </div>

      <!-- 路由视图 -->
      <router-view class="router-view-content" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { isMobile } from '@/utils/isMobile'
import SliderBar from './SliderBar.vue'

// 响应式数据
const isSidebarCollapsed = ref(false)
const router = useRouter()

// 计算属性
const isMobileDevice = isMobile()
const shouldShowMobileHeader = computed(() => isMobileDevice && isSidebarCollapsed.value)

// 生命周期
onMounted(() => {
  // 移动端默认收起侧边栏
  if (isMobileDevice) {
    isSidebarCollapsed.value = true
  }
})

// 方法
const handleToggleSidebar = () => {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const handleOpenSidebar = () => {
  isSidebarCollapsed.value = false
}

const handleExplore = () => {
  router.push('/searchRole')
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  height: 100vh;
  width: 100%;
  overflow: auto;
  background-color: #fff;
  font-size: 0.2rem;
  transition: all 0.3s ease;
}

.main-content {
  flex: 1;
  height: 100%;
  overflow: auto;
  transition: margin-left 0.3s ease;

  .router-view-content {
    // height: 100%;
    overflow-y: auto;
  }
}

.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.12rem 0.15rem;
  height: 0.6rem;

  .toggle-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 0.35rem;
    border-radius: 0.1rem;
    cursor: pointer;
    transition: background-color 0.2s;
    background-color: #fff;
    padding: 0.03rem 0.07rem;
    border: 0.01rem solid #ddd;

    &:hover {
      background-color: #f5f5f5;
    }
  }
}

// 移动端特定样式
.main-layout.mobile {
  .main-content {
    margin-left: 0;
  }

  &.sidebar-collapsed {
    .main-content {
      margin-left: 0;
    }
  }
}
</style>
