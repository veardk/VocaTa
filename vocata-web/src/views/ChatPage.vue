<template>
  <div>
    <div :class="isM ? 'mobile' : 'pc'" class="main-container">
      <!-- 连接状态提示 -->
      <div v-if="!aiChat?.connected" class="connection-status">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>{{ connectionStatus }}</span>
      </div>

      <div class="chat-container" ref="chatContainer">
        <div class="chat-item" v-for="(item, index) in chats" :key="item.messageUuid || index">
          <div v-if="item.type == 'receive'" class="receive">
            <div class="avatar"></div>
            <div class="right">
              <div class="name">{{ getCharacterName() }}</div>
              <div class="content" :class="{ 'streaming': item.isStreaming }">
                {{ item.content }}
                <span v-if="item.isStreaming" class="cursor">|</span>
              </div>
              <div v-if="item.createDate" class="time">{{ formatTime(item.createDate) }}</div>
            </div>
          </div>
          <div v-if="item.type == 'send'" class="send">
            <div class="left">
              <div class="name">ME</div>
              <div class="content" :class="{ 'recognizing': item.isRecognizing }">
                {{ item.content }}
                <span v-if="item.isRecognizing" class="recognition-tip">(识别中...)</span>
              </div>
              <div v-if="item.createDate" class="time">{{ formatTime(item.createDate) }}</div>
            </div>
            <div class="avatar"></div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="isAIThinking" class="ai-thinking">
          <div class="avatar"></div>
          <div class="thinking-content">
            <div class="name">{{ getCharacterName() }}</div>
            <div class="thinking-dots">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-container">
        <div class="send-box">
          <el-input
            type="textarea"
            v-model="input"
            :autosize="{ minRows: 1, maxRows: 5 }"
            :placeholder="aiChat?.connected ? '输入消息或点击通话按钮开始语音对话...' : '连接中，请稍等...'"
            @keydown.enter.prevent="sendMessage"
            :disabled="!aiChat?.connected"
            resize="none"
            class="chat-input"
          ></el-input>
          <button class="send-btn" @click="sendMessage" :disabled="!aiChat?.connected || !input.trim()">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
        <button
          class="phone"
          :class="{ active: isAudioCallActive, recording: aiChat?.recording }"
          @click="toggleAudioCall"
          :disabled="!aiChat?.connected"
          :title="isAudioCallActive ? '点击挂断通话' : '点击开始语音通话'"
        >
          <el-icon v-if="!isAudioCallActive"><PhoneFilled /></el-icon>
          <el-icon v-else><Close /></el-icon>
        </button>
      </div>

      <!-- 音频通话界面 -->
      <div class="video-chat" v-if="isAudioCallActive">
        <div class="ai-avatar">
          <div class="avatar" :class="{ pulsing: aiChat?.playing }"></div>
          <div class="character-name">{{ getCharacterName() }}</div>
          <div v-if="currentSTTText" class="stt-display">
            <div class="stt-label">您说的是：</div>
            <div class="stt-text">{{ currentSTTText }}</div>
          </div>
        </div>
        <div class="control">
          <div
            class="control-item mic"
            :class="{ active: aiChat?.recording, muted: !aiChat?.recording }"
            @click="toggleMicrophone"
          >
            <el-icon><Microphone /></el-icon>
          </div>
          <div class="control-item close" @click="stopAudioCall">
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
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ChatMessage } from '@/types/common'
import type { MessageResponse } from '@/types/api'
import { VocaTaAIChat } from '@/utils/aiChat'
import { getToken } from '@/utils/token'
const isM = computed(() => isMobile())
const chats = ref<ChatMessage[]>([])
const isLoadingMessages = ref(false)
const hasMoreHistory = ref(true)
const currentOffset = ref(0)
const input = ref('')
const router = useRouter()
const route = useRoute()
const conversationUuid = computed(() => route.params.conversationUuid as string)
const currentConversation = ref<any>(null)

// AI对话相关状态
const aiChat = ref<VocaTaAIChat | null>(null)
const isAudioCallActive = ref(false)
const connectionStatus = ref('正在连接...')
const isAIThinking = ref(false)
const currentSTTText = ref('')
const currentStreamingMessage = ref<ChatMessage | null>(null)

// 引用
const chatContainer = ref<HTMLElement>()
onMounted(async () => {
  if (conversationUuid.value) {
    try {
      await loadConversationAndMessages()
      // 只有在对话加载成功时才初始化AI系统
      if (currentConversation.value) {
        await initializeAIChat()
      }
    } catch (error) {
      console.error('❌ 页面初始化失败:', error)
      // 如果对话不存在，跳转到角色选择页
      if ((error as Error).message.includes('对话不存在')) {
        router.push('/searchRole')
      }
    }
  }
})

