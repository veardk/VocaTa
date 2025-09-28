<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">
      <div class="title">探索</div>
      <div class="header-actions">
        <div class="search">
          <el-icon class="search-icon">
            <Search />
          </el-icon>
          <input
            type="text"
            v-model="searchInput"
            placeholder="搜索角色"
            @input="search"
            @keyup.enter="search"
          />
        </div>
        <div class="notice-btn" @click="showNotice">
          <el-icon>
            <Bell />
          </el-icon>
        </div>
      </div>
    </div>

    <!--banner角色列表-->
    <div class="banner" v-loading="loading1" element-loading-text="Loading..." v-if="seachStatus">
      <div
        v-for="(item, index) in selectRoleList"
        :key="item.id"
        class="banner-item"
        :style="[getCardStyle(index), { backgroundImage: `url(${item.avatarUrl})` }]"
        @mouseenter="cardFace[index] = 1"
        @mouseleave="cardFace[index] = 0"
      >
        <!-- 前 -->
        <div
          class="card-face card-front"
          vShow="cardFace=='card-front'"
          v-show="cardFace[index] == 0"
        >
          <div class="card-info">
            <div class="hot">
              <el-icon class="icon"><ChatDotRound /></el-icon>
              {{ item.chatCount }}
            </div>
            <div class="name">{{ item.name }}</div>
            <div class="greeting">{{ item.greeting }}</div>
          </div>
        </div>
        <!-- 后 -->
        <div
          class="card-face card-back"
          vShow="!cardFace=='card-front'"
          v-show="cardFace[index] == 1"
        >
          <div class="card-info">
            <div class="top">
              <div class="name">{{ item.name }}</div>
            </div>
            <div class="description">{{ item.description }}</div>
            <div class="more">
              <div class="info" @click="openRoleDialog(item)">详情 ></div>
            </div>
            <div class="goto" @click.stop="startConversation(item.id)">开始对话</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 角色列表配置 -->
    <div class="role-list-config">
      <h3>角色列表</h3>
      <div v-if="!searchInput">
        <span :class="currentView === 'my' ? 'active-config' : ''" @click="setViewMode('my')">我的</span>
        <span
          :class="currentView === 'public' && searchParam.orderDirection == 'desc' ? 'active-config' : ''"
          @click="setViewMode('hot')"
          >热门</span
        >
        <span
          :class="currentView === 'public' && searchParam.orderDirection == 'asc' ? 'active-config' : ''"
          @click="setViewMode('latest')"
          >最新</span
        >
      </div>
    </div>
    <!--角色列表-->
    <div class="role-list" v-loading="loading2" element-loading-text="Loading...">
      <div
        v-for="item in roleList"
        :key="item.id"
        class="role-list-item"
        :style="{ backgroundImage: `url(${item.avatarUrl})` }"
      >
        <!-- 前 -->
        <div class="card-face card-front">
          <div class="card-info">
            <div class="hot">
              <el-icon class="icon"><ChatDotRound /></el-icon>
              {{ item.chatCount }}
            </div>
            <div class="name">{{ item.name }}</div>
            <div class="greeting">{{ item.greeting }}</div>
          </div>
        </div>
        <!-- 后 -->
        <div class="card-face card-back">
          <div class="card-info">
            <div class="top">
              <div class="name">{{ item.name }}</div>
            </div>
            <div class="description">{{ item.description }}</div>
            <div class="more">
              <div class="info" @click="openRoleDialog(item)">详情 ></div>
            </div>
            <div class="goto" @click.stop="startConversation(item.id)">开始对话</div>
          </div>
        </div>
      </div>
    </div>
    <div class="pagination-container">
      <el-pagination
        :size="isM ? 'small' : 'default'"
        background
        layout="prev, pager, next, total"
        :total="total"
        :page-size="searchParam.pageSize"
        :current-page="searchParam.pageNum"
        @current-change="handlePageChange"
        hide-on-single-page
      />
    </div>

    <!-- 通知卡片 -->
    <div v-if="showNoticeCard" class="notice-overlay" @click="closeNotice">
      <div class="notice-card" @click.stop>
        <div class="notice-close" @click="closeNotice">
          <el-icon><Close /></el-icon>
        </div>

        <div class="notice-header">
          <h2>VocaTa平台更新</h2>
          <p class="notice-date">2024年9月28日</p>
        </div>

        <div class="notice-content">
          <h3>📢 角色对话功能上线</h3>
          <p class="body-text">
            <strong>新功能已可用！</strong><br><br>
            VocaTa现在支持角色对话功能。您可以创建角色、进行对话交流，体验更丰富的互动方式。欢迎试用新功能并给我们反馈。
          </p>
        </div>
      </div>
    </div>

    <RoleDialog :item="roleSelected" v-if="infoShow" @close="infoShow = false" />
  </div>
