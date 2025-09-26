<template>
  <div>
    <div :class="isM ? 'mobile' : 'pc'" class="main-container">
      <div class="chat-container">
        <div class="chat-item" v-for="(item, index) in chats" :key="item.messageUuid || index">
          <div v-if="item.type == 'receive'" class="receive">
            <div class="avatar"></div>
            <div class="right">
              <div class="name">{{ getCharacterName() }}</div>
              <div class="content">{{ item.content }}</div>
            </div>
          </div>
          <div v-if="item.type == 'send'" class="send">
            <div class="left">
              <div class="name">ME</div>
              <div class="content">{{ item.content }}</div>
            </div>
            <div class="avatar"></div>
          </div>
        </div>
      </div>
      <div class="input-container">
        <div class="send-box">
          <el-input
            type="textarea"
            v-model="input"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="请输入内容"
            @keydown.enter.prevent="sendMessage"
            resize="none"
            class="chat-input"
          ></el-input>
          <button class="send-btn" @click="sendMessage">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
        <button class="phone" @click="videoChat = !videoChat">
          <el-icon><PhoneFilled /></el-icon>
        </button>
      </div>
      <div class="video-chat" v-if="videoChat">
        <div class="ai-avatar">
          <div class="avatar"></div>
          <div class="loading"></div>
        </div>
        <div class="control">
          <div class="control-item">
            <el-icon><Microphone /></el-icon>
          </div>
          <div class="control-item close">
            <el-icon><Close /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { conversationApi } from '@/api/modules/conversation'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ChatMessage } from '@/types/common'
import type { MessageResponse } from '@/types/api'
const isM = computed(() => isMobile())
const chats = ref<ChatMessage[]>([])
const isLoadingMessages = ref(false)
const hasMoreHistory = ref(true)
const currentOffset = ref(0)
const input = ref('')
const router = useRouter()
const route = useRoute()
const videoChat = ref(false)
const conversationUuid = computed(() => route.params.conversationUuid as string)
const currentConversation = ref<any>(null)
onMounted(() => {
  if (conversationUuid.value) {
    loadConversationAndMessages()
  }
})
// 这里可以放全局逻辑

// 监听路由参数变化，加载对应的对话消息
watch(
  () => route.params.conversationUuid,
  (newConversationUuid) => {
    if (newConversationUuid) {
      chats.value = []
      currentOffset.value = 0
      hasMoreHistory.value = true
      loadConversationAndMessages()
    }
  }
)

// 加载对话信息和消息
const loadConversationAndMessages = async () => {
  try {
    // 先加载对话信息
    await loadConversationInfo()
    // 再加载消息
    await loadRecentMessages()
  } catch (error) {
    console.error('加载对话和消息失败:', error)
  }
}

