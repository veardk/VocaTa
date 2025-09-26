<template>
  <aside class="sidebar" :class="[isM ? 'mobile' : 'pc', { collapsed: sidebarCollapsed }]">
    <div class="sidebar-header">
      <h2 v-if="!sidebarCollapsed" class="sidebar-title">
        语Ta
        <img src="@/assets/images/logo-text.png" alt="" />
      </h2>
      <div
        class="toggle-btn"
        @click="toggleSidebar"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
      >
        <el-icon>
          <Fold v-if="!sidebarCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>
    </div>

    <div class="sidebar-content">
      <!-- 角色管理 -->
      <div class="role-actions">
        <div
          class="role-btn"
          @click="createNewRole"
          aria-label="新建角色"
          :class="route.meta.title === '新建角色' ? 'active' : ''"
        >
          <div class="role-btn__icon">
            <el-icon><CirclePlusFilled /></el-icon>
          </div>
          <span v-if="!sidebarCollapsed" class="role-btn__text">新建角色</span>
        </div>
        <div
          class="role-btn"
          @click="showRoleGallery"
          aria-label="选择角色"
          :class="route.meta.title === '探索' ? 'active' : ''"
        >
          <div class="role-btn__icon">
            <el-icon><UserFilled /></el-icon>
          </div>
          <span v-if="!sidebarCollapsed" class="role-btn__text">探索</span>
        </div>
      </div>
      <!-- 搜索框 -->
      <div class="search-section" aria-label="搜索">
        <el-input
          v-model="searchText"
          placeholder="搜索对话"
          suffix-icon="Search"
          v-if="!sidebarCollapsed"
          ref="searchInput"
          style="border-radius: 0.1rem"
        ></el-input>
        <el-icon v-else class="search-section__icon" @click="searchIconHandler"><Search /></el-icon>
      </div>
      <!-- 历史对话列表 -->
      <div class="history-section">
        <h3 v-if="!sidebarCollapsed" class="history-title">历史对话</h3>
        <div class="history-list" v-if="chatHistory.length > 0">
          <div
            v-for="chat in chatHistory"
            :key="chat.id"
            class="history-item"
            :class="{ active: activeChatId === chat.id }"
            @click="selectChat(chat.id)"
            :title="chat.title || '未命名对话'"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span v-if="!sidebarCollapsed" class="history-item-title">
              {{ chat.title || '未命名对话' }}
            </span>
            <!--  <span class="history-time" v-if="!sidebarCollapsed">
              {{ formatTime(chat.lastTime) }}
            </span> -->
          </div>
        </div>
        <div class="empty-history" v-else-if="!sidebarCollapsed">暂无历史对话</div>
      </div>
      <div class="user-section">
        <div class="user-box" @click.stop="toogleUserMenu">
          <div class="user-info">
            <div class="user-avatar">
              <img :src="userInfo.avatar" alt="" />
            </div>
            <div class="user-name">{{ userInfo.nickname }}</div>
          </div>
          <div class="more">
            <el-icon>
              <MoreFilled />
            </el-icon>
          </div>
        </div>
        <div class="user-menu" ref="userMenu" v-show="showUserMenu">
          <div class="user-menu-item">
            设置<el-icon><Setting /></el-icon>
          </div>
          <div
            class="user-menu-item"
            @click="logOut"
            v-loading.fullscreen.lock="fullscreenLoading"
            element-loading-text="退出中..."
          >
            退出<el-icon><SwitchButton /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { isMobile } from '@/utils/isMobile'
import { removeToken } from '@/utils/token'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const emit = defineEmits(['toggleSidebar'])
const { sidebarCollapsed } = defineProps(['sidebarCollapsed'])
const router = useRouter()
const route = useRoute()
const fullscreenLoading = ref(false)
const isM = computed(() => isMobile())
const chatHistory = ref<ChatHistoryItem[]>([
  {
    id: '1',
    title: '对话1',
    lastTime: new Date(),
  },
  {
    id: '2',
    title: '对话2',
    lastTime: new Date(),
  },
])
const activeChatId = ref('')
const userInfo = ref({
  nickname: '用户昵称',
  avatar: 'https://cdn.jsdelivr.net/gh/linhaishe/images/img/202307291610919.png',
})
const searchText = ref('')
const searchInput = ref()
const userMenu = useTemplateRef('userMenu')
const showUserMenu = ref(false)