</template>

<script setup lang="ts">
import { roleApi } from '@/api/modules/role'
import type { PublicRoleQuery } from '@/types/api'
import type { roleInfo } from '@/types/common'
import debounce from '@/types/debounce'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch, watchEffect, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import { chatHistoryStore } from '@/store'
import RoleDialog from './components/RoleDialog.vue'
import { Search, ChatDotRound, Bell, Close } from '@element-plus/icons-vue'
const isM = computed(() => isMobile())
const router = useRouter()

const roleSelected = ref<roleInfo>()
const searchInput = ref('')
const roleList: Ref<roleInfo[]> = ref([])
const selectRoleList: Ref<roleInfo[]> = ref([])
const myRoleList: Ref<roleInfo[]> = ref([])
const cardFace = ref([0, 0, 0, 0, 0])
const searchParam: Ref<PublicRoleQuery> = ref({
  pageNum: 1,
  pageSize: 15,
  orderDirection: 'desc',
})
const total = ref(0)
const currentView = ref('public') // 默认显示公开角色（热门）

const seachStatus = ref(true)
const infoShow = ref(false)
const showNoticeCard = ref(false)
const loading1 = ref(false)
const loading2 = ref(false)
// 这里可以放全局逻辑
watch(
  [searchParam, currentView],
  () => {
    if (currentView.value === 'public') {
      getRoleList()
    } else if (currentView.value === 'my') {
      getMyRoleList()
    }
  },
  {
    deep: true,
  },
)
// 计算卡片样式的方法
const getCardStyle = (index: number) => {
  // if (isM.value) return
  let radius = 1000 // 卡片距离中心的距离
  let angleStep = 45 / (5 - 1 || 1)
  let startAngle = -45 / 2
  if (isM.value) {
    radius = 150 // 卡片距离中心的距离
    angleStep = 90 / (5 - 1 || 1)
    startAngle = -90 / 2
  }
  const angle = startAngle + index * angleStep

  // 将角度转换为弧度
  const radian = (angle * Math.PI) / 180
  // 计算卡片位置
  const x = radius * Math.sin(radian)
  const y = isM.value ? -Math.abs(radian * 2) : Math.abs(radian * 100) / 1.5
  const transformY = isM.value ? '-50%' : '-40%'

  return {
    left: `calc(50% + ${x / 100}rem)`,
    top: `calc(50% - ${y / 50}rem)`,
    transform: `translate(-50%, ${transformY}) rotate(${angle}deg)`,
  }
}

// 获取角色列表
const getRoleList = async () => {
  loading2.value = true
  const res = await roleApi.getPublicRoleList(searchParam.value)
  roleList.value = res.data.list
  console.log(roleList.value)
  loading2.value = false
  total.value = res.data.total
}
// 获取精选角色列表
const getSelectedRoleList = async () => {
  loading1.value = true
  const res = await roleApi.getChoiceRoleList({ limit: 5 })
  selectRoleList.value = res.data
  console.log('精选角色列表:', selectRoleList.value)
  loading1.value = false
}

