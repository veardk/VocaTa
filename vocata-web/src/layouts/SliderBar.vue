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
            :key="chat.conversationUuid"
            class="history-item"
            :class="{ active: activeChatId === chat.conversationUuid }"
            @click="selectChat(chat.conversationUuid)"
            :title="chat.title || '未命名对话'"
          >
            <div class="history-item-avatar"><img :src="chat.characterAvatarUrl" alt="" /></div>
            <!-- 对话标题，支持双击编辑 -->
            <span
              v-if="!sidebarCollapsed && editingChatId !== chat.conversationUuid"
              class="history-item-title"
              @dblclick="startEditTitle(chat.conversationUuid, chat.title || '未命名对话')"
            >
              {{ chat.title || '未命名对话' }}
            </span>

            <!-- 编辑状态的输入框 -->
            <el-input
              v-if="!sidebarCollapsed && editingChatId === chat.conversationUuid"
              v-model="editingTitle"
              class="history-item-edit-input"
              size="small"
              @blur="confirmEditTitle(chat.conversationUuid)"
              @keydown.enter="confirmEditTitle(chat.conversationUuid)"
              @keydown.esc="cancelEditTitle"
              @click.stop
              ref="editInput"
            />

            <!-- 操作按钮组 -->
            <div v-if="!sidebarCollapsed" class="history-item-actions" @click.stop>
              <el-icon
                @click="startEditTitle(chat.conversationUuid, chat.title || '未命名对话')"
                class="action-btn rename-btn"
                title="重命名"
              >
                <Edit />
              </el-icon>
              <el-popconfirm
                title="确定要删除该对话吗？"
                @confirm="deleteChat(chat.conversationUuid, $event)"
              >
                <template #reference>
                  <el-icon class="action-btn delete-btn" title="删除">
                    <Delete />
                  </el-icon>
                </template>
              </el-popconfirm>
            </div>
            <!--  <span class="history-time" v-if="!sidebarCollapsed">
              {{ formatTime(chat.lastTime) }}
            </span> -->
          </div>
        </div>
        <div class="empty-history" v-else-if="!sidebarCollapsed">
          <div v-if="isLoadingHistory">加载中...</div>
          <div v-else>暂无历史对话</div>
        </div>
      </div>
      <div class="user-section">
        <div class="user-box" @click.stop="toogleUserMenu">
          <div class="user-info">
            <div class="user-avatar">
              <img :src="userInfo.avatar" alt="" />
            </div>
            <div v-if="!sidebarCollapsed" class="user-name">{{ userInfo.nickname }}</div>
          </div>
          <div v-if="!sidebarCollapsed" class="more">
            <el-icon>
              <MoreFilled />
            </el-icon>
          </div>
        </div>
        <div class="user-menu" ref="userMenu" v-show="showUserMenu">
          <div class="user-menu-item" @click="userShow = true">
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
    <UserInfo v-if="userShow" @close="userShow = false" />
  </aside>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { conversationApi } from '@/api/modules/conversation'
import { isMobile } from '@/utils/isMobile'
import { removeToken } from '@/utils/token'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useTemplateRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// import type { ChatHistoryItem } from '@/types/common'
import { chatHistoryStore } from '@/store'
import UserInfo from './UserInfo.vue'
const emit = defineEmits(['toggleSidebar'])
const { sidebarCollapsed } = defineProps(['sidebarCollapsed'])
const router = useRouter()
const route = useRoute()
const fullscreenLoading = ref(false)
const isM = computed(() => isMobile())
const chatHistory = computed(() => {
  return chatHistoryStore().chatHistory
})
const isLoadingHistory = ref(false)
const activeChatId = ref('')
const userInfo = ref({
  nickname: '用户昵称',
  avatar: 'https://cdn.jsdelivr.net/gh/linhaishe/images/img/202307291610919.png',
})
const searchText = ref('')
const searchInput = ref()
const userMenu = useTemplateRef('userMenu')
const editInput = useTemplateRef('editInput')
const showUserMenu = ref(false)

// 编辑相关状态
const editingChatId = ref('')
const editingTitle = ref('')

const userShow = ref(false)