// 定义聊天记录类型
interface ChatHistoryItem {
  id: string
  title?: string
  lastTime?: Date | string
  // 可以添加更多属性，如消息数量、未读数量等
}
onMounted(() => {
  document.addEventListener('click', handleOutSide)
  getUserInfo()
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleOutSide)
})
const toggleSidebar = () => {
  emit('toggleSidebar')
}
const showRoleGallery = () => {
  console.log('showRoleGallery')
  router.push('/searchRole')
}
const createNewRole = () => {
  console.log('createNewRole')
  router.push('/newRole')
}
const selectChat = (id: string) => {
  console.log('selectChat', id)
  router.push(`/chat/${id}`)
}
const searchIconHandler = () => {
  emit('toggleSidebar')
  nextTick(() => {
    searchInput.value.focus()
  })
}
const toogleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
}
const handleOutSide = (e: MouseEvent) => {
  if (userMenu.value && !userMenu.value.contains(e.target as Node) && showUserMenu.value) {
    toogleUserMenu()
  }
}
const logOut = async () => {
  try {
    fullscreenLoading.value = true
    const res = await userApi.logout()
    if (res.code === 200) {
      removeToken()
      ElMessage.success('退出成功')
      router.push('/login')
    }
  } catch (error) {
    console.log(error)
    ElMessage.error('退出失败')
  } finally {
    fullscreenLoading.value = false
  }
}
const getUserInfo = async () => {
  try {
    const res = await userApi.getUserInfo()
    if (res.code === 200) {
      userInfo.value = res.data
    }
  } catch (error) {
    console.log(error)
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 2.5rem;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s ease;
  font-size: 0.16rem;
  overflow: hidden;
  box-shadow: 0.02rem 0 0.1rem rgba(0, 0, 0, 0.07);

  &.mobile {
    width: 100%;
    height: 100%;
    box-shadow: 2px 0 20px rgba(0, 0, 0, 0.1);
    &.collapsed {
      display: none;
    }
  }

  &.collapsed {
    width: 0.8rem;
    .sidebar-header {
      justify-content: center;
    }
    .role-actions .role-btn {
      padding-left: 0;
      padding-right: 0;
      justify-content: center;
    }
  }
}

.sidebar-header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.15rem;
  font-size: 0.25rem;
  border-bottom: 0.01rem solid #f0f0f0;
  .sidebar-title {
    margin: 0;
    font-size: 0;
    font-weight: 600;
    color: #333;
    img {
      width: 0.9rem;
      height: 0.4rem;
    }
  }
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

.sidebar-content {
  margin: 0.2rem 0;
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  overflow-y: auto;
  // 角色管理
  .role-actions {
    width: 90%;
    display: flex;
    flex-direction: column;
    margin-bottom: 0.2rem;

    .role-btn {
      display: flex;
      align-items: center;
      border-radius: 0.1rem;
      font-size: 0.2rem;
      margin: 0.03rem 0 0;
      padding: 0.1rem 0.2rem;
      transition: all 0.2s;
      cursor: pointer;
      color: #333;

      &:hover {
        background-color: #f7f7f7;
        // color: #fff;
      }
      .role-btn__icon {
        // background: #000;
        // color: #000;
        font-weight: bold;
        border-radius: 0.05rem;
        width: 0.3rem;
        height: 0.3rem;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .role-btn__text {
        margin-left: 0.15rem;
      }
    }
    .active {
      background-color: #eee;
      // color: #fff;
      font-weight: 500;
    }
  }

  // 搜索框
  .search-section {
    width: 85%;
    margin-bottom: 0.3rem;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 0.2rem;

    .search-section__icon {
      cursor: pointer;
    }
  }

  // 历史对话列表
  .history-section {
    width: 90%;
    display: flex;
    flex-direction: column;
    padding: 0.2rem 0;
    border-top: 0.01rem solid #f0f0f0;
    flex: 1;
    overflow: auto;
    font-size: 0.16rem;

    h3 {
      font-size: 0.2rem;
      text-align: center;
      margin: 0 0 0.1rem 0;
      color: #333;
      font-weight: 500;
    }
    .history-list {
      width: 100%;
      overflow-y: auto;
      flex: 1;
    }

    .history-item {
      display: flex;
      align-items: center;
      padding: 0.1rem 0.15rem;
      border-radius: 0.08rem;
      margin-bottom: 0.05rem;
      cursor: pointer;
      transition: all 0.2s;
      color: #666;
      position: relative;

      &:hover {
        background-color: #f7f7f7;
      }

      &.active {
        background-color: #eaeaea;
        // color: #1890ff;
        font-weight: bold;
      }

      .history-item-title {
        flex: 1;
        margin: 0 0.1rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .history-time {
        font-size: 0.12rem;
        color: #999;
        white-space: nowrap;
      }
    }

    .empty-history {
      color: #999;
      text-align: center;
      padding: 0.2rem;
      font-size: 0.14rem;
    }
  }
  .user-section {
    width: 90%;
    position: relative;
    .user-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 0.2rem;
      height: 0.4rem;
      background: linear-gradient(to top, #fff, #fff3);
      cursor: pointer;
      border-radius: 0.1rem;
      color: #000;
      &:hover {
        background: linear-gradient(to top, #eee9, #eee3);
      }
    }
    .user-info {
      display: flex;
      align-items: center;
      justify-content: center;
      .user-avatar {
        width: 0.25rem;
        height: 0.25rem;
        border-radius: 50%;
        overflow: hidden;
        background-color: #ccc;
        img {
          width: 100%;
          height: 100%;
          object-fit: contain;
        }
      }
      .user-name {
        margin-left: 0.15rem;
        font-size: 0.2rem;
        line-height: 0.16rem;
      }
    }
    .more {
      font-size: 0.16rem;
      text-align: center;
    }
    .user-menu {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 110%;
      background-color: #fffd;
      color: #000;
      border: 0.03rem solid #f5f5f5;
      border-radius: 0.1rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      .user-menu-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.1rem 0.2rem;
        width: 100%;
        height: 0.4rem;
        font-size: 0.16rem;
        cursor: pointer;
        transition: all 0.2s;
        &:hover {
          background-color: #f5f5f5;
        }
      }
    }
  }
}
</style>