// 获取我的角色列表
const getMyRoleList = async () => {
  loading2.value = true
  try {
    const res = await roleApi.getMyRoleList()
    roleList.value = res.data.list || res.data
    console.log('我的角色列表:', roleList.value)
    total.value = roleList.value.length
  } catch (error) {
    console.error('获取我的角色列表失败:', error)
    ElMessage.error('获取我的角色列表失败')
    roleList.value = []
    total.value = 0
  } finally {
    loading2.value = false
  }
}

// 设置视图模式
const setViewMode = (mode: string) => {
  if (mode === 'my') {
    currentView.value = 'my'
  } else {
    currentView.value = 'public'
    if (mode === 'hot') {
      searchParam.value.orderDirection = 'desc'
    } else if (mode === 'latest') {
      searchParam.value.orderDirection = 'asc'
    }
  }
}
const debouncedSearch = debounce(async () => {
  console.log('搜索:', searchInput.value)
  if (searchInput.value != '') {
    currentView.value = 'public'
    seachStatus.value = false
    const res = await roleApi.searchRole({ keyword: searchInput.value })
    console.log(searchInput.value)
    roleList.value = res.data.list
  } else {
    seachStatus.value = true
    if (currentView.value === 'public') {
      getRoleList()
    } else if (currentView.value === 'my') {
      getMyRoleList()
    }
  }
}, 500)
const search = () => {
  debouncedSearch()
}

// 打开角色详情页
const openRoleDialog = (item: roleInfo) => {
  roleSelected.value = item
  infoShow.value = true
}
onMounted(() => {
  Promise.all([getRoleList(), getSelectedRoleList()]).then(() => {})
})

// 开始对话
const startConversation = async (characterId: string | number) => {
  try {
    console.log('点击开始对话，角色ID:', characterId)
    console.log('角色ID类型:', typeof characterId)

    if (!characterId) {
      console.error('角色ID为空')
      ElMessage.error('角色信息有误，请重试')
      return
    }

    // 添加加载状态
    const loadingMessage = ElMessage.info('正在创建对话...')

    // 调用创建对话接口，确保ID转换为字符串
    const conversationUuid = await chatHistoryStore().addChatHistory(characterId)

    loadingMessage.close()

    ElMessage.success('对话创建成功！')

    // 跳转到聊天页面
    router.push(`/chat/${conversationUuid}`)
  } catch (error) {
    console.error('创建对话失败:', error)
    ElMessage.error('创建对话失败，请稍后重试')
  }
}

// 显示公告
const showNotice = () => {
  showNoticeCard.value = true
}

// 关闭公告
const closeNotice = () => {
  showNoticeCard.value = false
}

// 处理分页变化
const handlePageChange = (page: number) => {
  if (page < 1 || (total.value > 0 && page > Math.ceil(total.value / searchParam.value.pageSize))) {
    return
  }
  searchParam.value.pageNum = page
}
</script>

