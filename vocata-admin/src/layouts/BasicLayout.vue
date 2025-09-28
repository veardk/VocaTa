<template>
  <div class="flex h-screen">
    <!-- 菜单栏 -->
    <div
      class="transition-all duration-300 bg-slate-500"
      :class="`${isCollapse ? 'w-1/24' : 'w-3/24'}`"
    >
      <div
        class="h-2/24 bg-blue-200 text-3xl font-bold flex items-center justify-center gap-3 text-white"
      >
        <img class="h-10" src="@/assets/images/logo-text.png" alt="" v-show="!isCollapse" />
        <img class="h-10" src="@/assets/images/logo.png" alt="" v-show="isCollapse" />
      </div>
      <div class="h-22/24 py-13">
        <el-menu
          :default-active="activePath"
          id="menu"
          :collapse="isCollapse"
          :collapse-transition="false"
        >
          <Menu :menuList="userStore.menuRoutes" :isCollapse="isCollapse" />
        </el-menu>
      </div>
    </div>
    <!-- 主界面 -->
    <div class="bg-amber-300" :class="`${isCollapse ? 'w-23/24' : 'w-21/24'}`">
      <div class="h-2/24 bg-white pl-5 pr-10">
        <Tabbar v-model="isCollapse" @refresh="refresh" />
      </div>
      <div class="h-22/24 bg-gray-100 overflow-auto">
        <Breadcrum />
        <router-view v-slot="{ Component }">
          <transition name="fade">
            <component :is="Component" v-if="flag" />
          </transition>
        </router-view>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import Menu from './MenuCom.vue'
import Tabbar from './TabBar.vue'
import Breadcrum from './BreadCrum.vue'
import { user } from '@/store' // 使用Pinia store
import { useRoute, useRouter } from 'vue-router'
const flag = ref(true)
const router = useRouter()
// 获取用户store
const userStore = user()
const route = useRoute()

// 侧边栏折叠状态
const isCollapse = ref(false)
const activePath = computed(() => {
  return route.path
})
onMounted(() => {
  if (localStorage.userId == null) {
    router.push('/passport/login')
  }
})
const refresh = () => {
  flag.value = false
  nextTick(() => {
    flag.value = true
  })
}
</script>

<style scoped>
.fade-enter-from {
  opacity: 0;
}

.fade-enter-active {
  transition: all 0.5s;
}

.fade-enter-to {
  opacity: 1;
}

#menu {
  --el-menu-bg-color: var(--color-slate-600);
  --el-menu-text-color: var(--color-white);
  --el-menu-active-color: var(--color-orange-300);
  --el-menu-hover-bg-color: var(--color-gray-400);
  --el-menu-item-font-size: 1.3rem;
  --el-menu-level-padding: 2rem;
  transition: all 0.5s;
}

.el-menu--collapse {
  width: auto;
}

::v-deep #menu .el-sub-menu .el-menu-item {
  font-size: 1rem;
}
</style>
