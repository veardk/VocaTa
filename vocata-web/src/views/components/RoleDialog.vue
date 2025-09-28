<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main">
    <div class="header">
      <div class="title">角色详情</div>
      <div class="close" @click="$emit('close')">
        <el-icon>
          <Close />
        </el-icon>
      </div>
    </div>

    <div class="main-part">
      <div class="top">
        <div class="left">
          <div class="avatar">
            <img :src="item.avatarUrl" alt="" />
          </div>
          <div class="name">{{ item.name }}</div>
        </div>
        <div class="creator">作者：@ {{ item.creatorId ? `用户${item.creatorId}` : '官方' }}</div>
      </div>
      <div class="description flex">
        <div class="label">描述：</div>
        <div class="value">{{ item.description || '暂无描述' }}</div>
      </div>
      <div class="hello flex">
        <div class="label">开场白：</div>
        <div class="value">{{ item.greeting || '暂无开场白' }}</div>
      </div>
      <div class="tags flex">
        <div class="label">标签：</div>
        <div class="value tags-container">
          <span class="tag" v-for="(tag, index) in tagsList" :key="index">{{ tag }}</span>
          <span v-if="!tagsList || tagsList.length === 0" class="no-tags">暂无标签</span>
        </div>
      </div>
      <div class="goto" @click="startConversation">开始对话</div>
    </div>
  </div>
  <div class="bgc"></div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { chatHistoryStore } from '@/store'
import { ElMessage } from 'element-plus'

const { item } = defineProps({
  item: Object,
})

const emit = defineEmits(['close'])
const router = useRouter()
const isM = computed(() => isMobile())

// 处理标签数据：将字符串转换为数组，并清理特殊字符
const tagsList = computed(() => {
  if (!item.tags) return []

  let tags = []

  // 如果tags是字符串，尝试按逗号分割
  if (typeof item.tags === 'string') {
    tags = item.tags.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0)
  } else if (Array.isArray(item.tags)) {
    // 如果已经是数组，直接使用
    tags = item.tags
  } else {
    return []
  }

  // 清理标签中的特殊字符：#、引号、方括号等
  return tags.map(tag => {
    return tag
      .replace(/^["'\[\]#\s]+|["'\[\]#\s]+$/g, '') // 去掉开头和结尾的特殊字符
      .replace(/["'\[\]#]/g, '') // 去掉中间的特殊字符
      .trim()
  }).filter(tag => tag.length > 0)
})

// 开始对话功能
const startConversation = async () => {
  try {
    console.log('角色详情弹窗 - 开始对话，角色ID:', item.id)

    if (!item.id) {
      console.error('角色ID为空')
      ElMessage.error('角色信息有误，请重试')
      return
    }

    // 添加加载状态
    const loadingMessage = ElMessage.info('正在创建对话...')

    // 调用创建对话接口
    const conversationUuid = await chatHistoryStore().addChatHistory(item.id)

    loadingMessage.close()

    ElMessage.success('对话创建成功！')

    // 关闭弹窗
    emit('close')

    // 跳转到聊天页面
    router.push(`/chat/${conversationUuid}`)
  } catch (error) {
    console.error('创建对话失败:', error)
    ElMessage.error('创建对话失败，请稍后重试')
  }
}
</script>

<style lang="scss" scoped>
.bgc {
  width: 100%;
  height: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.2);
  z-index: 5;
}
.main {
  width: 50%;
  max-height: 70%;
  overflow-y: auto;
  background-color: #f5f5f5;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  border-radius: 0.1rem;
  border: 1px solid #ccc;
  display: flex;
  flex-direction: column;
  padding: 0.3rem;
  &.mobile {
    width: 95%;
    position: fixed;
    padding: 0.2rem;
    .top {
      margin: 0.1rem 0;
      .avatar {
        width: 0.7rem;
        height: 0.8rem;
        margin-right: 0.1rem;
      }
    }
    .main-part {
      padding: 0;
      .flex {
        margin: 0.1rem 0;
      }
      .label,
      .value {
        font-size: 0.16rem;
      }
      .tag {
        font-size: 0.16rem;
        padding: 0.08rem 0.12rem;
      }
      .no-tags {
        font-size: 0.16rem;
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
  .close {
    cursor: pointer;
  }
}
.main-part {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0 0.2rem;
}
.top {
  width: 100%;
  margin: 0.1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .left {
    display: flex;
    align-items: center;
    width: 55%;

    .name {
      flex: 1;
      overflow: hidden;
      white-space: wrap;
    }
    .avatar {
      width: 1rem;
      height: 1rem;
      border-radius: 0.1rem;
      overflow: hidden;
      margin-right: 0.2rem;
      img {
        width: 100%;
        height: 100%;
      }
    }
  }
  .creator {
    font-size: 0.16rem;
  }
}

.label {
  text-align: right;
  white-space: nowrap;
  width: 4.5em;
}
.value {
  flex: 1;
}
.flex {
  width: 100%;
  display: flex;
  justify-content: start;
  align-items: center;
  margin: 0.1rem;
}

.tags {
  display: flex;
  align-items: center;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 0.08rem;
  align-items: center;
}

.tag {
  display: inline-block;
  margin: 0;
  padding: 0.08rem 0.12rem;
  border-radius: 0.08rem;
  background-color: #f0f0f0;
  border: 1px solid #d5d5d5;
  color: #333333;
  font-size: 0.16rem;
  font-weight: 400;
  line-height: 1.3;
  white-space: nowrap;
  transition: all 0.2s ease;

  &:hover {
    background-color: #e5e5e5;
    border-color: #bbb;
  }
}

.no-tags {
  color: #999999;
  font-size: 0.16rem;
  font-style: italic;
}
.goto {
  // width: 20%;
  align-self: end;
  font-size: 0.2rem;
  text-align: center;
  margin: 0.05rem 0.1rem;
  background-color: #000;
  border-radius: 0.1rem;
  padding: 0.1rem 0.2rem;
  color: #fff;
  cursor: pointer;
  &:hover {
    background-color: #333;
    transform: translateY(-1px);
  }
}
</style>