<style lang="scss" scoped>
.main-container {
  padding: 0.3rem 0.5rem;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  &.mobile {
    padding: 0;
    .header {
      padding: 0 0.2rem 0;

      .title {
        font-size: 0.2rem;
      }

      .header-actions {
        gap: 0.15rem;
      }

      .search {
        width: 55%;
        max-width: 3rem;
        min-width: 2rem;
        height: 0.4rem;
        border: 1px solid #e1e5e9;
        border-radius: 0.15rem;
        background-color: #fafbfc;

        .search-icon {
          font-size: 0.2rem;
          margin: 0 0.08rem 0 0.12rem;
        }

        input {
          font-size: 0.2rem;
          padding: 0 0.12rem 0 0;

          &::placeholder {
            font-size: 0.18rem;
          }
        }
      }

      .notice-btn {
        width: 0.4rem;
        height: 0.4rem;
        border-radius: 0.15rem;

        .el-icon {
          font-size: 0.2rem;
        }
      }
    }
    // .banner角色列表
    .banner {
      margin: 0.1rem 0;
      height: 2.8rem;
      overflow: hidden;
      .banner-item {
        width: 1.5rem;
        height: 2rem;
        font-size: 0.2rem;
      }
    }
    // 角色列表
    .role-list-config {
      margin-top: 0.1rem;
      h3 {
        font-size: 0.2rem;
      }
    }
    .role-list {
      padding-left: 0.2rem;
      padding-right: 0.2rem;
      // margin: 0.1rem 0;
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.1rem;
      .role-list-item {
        width: 100%;
        height: 2rem;
        font-size: 0.2rem;
      }
    }
    .name {
      font-size: 0.2rem;
    }
    .bottom {
      padding: 0 0.1rem;
    }
    .card-back {
      .description {
        font-size: 0.16rem;
      }
      .avatar {
        width: 0.35rem;
        height: 0.35rem;
      }
      .name {
        font-size: 0.18rem;
      }
      .info {
        font-size: 0.15rem;
      }
      .more {
        margin: 0.05rem;
      }
      .goto {
        margin: 0rem;
      }
    }
    .card-info {
      padding: 0.1rem;
    }
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .title {
    font-size: 0.3rem;
    font-weight: bold;
  }

  .header-actions {
    display: flex;
    align-items: center;
    gap: 0.25rem;
  }

  .search {
    width: 35%;
    max-width: 4.5rem;
    min-width: 3rem;
    height: 0.52rem;
    display: flex;
    align-items: center;
    border: 1px solid #e1e5e9;
    border-radius: 0.2rem;
    overflow: hidden;
    background-color: #fafbfc;
    transition: all 0.2s ease;
    font-family: "SF Pro Display", "SF Pro Text", -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "PingFang SC", "Hiragino Sans GB", "Noto Sans CJK SC", "Source Han Sans SC", "Microsoft YaHei UI", "Microsoft YaHei", sans-serif;

    &:hover {
      border-color: #d1d5db;
      background-color: #f9fafb;
    }

    &:focus-within {
      border-color: #9ca3af;
      background-color: #fff;
      box-shadow: 0 0 0 1px rgba(156, 163, 175, 0.1);
    }

    .search-icon {
      font-size: 0.24rem;
      color: #9ca3af;
      margin: 0 0.12rem 0 0.16rem;
      flex-shrink: 0;
    }

    input {
      flex: 1;
      height: 100%;
      border: none;
      padding: 0 0.16rem 0 0;
      outline: none;
      font-size: 0.22rem;
      color: #1f2937;
      background-color: transparent;
      font-family: inherit;

      &::placeholder {
        font-size: 0.2rem;
        color: #9ca3af;
        font-weight: 400;
      }
    }
  }

  .notice-btn {
    width: 0.52rem;
    height: 0.52rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #e1e5e9;
    border-radius: 0.2rem;
    background-color: #fafbfc;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover {
      border-color: #d1d5db;
      background-color: #f9fafb;
    }

    &:active {
      background-color: #e5e7eb;
    }

    .el-icon {
      font-size: 0.24rem;
      color: #6b7280;
    }
  }
}

// banner角色列表
.banner {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  margin-top: 0.2rem;
  position: relative;
  height: 4rem;
  width: 100%;
  .banner-item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    width: 2.75rem;
    height: 3.3rem;
    background-color: #eee;
    border-radius: 0.2rem;
    backdrop-filter: blur(0.1rem);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 0.1rem 0.3rem rgba(0, 0, 0, 0.2);
    // padding: 0 0.2rem;
    transition: all 0.3s ease;
    position: absolute;
    overflow: hidden;
    cursor: pointer;
    &:hover {
      background: rgba(255, 255, 255, 0.3);
      z-index: 5;
      transform: translate(-50%, -50%) scale(1.2) !important;
    }
  }
  &:last-child {
    margin-right: 0;
  }
}
.role-list-config {
  display: flex;
  width: 100%;
  justify-content: space-between;
  padding: 0.2rem;
  margin-top: 0.2rem;
  align-items: center;
  border-top: 1px solid #ccc;
  .active-config {
    font-weight: bold;
  }
  span {
    margin-right: 0.1rem;
    cursor: pointer;
  }
  h3 {
    font-size: 0.25rem;
  }
}
// 角色列表
.role-list {
  width: 100%;
  display: grid;
  // margin-top: 0.2rem;
  grid-template-columns: repeat(5, 1fr); /* 一行五个 */
  gap: 0.25rem; /* 间距0.1rem */
  .role-list-item {
    width: 2.75rem;
    height: 3.3rem;
    font-size: 0.3rem;
    background-size: cover;
    background-position: center;
    background-repeat: no-repeat;
    transform-style: preserve-3d;
    transition: transform 0.8s;
    border: 1px solid #ccc;
    cursor: pointer;
    border-radius: 0.1rem;
    position: relative;
    // backface-visibility: hidden;

    &:hover {
      transform: rotateY(180deg);
    }
  }
  .card-face {
    backface-visibility: hidden;
    overflow: hidden;
  }
  .card-back {
    transform: rotateY(180deg);
  }
  &:last-child {
    margin-right: 0;
  }
}
.card-face {
  display: flex;
  flex-direction: column;
  align-self: start;
  height: 100%;
  position: absolute;
  width: 100%;
  top: 0;
}