onUnmounted(() => {
  // 清理AI对话系统资源
  if (aiChat.value) {
    aiChat.value.destroy()
  }
})
// 这里可以放全局逻辑

// 监听路由参数变化，加载对应的对话消息
watch(
  () => route.params.conversationUuid,
  async (newConversationUuid, oldConversationUuid) => {
    console.log('🔄 路由变化 - 旧UUID:', oldConversationUuid, '新UUID:', newConversationUuid)

    if (newConversationUuid) {
      // 清理之前的AI对话系统
      if (aiChat.value) {
        console.log('🧹 清理之前的AI对话系统')
        aiChat.value.destroy()
        aiChat.value = null
      }

      // 强制重置所有状态，确保不使用缓存
      console.log('🔄 重置所有状态')
      chats.value = []
      currentConversation.value = null
      currentOffset.value = 0
      hasMoreHistory.value = true
      isAudioCallActive.value = false
      currentSTTText.value = ''
      isAIThinking.value = false
      currentStreamingMessage.value = null

      try {
        // 重新加载（强制不使用缓存）
        await loadConversationAndMessages()

        // 只有在对话加载成功且UUID仍然匹配时才初始化AI系统
        if (conversationUuid.value === newConversationUuid && currentConversation.value) {
          await initializeAIChat()
        }
      } catch (error) {
        console.error('❌ 加载对话失败:', error)
        // 如果对话不存在，跳转到角色选择页
        if ((error as Error).message.includes('对话不存在')) {
          router.push('/searchRole')
        }
        // 如果加载失败，不初始化AI系统
      }
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
  if (!input.value.trim() || !conversationUuid.value || !aiChat.value?.connected) return

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
  scrollToBottom()

  // 显示AI思考状态
  isAIThinking.value = true

  try {
    // 通过WebSocket发送消息给AI
    aiChat.value.sendTextMessage(messageContent)
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
    isAIThinking.value = false

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

// 获取对话信息 - 每次都重新获取最新数据，不使用缓存
const loadConversationInfo = async () => {
  try {
    console.log('📋 加载对话信息 - UUID:', conversationUuid.value)

    // 每次都重新获取完整的对话列表，确保获取到最新状态
    const res = await conversationApi.getConversationList()
    if (res.code === 200) {
      // 从最新的对话列表中查找当前对话
      const conversation = res.data.find(
        (conv: any) => conv.conversationUuid === conversationUuid.value
      )

      if (!conversation) {
        console.warn('⚠️ 在对话列表中找不到当前对话UUID:', conversationUuid.value)
        console.log('📋 可用的对话列表:', res.data.map((c: any) => ({
          uuid: c.conversationUuid,
          title: c.title,
          characterName: c.characterName
        })))

        // 如果找不到对话，可能是对话已被删除或UUID已变化
        ElMessage.error('对话不存在或已过期，请重新选择角色')
        // 抛出错误而不是直接跳转，让调用方处理跳转逻辑
        throw new Error('对话不存在或已过期')
      }

      currentConversation.value = conversation
      console.log('✅ 对话信息加载完成:', {
        uuid: conversation.conversationUuid,
        title: conversation.title,
        characterName: conversation.characterName
      })
    } else {
      throw new Error('获取对话列表失败: ' + res.message)
    }
  } catch (error) {
    console.error('❌ 获取对话信息失败:', error)
    ElMessage.error('加载对话信息失败，请刷新页面重试')
    throw error
  }
}

// 获取角色名称
const getCharacterName = () => {
  return currentConversation.value?.characterName || 'AI助手'
}

// 初始化AI对话系统
const initializeAIChat = async () => {
  try {
    if (!conversationUuid.value) {
      throw new Error('对话UUID不能为空')
    }

    // 从token中获取用户ID（这里需要根据实际token结构调整）
    const token = getToken()
    if (!token) {
      throw new Error('用户未登录')
    }

    console.log('🚀 初始化AI对话系统 - conversationUuid:', conversationUuid.value)

    connectionStatus.value = '正在连接AI系统...'

    // 创建AI对话实例
    aiChat.value = new VocaTaAIChat()

    // 设置回调函数
    setupAIChatCallbacks()

    // 初始化AI对话系统
    await aiChat.value.initialize(conversationUuid.value)

  } catch (error) {
    console.error('❌ 初始化AI对话系统失败:', error)
    connectionStatus.value = '连接失败，请刷新页面重试'
    ElMessage.error('AI对话系统初始化失败: ' + (error as Error).message)
  }
}

// 设置AI对话系统的回调函数
const setupAIChatCallbacks = () => {
  if (!aiChat.value) return

  // 连接状态回调
  aiChat.value.onConnectionStatus((status, message) => {
    switch (status) {
      case 'connected':
        connectionStatus.value = '已连接到AI服务'
        break
      case 'disconnected':
        connectionStatus.value = '连接已断开，正在重连...'
        break
      case 'error':
        connectionStatus.value = '连接失败'
        break
    }
  })

  // STT识别结果回调
  aiChat.value.onSTTResult((text, isFinal) => {
    currentSTTText.value = text

    if (isFinal) {
      // 最终识别结果，添加到聊天记录
      const userMessage: ChatMessage = {
        type: 'send',
        content: text,
        senderType: 1,
        contentType: 2, // 语音类型
        createDate: new Date().toISOString()
      }
      chats.value.push(userMessage)
      scrollToBottom()

      // 显示AI思考状态
      isAIThinking.value = true
      currentSTTText.value = '' // 清空显示
    }
  })

  // LLM流式文本回调
  aiChat.value.onLLMStream((text, isComplete, characterName) => {
    isAIThinking.value = false

    if (!currentStreamingMessage.value) {
      // 创建新的流式消息
      currentStreamingMessage.value = {
        type: 'receive',
        content: text,
        senderType: 2,
        contentType: 1,
        createDate: new Date().toISOString(),
        isStreaming: !isComplete
      }
      chats.value.push(currentStreamingMessage.value)
    } else {
      // 更新现有的流式消息
      currentStreamingMessage.value.content = text
      currentStreamingMessage.value.isStreaming = !isComplete
    }

    scrollToBottom()

    if (isComplete) {
      // 流式完成，重置状态
      currentStreamingMessage.value = null
    }
  })

  // 音频播放状态回调
  aiChat.value.onAudioPlay((isPlaying) => {
    // 可以在这里添加音频播放的视觉反馈
    console.log('🔊 音频播放状态:', isPlaying)
  })
}

// 音频通话相关方法
const toggleAudioCall = async () => {
  if (isAudioCallActive.value) {
    stopAudioCall()
  } else {
    await startAudioCall()
  }
}

const startAudioCall = async () => {
  try {
    if (!aiChat.value) {
      ElMessage.error('AI对话系统未初始化')
      return
    }

    console.log('📞 开始音频通话')
    await aiChat.value.startAudioCall()
    isAudioCallActive.value = true

  } catch (error) {
    console.error('❌ 启动音频通话失败:', error)
    ElMessage.error('无法启动音频通话: ' + (error as Error).message)
  }
}

const stopAudioCall = () => {
  try {
    if (!aiChat.value) return

    console.log('📞 停止音频通话')
    aiChat.value.stopAudioCall()
    isAudioCallActive.value = false
    currentSTTText.value = ''

  } catch (error) {
    console.error('❌ 停止音频通话失败:', error)
  }
}

const toggleMicrophone = async () => {
  if (!aiChat.value || !isAudioCallActive.value) return

  try {
    if (aiChat.value.recording) {
      // 当前在录音，停止录音
      aiChat.value.stopAudioCall()
      await aiChat.value.startAudioCall() // 重新开始但不录音
    } else {
      // 当前没有录音，开始录音
      aiChat.value.stopAudioCall()
      await aiChat.value.startAudioCall()
    }
  } catch (error) {
    console.error('❌ 切换麦克风状态失败:', error)
    ElMessage.error('切换麦克风状态失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (dateString: string) => {
  return new Date(dateString).toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit'
  })
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

// 连接状态提示
.connection-status {
  position: absolute;
  top: 0.2rem;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 0.1rem 0.2rem;
  border-radius: 0.15rem;
  font-size: 0.14rem;
  display: flex;
  align-items: center;
  gap: 0.1rem;
  z-index: 1000;

  .loading-icon {
    animation: spin 1s linear infinite;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
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
      flex-shrink: 0;
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
      position: relative;

      // 流式文本显示效果
      &.streaming {
        .cursor {
          animation: blink 1s infinite;
          font-weight: bold;
          color: #007bff;
        }
      }

      // 语音识别中的样式
      &.recognizing {
        background-color: #e3f2fd;
        border-color: #2196f3;

        .recognition-tip {
          font-size: 0.12rem;
          color: #2196f3;
          font-style: italic;
        }
      }
    }

    .time {
      font-size: 0.12rem;
      color: #ccc;
      margin-top: 0.05rem;
    }

    .left,
    .right {
      margin: 0 0.2rem;
      max-width: 70%;
      word-wrap: break-word;
    }

    .left {
      .name {
        text-align: right;
      }

      .content {
        border-top-right-radius: 0;
        background-color: #007bff;
        color: white;
        border-color: #007bff;
      }

      .time {
        text-align: right;
      }
    }

    .right {
      .content {
        border-top-left-radius: 0;
      }
    }
  }
}

// AI思考状态
.ai-thinking {
  display: flex;
  align-items: flex-start;
  margin: 0.1rem 0;

  .avatar {
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background-color: #ddd;
    margin-right: 0.2rem;
  }

  .thinking-content {
    .name {
      font-size: 0.14rem;
      color: #999;
      margin-bottom: 0.05rem;
    }

    .thinking-dots {
      display: flex;
      gap: 0.05rem;

      span {
        width: 0.06rem;
        height: 0.06rem;
        background-color: #007bff;
        border-radius: 50%;
        animation: thinking 1.4s infinite both;

        &:nth-child(2) {
          animation-delay: 0.2s;
        }

        &:nth-child(3) {
          animation-delay: 0.4s;
        }
      }
    }
  }
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

@keyframes thinking {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1.2); opacity: 1; }
}

.input-container {
  width: 80%;
  display: flex;
  justify-content: center;
  align-items: center;

  .send-box {
    border: 1px solid #ccc;
    width: 80%;
    border-radius: 0.3rem;
    overflow: hidden;
    display: flex;
    align-items: end;
    justify-content: center;
    transition: border-color 0.3s;

    &:focus-within {
      border-color: #007bff;
    }

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
      background-color: #007bff;
      border-radius: 50%;
      border: none;
      cursor: pointer;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;

      &:disabled {
        background-color: #ccc;
        cursor: not-allowed;
      }

      &:not(:disabled):hover {
        background-color: #0056b3;
        transform: scale(1.05);
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

  .phone {
    margin-left: 0.2rem;
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background-color: #28a745;
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    cursor: pointer;
    transition: all 0.3s;
    position: relative;

    &:disabled {
      background-color: #ccc;
      cursor: not-allowed;
    }

    &:not(:disabled):hover {
      background-color: #1e7e34;
      transform: scale(1.05);
    }

    &.active {
      background-color: #dc3545;

      &:hover {
        background-color: #c82333;
      }
    }

    &.recording {
      animation: pulse-red 2s infinite;

      &::after {
        content: '';
        position: absolute;
        width: 100%;
        height: 100%;
        border-radius: 50%;
        background-color: rgba(220, 53, 69, 0.3);
        animation: pulse-ring 2s infinite;
      }
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

@keyframes pulse-red {
  0% { background-color: #dc3545; }
  50% { background-color: #ff6b7a; }
  100% { background-color: #dc3545; }
}

@keyframes pulse-ring {
  0% { transform: scale(1); opacity: 0.8; }
  100% { transform: scale(1.4); opacity: 0; }
}

.video-chat {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  backdrop-filter: blur(10px);

  .ai-avatar {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;

    .avatar {
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background: linear-gradient(45deg, #ff9a9e, #fecfef, #fecfef);
      margin: auto;
      box-shadow: 0 10px 30px rgba(0,0,0,0.3);
      transition: transform 0.3s ease;

      &.pulsing {
        animation: avatar-pulse 2s infinite;
      }
    }

    .character-name {
      color: white;
      font-size: 0.24rem;
      font-weight: 500;
      margin-top: 0.3rem;
      text-shadow: 0 2px 4px rgba(0,0,0,0.3);
    }

    .stt-display {
      margin-top: 0.4rem;
      background: rgba(255, 255, 255, 0.1);
      padding: 0.2rem 0.3rem;
      border-radius: 0.2rem;
      backdrop-filter: blur(10px);
      max-width: 80%;

      .stt-label {
        color: rgba(255, 255, 255, 0.8);
        font-size: 0.14rem;
        margin-bottom: 0.1rem;
      }

      .stt-text {
        color: white;
        font-size: 0.18rem;
        font-weight: 500;
      }
    }
  }

  .control {
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 1rem auto;
    gap: 0.8rem;

    .control-item {
      width: 1rem;
      height: 1rem;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      cursor: pointer;
      transition: all 0.3s;
      backdrop-filter: blur(10px);
      border: 2px solid rgba(255, 255, 255, 0.2);

      &.mic {
        background: rgba(40, 167, 69, 0.8);

        &.active {
          background: rgba(40, 167, 69, 1);
          transform: scale(1.1);
          box-shadow: 0 0 20px rgba(40, 167, 69, 0.5);
        }

        &.muted {
          background: rgba(108, 117, 125, 0.8);
        }

        &:hover {
          transform: scale(1.05);
        }
      }

      &.close {
        background: rgba(220, 53, 69, 0.8);

        &:hover {
          background: rgba(220, 53, 69, 1);
          transform: scale(1.05);
        }
      }

      :deep(.el-icon) {
        font-size: 0.5rem;
        color: white;
        svg {
          font-size: 0.5rem;
        }
      }
    }
  }
}

@keyframes avatar-pulse {
  0% {
    transform: scale(1);
    box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 15px 40px rgba(255, 154, 158, 0.4);
  }
  100% {
    transform: scale(1);
    box-shadow: 0 10px 30px rgba(0,0,0,0.3);
  }
}
</style>
