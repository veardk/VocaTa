<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">
      <div class="title">探索</div>
      <div class="search">
        <input
          type="text"
          v-model="searchInput"
          placeholder="搜索角色"
          @input="search"
          @keyup.enter="search"
        />
        <el-icon>
          <Search />
        </el-icon>
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
      <div>
        <span
          :class="searchParam.orderDirection == 'desc' ? 'active-config' : ''"
          @click="searchParam.orderDirection = 'desc'"
          >热门</span
        >
        <span
          :class="searchParam.orderDirection == 'asc' ? 'active-config' : ''"
          @click="searchParam.orderDirection = 'asc'"
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
    <el-pagination
      :size="isM ? 'small' : 'large'"
      background
      style="margin: 0.2rem"
      layout="prev, pager, next,total"
      :total="total"
      @prev-click="searchParam.pageNum--"
      @next-click="searchParam.pageNum++"
      @current-change="searchParam.pageNum = $event"
    >
    </el-pagination>
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
const isM = computed(() => isMobile())
const router = useRouter()

const roleSelected = ref<roleInfo>()
const searchInput = ref('')
const roleList: Ref<roleInfo[]> = ref([])
const selectRoleList: Ref<roleInfo[]> = ref([])
const cardFace = ref([0, 0, 0, 0, 0])
const searchParam: Ref<PublicRoleQuery> = ref({
  pageNum: 1,
  pageSize: 15,
  orderDirection: 'desc',
})
const total = ref(0)

const seachStatus = ref(true)
const infoShow = ref(false)
const loading1 = ref(false)
const loading2 = ref(false)
// 这里可以放全局逻辑
watch(
  searchParam,
  () => {
    getRoleList()
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
const debouncedSearch = debounce(async () => {
  console.log('搜索:', searchInput.value)
  if (searchInput.value != '') {
    seachStatus.value = false
    const res = await roleApi.searchRole({ keyword: searchInput.value })
    console.log(searchInput.value)
    roleList.value = res.data.list
  } else {
    seachStatus.value = true
    getRoleList()
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
      .search {
        width: 60%;
        max-width: 3rem;
        height: 0.3rem;
        padding: 0 0.1rem;
        input {
          width: 50%;
          font-size: 0.2rem;
          margin: 0;
          padding: 0 0.05rem;
        }
        .el-icon {
          font-size: 0.2rem;
          margin: 0;
          padding: 0;
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

  .search {
    width: 40%;
    max-width: 4rem;
    height: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #ddd;
    border-radius: 0.25rem;
    overflow: hidden;
    background-color: #fff;
    box-shadow: 0 0.02rem 0.08rem rgba(0, 0, 0, 0.1);
    transition: all 0.2s ease;

    &:hover {
      border-color: #409eff;
      box-shadow: 0 0.04rem 0.12rem rgba(64, 158, 255, 0.2);
    }

    &:focus-within {
      border-color: #409eff;
      box-shadow: 0 0 0 0.02rem rgba(64, 158, 255, 0.2);
    }
    input {
      flex: 1;
      height: 95%;
      border: none;
      padding: 0.2rem;
      outline: none;
      font-size: 0.3rem;
      background-color: transparent;
    }
    .el-icon {
      font-size: 0.3rem;
      margin: 0.2rem;
      cursor: pointer;
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
    background-size: 100% 100%;
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
      transform: translate(-50%, -50%) scale(1.1) !important;
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
    background-size: 100% 100%;
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
:deep(.el-pagination) {
  .el-icon {
    font-size: 0.14rem;
    font-weight: bold;
    svg {
      font-size: 0.14rem;
    }
  }
}
</style>