.card-front {
  width: 100%;
  // background: linear-gradient(45deg, #ff9a9e 0%, #fad0c4 100%);
  color: #333;
  justify-content: end;
  .name {
    width: 80%;
    font-size: 0.2rem;
    font-weight: bold;
    // text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin: 0.05rem 0;
  }
  .hot {
    display: flex;
    align-items: center;
    font-size: 0.16rem;
    .icon {
      margin-right: 0.05rem;
    }
  }
  .greeting {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 0.16rem;
  }
}
.card-info {
  background-color: #0003;
  color: #fff;
  padding: 0.15rem;
  display: flex;
  flex-direction: column;
}
.card-back {
  // background: linear-gradient(45deg, #a1c4fd 0%, #c2e9fb 100%);
  color: #fff;
  .card-info {
    height: 100%;
  }
  .top {
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0.03rem auto;
    width: 99%;
    .avatar {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      overflow: hidden;
      img {
        width: 100%;
        height: 100%;
      }
    }
    .name {
      margin-left: 0.05rem;
      font-size: 0.25rem;
      font-weight: bold;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .description {
    font-size: 0.2rem;
    text-align: center;
    overflow-y: auto;
    padding: 0.05rem;
    flex: 1;
  }
  .more {
    display: flex;
    justify-content: space-between;
    margin: 0.1rem;
    .info {
      padding-bottom: 0.02rem;
      &:hover {
        border-bottom: 0.01rem solid #333;
      }
    }
  }
  .goto {
    font-size: 0.2rem;
    text-align: center;
    margin: 0.05rem 0.1rem;
    background-color: #000;
    border: 1px solid #aaa;
    border-radius: 0.1rem;
    padding: 0.05rem 0.1rem;
    color: #fff;
    cursor: pointer;
  }
}

// 分页容器样式
.pagination-container {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0.5rem 1rem;
  margin-top: 0.3rem;
}

// 分页组件样式优化 - 黑白灰主题
.pagination-container :deep(.el-pagination) {
  --el-color-primary: #333333;
  --el-color-primary-light-3: #f8f8f8;
  --el-color-primary-light-5: #cccccc;
  --el-color-primary-light-7: #e5e5e5;
  --el-color-primary-light-8: #f0f0f0;
  --el-color-primary-light-9: #f8f8f8;
  --el-text-color-primary: #333333;
  --el-text-color-regular: #666666;
  --el-text-color-secondary: #999999;
  --el-text-color-placeholder: #cccccc;
  --el-border-color: #e5e5e5;
  --el-border-color-light: #f0f0f0;
  --el-border-color-lighter: #f8f8f8;
  --el-fill-color-blank: #ffffff;
  --el-fill-color-light: #f8f8f8;

  .el-pager {
    li {
      min-width: 32px !important;
      height: 32px !important;
      line-height: 30px !important;
      border: 1px solid #e5e5e5 !important;
      border-radius: 6px !important;
      margin: 0 4px !important;
      font-size: 14px !important;
      font-weight: 400 !important;
      color: #666666 !important;
      background-color: #ffffff !important;
      transition: all 0.2s ease !important;

      &:hover {
        color: #333333 !important;
        border-color: #cccccc !important;
        background-color: #f8f8f8 !important;
      }

      &.is-active {
        color: #ffffff !important;
        background-color: #333333 !important;
        border-color: #333333 !important;
        font-weight: 500 !important;
      }

      &.more {
        border: none !important;
        background: transparent !important;
        color: #999999 !important;

        &:hover {
          color: #666666 !important;
          background: transparent !important;
        }
      }
    }
  }

  .btn-prev,
  .btn-next {
    min-width: 32px !important;
    height: 32px !important;
    line-height: 30px !important;
    border: 1px solid #e5e5e5 !important;
    border-radius: 6px !important;
    margin: 0 4px !important;
    color: #666666 !important;
    background-color: #ffffff !important;
    transition: all 0.2s ease !important;

    &:hover:not(:disabled) {
      color: #333333 !important;
      border-color: #cccccc !important;
      background-color: #f8f8f8 !important;
    }

    &:disabled {
      color: #cccccc !important;
      background-color: #ffffff !important;
      border-color: #f0f0f0 !important;
      cursor: not-allowed !important;
    }

    .el-icon {
      font-size: 12px !important;
      font-weight: 500 !important;
    }
  }

  .el-pagination__total {
    color: #666666 !important;
    font-size: 13px !important;
    font-weight: 400 !important;
    margin-right: 16px !important;
  }

  // 移动端适配
  @media (max-width: 640px) {
    .el-pager li,
    .btn-prev,
    .btn-next {
      min-width: 28px !important;
      height: 28px !important;
      line-height: 26px !important;
      font-size: 12px !important;
      margin: 0 2px !important;
    }

    .el-pagination__total {
      font-size: 12px !important;
      margin-right: 8px !important;
    }
  }
}

// 移动端分页容器
.main-container.mobile {
  .pagination-container {
    padding: 0.3rem 0.5rem;
    margin-top: 0.2rem;
  }
}

// 通知卡片样式
.notice-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 70;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}

.notice-card {
  background: white;
  max-width: 400px;
  width: 90%;
  padding: 24px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  position: relative;
  box-sizing: border-box;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  animation: slideInScale 0.15s ease-out;
}

@keyframes slideInScale {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(-10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.notice-close {
  position: absolute;
  top: 16px;
  right: 16px;
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 50%;
  transition: background-color 0.15s ease;

  &:hover {
    background-color: #f5f5f5;
  }

  .el-icon {
    font-size: 20px;
    color: #8e8e93;
  }
}

.notice-header {
  margin-bottom: 20px;

  h2 {
    font-size: 24px;
    font-weight: 600;
    margin: 0 0 4px 0;
    color: #1c1c1e;
    line-height: 1.3;
  }

  .notice-date {
    font-size: 14px;
    color: #8e8e93;
    margin: 0;
    font-weight: 400;
  }
}

.notice-content {
  h3 {
    font-size: 20px;
    font-weight: 600;
    color: #1c1c1e;
    margin: 0 0 16px 0;
    line-height: 1.4;
  }

  .body-text {
    font-size: 16px;
    line-height: 1.5;
    color: #3c3c43;
    margin: 0;

    strong {
      color: #1c1c1e;
      font-weight: 600;
    }
  }
}

// 移动端适配
@media (max-width: 640px) {
  .notice-card {
    max-width: none;
    width: 95%;
    margin: 20px;
    padding: 20px;
    border-radius: 16px;

    .notice-close {
      top: 12px;
      right: 12px;
      width: 28px;
      height: 28px;

      .el-icon {
        font-size: 18px;
      }
    }

    .notice-header {
      margin-bottom: 16px;

      h2 {
        font-size: 20px;
      }

      .notice-date {
        font-size: 13px;
      }
    }

    .notice-content {
      h3 {
        font-size: 18px;
      }

      .body-text {
        font-size: 15px;
      }
    }
  }
}
</style>