onMounted(() => {
  document.addEventListener('click', handleOutSide)
  getUserInfo()
  loadChatHistory()
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
const selectChat = (conversationUuid: string) => {
  console.log('selectChat', conversationUuid)
  activeChatId.value = conversationUuid
  router.push(`/chat/${conversationUuid}`)
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

// 加载聊天历史记录
const loadChatHistory = async () => {
  if (isLoadingHistory.value) return
  chatHistoryStore().getChatHistory()
  try {
    isLoadingHistory.value = true
  } catch (error) {
    console.error('加载聊天历史失败:', error)
    ElMessage.error('加载聊天历史失败')
  } finally {
    isLoadingHistory.value = false
  }
}

// 删除对话
const deleteChat = async (conversationUuid: string, event: Event) => {
  event.stopPropagation() // 阻止事件冒泡

  try {
    await chatHistoryStore().deleteChatHistory(conversationUuid)
  } catch (error) {
    console.error('删除对话失败:', error)
    ElMessage.error('删除对话失败')
  }
}

// 开始编辑标题
const startEditTitle = (conversationUuid: string, currentTitle: string) => {
  editingChatId.value = conversationUuid
  editingTitle.value = currentTitle
  nextTick(() => {
    if (editInput.value) {
      editInput.value.focus()
      editInput.value.select()
    }
  })
}

// 取消编辑
const cancelEditTitle = () => {
  editingChatId.value = ''
  editingTitle.value = ''
}

// 确认编辑标题
const confirmEditTitle = async (conversationUuid: string) => {
  if (!editingTitle.value.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }

  if (
    editingTitle.value.trim() ===
    chatHistory.value.find((chat) => chat.conversationUuid === conversationUuid)?.title
  ) {
    // 标题没有变化，直接取消编辑
    cancelEditTitle()
    return
  }

  try {
    const res = await conversationApi.updateConversationTitle(conversationUuid, {
      title: editingTitle.value.trim(),
    })

    if (res.code === 200) {
      // 更新本地数据
      const chatIndex = chatHistory.value.findIndex(
        (chat) => chat.conversationUuid === conversationUuid,
      )
      if (chatIndex !== -1) {
        chatHistory.value[chatIndex].title = editingTitle.value.trim()
      }
      ElMessage.success('标题更新成功')
    }
  } catch (error) {
    console.error('更新标题失败:', error)
    ElMessage.error('更新标题失败')
  } finally {
    cancelEditTitle()
  }
}
</script>

<style lang="scss" scoped>
.sidebar {
  width: 3.5rem;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  transition: all 0.3s ease;
  font-size: 0.26rem;
  overflow: hidden;
  border-right: 1px solid #e5e5e5;

  &.mobile {
    width: 100%;
    height: 100%;
    box-shadow: 2px 0 20px rgba(0, 0, 0, 0.1);
    &.collapsed {
      display: none;
    }
  }

  &.collapsed {
    width: 1.2rem;
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
      border-radius: 0.2rem;
      font-size: 0.28rem !important;
      margin: 0.05rem 0;
      padding: 0.18rem 0.24rem;
      transition: all 0.2s;
      cursor: pointer;
      color: #374151;
      background-color: transparent;

      &:hover {
        background-color: #f3f4f6;
        color: #111827;
      }

      .role-btn__icon {
        font-weight: 500;
        border-radius: 0.08rem;
        width: 0.36rem;
        height: 0.36rem;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .role-btn__text {
        margin-left: 0.16rem;
        font-weight: 500;
        font-size: 0.28rem !important;
      }
    }
  }

  // 搜索框
  .search-section {
    width: 90%;
    margin-bottom: 0.25rem;
    display: flex;
    justify-content: center;
    align-items: center;

    :deep(.el-input) {
      .el-input__wrapper {
        border-radius: 0.18rem;
        padding: 0.12rem 0.16rem;
        font-size: 0.24rem;
        box-shadow: none;
        border: 1px solid #e5e7eb;
        background-color: #f9fafb;
        transition: all 0.2s ease;

        &:hover {
          border-color: #d1d5db;
          background-color: #f3f4f6;
        }

        &.is-focus {
          border-color: #3b82f6;
          background-color: #fff;
          box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
        }

        .el-input__inner {
          font-size: 0.24rem;
          line-height: 1.5;
          color: #374151;

          &::placeholder {
            color: #9ca3af;
            font-size: 0.2rem;
          }
        }

        .el-input__suffix {
          .el-input__suffix-inner {
            .el-icon {
              color: #6b7280;
              font-size: 0.18rem;
            }
          }
        }
      }
    }

    .search-section__icon {
      cursor: pointer;
    }
  }

  // 历史对话列表
  .history-section {
    width: 95%;
    display: flex;
    flex-direction: column;
    padding: 0.15rem 0;
    border-top: 1px solid #e5e7eb;
    flex: 1;
    overflow: hidden;

    h3 {
      font-size: 0.28rem !important;
      text-align: left;
      margin: 0 0 0.15rem 0.15rem;
      color: #374151;
      font-weight: 600;
    }
    .history-list {
      width: 100%;
      overflow-y: auto;
      flex: 1;
      padding: 0 0.1rem;

      &::-webkit-scrollbar {
        width: 4px;
      }

      &::-webkit-scrollbar-track {
        background: transparent;
      }

      &::-webkit-scrollbar-thumb {
        background: #d1d5db;
        border-radius: 2px;
      }

      &::-webkit-scrollbar-thumb:hover {
        background: #9ca3af;
      }
    }

    .history-item {
      display: flex;
      align-items: center;
      padding: 0.16rem 0.15rem;
      border-radius: 0.16rem;
      margin-bottom: 0.06rem;
      cursor: pointer;
      transition: all 0.2s;
      color: #6b7280;
      position: relative;
      font-size: 0.26rem !important;

      &:hover {
        background-color: #f3f4f6;
        color: #374151;

        .history-item-actions {
          opacity: 1;
        }
      }

      &.active {
        background-color: #e5f3ff;
        color: #0066cc;
        font-weight: 500;
      }

      .history-item-actions {
        opacity: 0;
        transition: opacity 0.2s;
        display: flex;
        align-items: center;
        gap: 0.08rem;
        margin-left: auto;

        .action-btn {
          width: 0.28rem;
          height: 0.28rem;
          padding: 0.04rem;
          border-radius: 50%;
          transition: all 0.2s;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 0.16rem;
          color: #6b7280;

          &:hover {
            background-color: #e5e7eb;
            color: #374151;
          }

          &.rename-btn:hover {
            background-color: #dbeafe;
            color: #2563eb;
          }

          &.delete-btn:hover {
            background-color: #fee2e2;
            color: #dc2626;
          }
        }
      }
      .history-item-avatar {
        width: 0.36rem;
        height: 0.36rem;
        border-radius: 50%;
        overflow: hidden;
        flex-shrink: 0;
        margin-right: 0.06rem;
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }

      .history-item-title {
        flex: 1;
        margin: 0 0.08rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        cursor: text;
        font-size: 0.24rem !important;
        line-height: 1.4;
        font-family: "SF Pro Display", "SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
        font-weight: 600;
        letter-spacing: 0.02em;
        color: inherit;
        text-rendering: optimizeLegibility;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
      }
      .history-item-edit-input {
        flex: 1;
        margin: 0 0.1rem;

        :deep(.el-input__wrapper) {
          padding: 0 0.08rem;
          box-shadow: 0 0 0 1px #409eff inset;
          background-color: #fff;
        }
      }

      .history-item-actions {
        opacity: 0;
        transition: opacity 0.2s;
        display: flex;
        align-items: center;
        gap: 0.05rem;
        margin-left: auto;

        .action-btn {
          width: 0.24rem;
          height: 0.24rem;
          padding: 0.02rem;
          border-radius: 50%;
          transition: all 0.2s;
          cursor: pointer;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 0.14rem;

          &:hover {
            transform: scale(1.1);
          }
        }

        .rename-btn {
          &:hover {
            background-color: #409eff;
            color: #fff;
          }
        }

        .delete-btn {
          &:hover {
            background-color: #ff4d4f;
            color: #fff;
          }
        }
      }
    }

    .empty-history {
      color: #6b7280;
      text-align: center;
      padding: 0.2rem;
      font-size: 0.22rem !important;
    }
  }

  // 用户信息
  .user-section {
    width: 90%;
    position: relative;
    .user-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0.12rem 0.15rem;
      height: auto;
      min-height: 0.5rem;
      background: #f9fafb;
      cursor: pointer;
      border-radius: 0.16rem;
      color: #374151;
      border: 1px solid #e5e7eb;
      transition: all 0.2s ease;

      &:hover {
        background: #f3f4f6;
        border-color: #d1d5db;
      }
    }
    .user-info {
      display: flex;
      align-items: center;
      justify-content: center;
      .user-avatar {
        width: 0.32rem;
        height: 0.32rem;
        border-radius: 50%;
        overflow: hidden;
        background-color: #e5e7eb;
        flex-shrink: 0;
        img {
          width: 100%;
          height: 100%;
          object-fit: cover;
        }
      }
      .user-name {
        margin-left: 0.06rem;
        font-size: 0.22rem;
        line-height: 1.3;
        color: #374151;
        font-family: "SF Pro Display", "SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;
        font-weight: 600;
        letter-spacing: 0.02em;
        text-rendering: optimizeLegibility;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
      }
    }
    .more {
      font-size: 0.18rem;
      color: #6b7280;
      transition: color 0.2s ease;

      &:hover {
        color: #374151;
      }
    }
    .user-menu {
      position: absolute;
      left: 0;
      right: 0;
      bottom: 110%;
      background-color: #fff;
      color: #374151;
      border: 1px solid #e5e7eb;
      border-radius: 0.16rem;
      display: flex;
      flex-direction: column;
      align-items: center;
      box-shadow: 0 0.1rem 0.3rem rgba(0, 0, 0, 0.15), 0 0.04rem 0.08rem rgba(0, 0, 0, 0.1);
      .user-menu-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0.14rem 0.2rem;
        width: 100%;
        height: auto;
        min-height: 0.48rem;
        font-size: 0.2rem;
        cursor: pointer;
        transition: all 0.2s;
        border-radius: 0.12rem;
        margin: 0.02rem 0;

        &:hover {
          background-color: #f3f4f6;
        }

        &:first-child {
          margin-top: 0.08rem;
        }

        &:last-child {
          margin-bottom: 0.08rem;
        }
      }
    }
  }
}
</style>
