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
        :style="getCardStyle(index)"
        @mouseenter="cardFace[index] = 1"
        @mouseleave="cardFace[index] = 0"
      >
        <div
          class="card-face card-front"
          vShow="cardFace=='card-front'"
          v-show="cardFace[index] == 0"
        >
          <div class="avatar"><img :src="item.avatarUrl" alt="" /></div>
          <div class="name">{{ item.name }}</div>
        </div>
        <div
          class="card-face card-back"
          vShow="!cardFace=='card-front'"
          v-show="cardFace[index] == 1"
        >
          <div class="top">
            <div class="avatar"><img :src="item.avatarUrl" alt="" /></div>
            <div class="name">{{ item.name }}</div>
          </div>
          <div class="description">{{ item.description }}</div>
          <div class="goto" @click.stop="startConversation(item.id)">开始对话</div>
        </div>
      </div>
    </div>
    <!--角色列表-->
    <div class="role-list" v-loading="loading2" element-loading-text="Loading...">
      <div v-for="item in roleList" :key="item.id" class="role-list-item">
        <div class="card-face card-front" vShow="cardFace=='card-front'">
          <div class="avatar"><img :src="item.avatarUrl" alt="" /></div>
          <div class="name">{{ item.name }}</div>
        </div>
        <div class="card-face card-back" vShow="!cardFace=='card-front'">
          <div class="top">
            <div class="avatar"><img :src="item.avatarUrl" alt="" /></div>
            <div class="name">{{ item.name }}</div>
          </div>
          <div class="description">{{ item.description }}</div>
          <div class="goto" @click.stop="startConversation(item.id)">开始对话</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { roleApi } from '@/api/modules/role'
import { conversationApi } from '@/api/modules/conversation'
import type { PublicRoleQuery } from '@/types/api'
import type { roleInfo } from '@/types/common'
import debounce from '@/types/debounce'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
const isM = computed(() => isMobile())
const router = useRouter()

const searchInput = ref('')
const roleList: Ref<roleInfo[]> = ref([])
const selectRoleList: Ref<roleInfo[]> = ref([])
const cardFace = ref([0, 0, 0, 0, 0])
const searchParam: Ref<PublicRoleQuery> = ref({
  pageNum: 1,
  pageSize: 10,
})

const seachStatus = ref(true)

const loading1 = ref(false)
const loading2 = ref(false)
// 这里可以放全局逻辑

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
    const loadingMessage = ElMessage.loading('正在创建对话...')

    // 调用创建对话接口，确保ID转换为字符串
    const res = await conversationApi.createConversation({
      characterId: String(characterId)
    })

    loadingMessage.close()

    console.log('创建对话响应:', res)

    if (res.code === 200) {
      const conversationUuid = res.data.conversationUuid
      ElMessage.success('对话创建成功！')

      // 跳转到聊天页面
      router.push(`/chat/${conversationUuid}`)
    } else {
      ElMessage.error('创建对话失败：' + res.message)
    }
  } catch (error) {
    console.error('创建对话失败:', error)
    ElMessage.error('创建对话失败，请稍后重试')
  }
}
</script>

<style lang="scss" scoped>
.main-container {
  padding: 0.3rem 0.5rem;

  &.mobile {
    padding: 0;
    .header {
      padding: 0 0.2rem 0;

      .title {
        font-size: 0.2rem;
      }
      .search {
        width: 70%;
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
    .role-list {
      padding-left: 0.2rem;
      padding-right: 0.2rem;
      margin: 0.1rem 0;
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.1rem;
      .role-list-item {
        width: 100%;
        height: 2rem;
        font-size: 0.2rem;
      }
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
        font-size: 0.2rem;
      }
    }
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .title {
    font-size: 0.3rem;
    font-weight: bold;
  }

  .search {
    width: 50%;
    height: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #ccc;
    border-radius: 0.1rem;
    overflow: hidden;
    background-color: #f0f0f0;
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
  .banner-item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    width: 2.5rem;
    height: 3rem;
    background-color: #eee;
    border-radius: 0.2rem;
    backdrop-filter: blur(0.1rem);
    border: 1px solid rgba(255, 255, 255, 0.2);
    box-shadow: 0 0.1rem 0.3rem rgba(0, 0, 0, 0.2);
    // padding: 0 0.2rem;
    transition: all 0.3s ease;
    position: absolute;
    cursor: pointer;
    &:hover {
      background: rgba(255, 255, 255, 0.2);
      z-index: 5;
      transform: translate(-50%, -50%) scale(1.1) !important;
    }
  }
  &:last-child {
    margin-right: 0;
  }
}
// 角色列表
.role-list {
  border-top: 1px solid #ccc;
  padding-top: 0.3rem;
  display: grid;
  margin-top: 0.2rem;

  grid-template-columns: repeat(5, 1fr); /* 一行五个 */
  gap: 0.2rem; /* 间距0.1rem */
  .role-list-item {
    width: 2.5rem;
    height: 3rem;
    font-size: 0.3rem;
    background-color: #eee;
    transform-style: preserve-3d;
    transition: transform 0.8s;
    border: 1px solid #ccc;
    cursor: pointer;
    border-radius: 0.1rem;
    // overflow: hidden;
    position: relative;
    &:hover {
      transform: rotateY(180deg);
    }
  }
  .card-face {
    backface-visibility: hidden;
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
  align-self: center;
  height: 100%;
  position: absolute;
  top: 0;
}

.card-front {
  // background: linear-gradient(45deg, #ff9a9e 0%, #fad0c4 100%);
  color: #333;
  .avatar {
    width: 100%;
    border-radius: 50%;
    margin: 0.02rem auto;
    overflow: hidden;
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  .name {
    font-size: 0.3rem;
    font-weight: bold;
    text-align: center;
  }
}

.card-back {
  // background: linear-gradient(45deg, #a1c4fd 0%, #c2e9fb 100%);
  color: #333;
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
  .top {
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0.05rem auto;
  }
  .name {
    margin-left: 0.1rem;
    font-size: 0.25rem;
    font-weight: bold;
    text-align: center;
  }
  .description {
    font-size: 0.2rem;
    text-align: center;
    overflow-y: auto;
    padding: 0.05rem;
    flex: 1;
  }
  .goto {
    font-size: 0.2rem;
    text-align: center;
    margin: 0.1rem;
    background-color: #333;
    border-radius: 0.1rem;
    padding: 0.1rem;
    color: #fff;
    cursor: pointer;
  }
}
</style>