// 加载最新消息
const loadRecentMessages = async (limit: number = 20) => {
  if (!conversationUuid.value || isLoadingMessages.value) return

  try {
    isLoadingMessages.value = true
    const res = await conversationApi.getRecentMessages(conversationUuid.value, limit)
    if (res.code === 200) {
      // 将后端消息转换为前端所需的格式
      const messages = convertMessagesToChatFormat(res.data)
      // 按时间顺序排列（早的在前）
      chats.value = messages.reverse()
      currentOffset.value = res.data.length

      // 如果是新对话（没有消息），显示欢迎消息
      if (messages.length === 0) {
        await showWelcomeMessage()
      }
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
  } finally {
    isLoadingMessages.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async (limit: number = 20) => {
  if (!conversationUuid.value || isLoadingMessages.value || !hasMoreHistory.value) return

  try {
    isLoadingMessages.value = true
    const res = await conversationApi.getHistoryMessages(
      conversationUuid.value,
      currentOffset.value,
      limit
    )
    if (res.code === 200) {
      if (res.data.length === 0) {
        hasMoreHistory.value = false
        return
      }

      const messages = convertMessagesToChatFormat(res.data)
      // 将历史消息添加到列表开头
      chats.value = [...messages.reverse(), ...chats.value]
      currentOffset.value += res.data.length

      if (res.data.length < limit) {
        hasMoreHistory.value = false
      }
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    ElMessage.error('加载历史消息失败')
  } finally {
    isLoadingMessages.value = false
  }
}

// 将后端消息转换为前端所需的格式
const convertMessagesToChatFormat = (messages: MessageResponse[]): ChatMessage[] => {
  return messages.map(msg => ({
    messageUuid: msg.messageUuid,
    type: msg.senderType === 1 ? 'send' : 'receive',
    content: msg.textContent,
    senderType: msg.senderType,
    contentType: msg.contentType,
    audioUrl: msg.audioUrl,
    createDate: msg.createDate,
    metadata: msg.metadata
  }))
}

// 发送消息
const sendMessage = async () => {
  if (!input.value.trim() || !conversationUuid.value) return

  const messageContent = input.value.trim()
  input.value = ''

  // 立即在界面上显示用户消息
  const userMessage: ChatMessage = {
    type: 'send',
    content: messageContent,
    senderType: 1,
    contentType: 1,
    createDate: new Date().toISOString()
  }
  chats.value.push(userMessage)

  // TODO: 调用发送消息接口（后端还未提供）
  try {
    // 这里需要等待后端实现发送消息的接口
    // const response = await conversationApi.sendMessage(conversationUuid.value, { content: messageContent })

    // 模拟AI回复（临时用）
    setTimeout(() => {
      const aiMessage: ChatMessage = {
        type: 'receive',
        content: '谢谢您的消息，这是一个模拟回复。待后端接口完成后将获得真实AI回复。',
        senderType: 2,
        contentType: 1,
        createDate: new Date().toISOString()
      }
      chats.value.push(aiMessage)
    }, 1000)
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
    // 发送失败时移除用户消息
    chats.value.pop()
    input.value = messageContent // 恢复输入内容
  }
}

// 显示欢迎消息
const showWelcomeMessage = async () => {
  try {
    if (currentConversation.value) {
      const characterName = currentConversation.value.characterName || 'AI助手'
      const defaultGreeting = `你好！我是${characterName}，很高兴和你对话！有什么我可以帮助你的吗？`

      const welcomeMessage: ChatMessage = {
        type: 'receive',
        content: defaultGreeting,
        senderType: 2,
        contentType: 1,
        createDate: new Date().toISOString()
      }
      chats.value.push(welcomeMessage)
    }
  } catch (error) {
    console.error('显示欢迎消息失败:', error)
  }
}

// 获取对话信息
const loadConversationInfo = async () => {
  try {
    const res = await conversationApi.getConversationList()
    if (res.code === 200) {
      // 找到当前对话的信息
      currentConversation.value = res.data.find(
        (conv: any) => conv.conversationUuid === conversationUuid.value
      )
    }
  } catch (error) {
    console.error('获取对话信息失败:', error)
  }
}

// 获取角色名称
const getCharacterName = () => {
  return currentConversation.value?.characterName || 'AI助手'
}
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  padding: 0.3rem 0.7rem;
  position: relative;
  &.mobile {
    height: calc(100vh - 0.6rem);
    padding: 0.1rem 0;
    width: 100%;

    .send-box {
      width: 100%;
    }
    .chat-input {
      width: 80%;
      padding: 0.05rem;
    }
    .phone {
      margin-left: 0.15rem;
    }
    .chat-container {
      width: 100%;
      padding: 0 0.1rem;
    }
    .avatar {
      width: 0.3rem;
      height: 0.3rem;
    }
    .left,
    .right {
      margin: 0 0.1rem;
    }
  }
}
.chat-container {
  width: 80%;
  flex: 1;
  overflow-y: auto;

  .chat-item {
    width: 100%;
    .receive {
      display: flex;
      width: 100%;
      justify-content: start;
      margin: 0.1rem 0;
    }
    .send {
      display: flex;
      justify-content: end;
      width: 100%;
      margin: 0.1rem 0;
    }
    .avatar {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      background-color: #ddd;
    }
    .name {
      font-size: 0.14rem;
      color: #999;
    }
    .content {
      font-size: 0.16rem;
      color: #000;
      padding: 0.1rem 0.2rem;
      border-radius: 0.3rem;
      background-color: #f9f9f9;
      border: 0.01rem solid #eaeaea;
      margin-top: 0.1rem;
    }
    .left,
    .right {
      margin: 0 0.2rem;
      max-width: 50%;
    }
    .left {
      .name {
        text-align: right;
      }
      .content {
        border-top-right-radius: 0;
      }
    }
    .right {
      .content {
        border-top-left-radius: 0;
      }
    }
  }
}

.input-container {
  width: 80%;
  display: flex;
  justify-content: center;
  align-items: center;
  .send-box {
    border: 1px solid #ccc;
    width: 80%;
    // height: 0.5rem;
    border-radius: 0.3rem;
    overflow: hidden;
    display: flex;
    align-items: end;
    justify-content: center;
    .chat-input {
      resize: none;
      width: 90%;
      height: auto;
      padding: 0.1rem;
      border: none;
      &:focus {
        outline: none;
      }
      :deep(.el-textarea__inner) {
        background-color: transparent;
        font-size: 0.2rem;
        box-shadow: none;
      }
    }
    .send-btn {
      width: 0.5rem;
      height: 0.5rem;
      margin-bottom: 0.03rem;
      margin-left: 0.03rem;

      background-color: #000;
      border-radius: 50%;
      border: none;
      cursor: pointer;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      :deep(.el-icon) {
        font-size: 0.25rem;
        svg {
          font-size: 0.25rem;
          font-weight: bold;
        }
      }
    }
  }
  .phone {
    margin-left: 0.2rem;
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background-color: #ddd;
    color: #000;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    cursor: pointer;
    &:hover {
      background-color: #ccc;
    }
    :deep(.el-icon) {
      font-size: 0.25rem;
      svg {
        font-size: 0.25rem;
        font-weight: bold;
      }
    }
  }
}

.video-chat {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top left, #fff, #fce9e9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  .header {
  }
  .ai-avatar {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    .avatar {
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background-color: #ddd;
      margin: auto;
    }
  }
  .control {
    display: flex;
    justify-content: center;
    margin: 1rem auto;
    :deep(.el-icon) {
      font-size: 0.5rem;
      svg {
        font-size: 0.5rem;
      }
    }
    .control-item {
      background-color: #e5e5e5;
      width: 1rem;
      height: 1rem;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      cursor: pointer;
      margin: 0 0.5rem;
      &:hover {
        background-color: #ddd;
      }
    }
    .close {
      color: #f00;
      font-weight: bold;
      margin-left: 1rem;
    }
  }
}
</style>
