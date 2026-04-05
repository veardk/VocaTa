<template>
  <div>
    <div :class="isM ? 'mobile' : 'pc'" class="main-container">
      <!-- 连接状态提示 -->
      <div v-if="!isAIConnected" class="connection-status">
        <el-icon class="loading-icon"><Loading /></el-icon>
        <span>{{ connectionStatus }}</span>
      </div>

      <div class="chat-container" ref="chatContainer">
        <div class="chat-item" v-for="(item, index) in chats" :key="index">
          <div v-if="item.type == 'receive'" class="receive">
            <div class="avatar">
              <img
                v-if="characterAvatar"
                :src="characterAvatar"
                :alt="getCharacterName()"
              />
              <div v-else class="avatar-placeholder">{{ characterInitials }}</div>
            </div>
            <div class="right">
              <div class="content" :class="{ 'streaming': item.isStreaming }">
                <span class="text-content">{{ item.content }}</span>
                <span v-if="item.isStreaming" class="typing-cursor">|</span>
              </div>
              <div v-if="item.createDate" class="time">{{ formatTime(item.createDate) }}</div>
            </div>
          </div>
          <div v-else-if="item.type == 'send'" class="send">
            <div class="left">
              <div class="content" :class="{ 'recognizing': item.isRecognizing }">
                <span class="text-content">{{ item.content }}</span>
                <span v-if="item.isRecognizing" class="recognition-tip">(识别中...)</span>
              </div>
              <div v-if="item.createDate" class="time">{{ formatTime(item.createDate) }}</div>
            </div>
            <div class="avatar">
              <img
                v-if="userAvatar"
                :src="userAvatar"
                :alt="userDisplayName"
              />
              <div v-else class="avatar-placeholder">{{ userInitials }}</div>
            </div>
          </div>
        </div>

        <!-- 加载指示器 -->
        <div v-if="isAIThinking" class="ai-thinking">
          <div class="avatar">
            <img
              v-if="characterAvatar"
              :src="characterAvatar"
              :alt="getCharacterName()"
            />
            <div v-else class="avatar-placeholder">{{ characterInitials }}</div>
          </div>
          <div class="thinking-content">
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
            :placeholder="isAIConnected ? '输入消息或点击通话按钮开始语音对话...' : '连接中，请稍等...'"
            @keydown.enter.prevent="sendMessage"
            :disabled="!isAIConnected"
            resize="none"
            class="chat-input"
          ></el-input>
          <button class="send-btn" @click="sendMessage" :disabled="!isAIConnected || !input.trim()">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
        <button
          class="phone"
          :class="{ active: isAudioCallActive, recording: aiChat?.recording }"
          @click="toggleAudioCall"
          :disabled="!isAIConnected"
          :title="isAudioCallActive ? '点击挂断通话' : '点击开始语音通话'"
        >
          <el-icon v-if="!isAudioCallActive"><PhoneFilled /></el-icon>
          <el-icon v-else><Close /></el-icon>
        </button>
      </div>

      <!-- 极简语音界面 -->
      <div class="voice-minimal" v-if="isAudioCallActive">
        <div class="voice-minimal__avatar" :class="{
          'is-speaking': isAISpeaking,
          'is-recording': aiChat?.recording
        }">
          <div class="voice-minimal__pulse"></div>
          <img v-if="characterAvatar" :src="characterAvatar" :alt="getCharacterName()" />
          <span v-else>{{ characterInitials }}</span>
        </div>

        <div class="voice-minimal__status">{{ voiceStatusText }}</div>

        <div class="voice-minimal__transcripts">
          <div v-if="!visibleVoiceTranscripts.length" class="voice-minimal__transcripts-empty">
            暂无语音内容，点击开始说话或等待 AI 回复。
          </div>
          <div v-else class="voice-minimal__transcripts-list">
            <div
              v-for="entry in visibleVoiceTranscripts"
              :key="entry.timestamp"
              class="voice-minimal__transcript-item"
              :class="entry.speaker === 'user' ? 'is-user' : 'is-ai'"
            >
              <span class="voice-minimal__transcript-speaker">
                {{ entry.speaker === 'user' ? '我' : getCharacterName() }}：
              </span>
              <span class="voice-minimal__transcript-text">{{ entry.text }}</span>
            </div>
          </div>
        </div>

        <div class="voice-minimal__controls">
          <button
            class="voice-minimal__control is-mic"
            :class="{ 'is-recording': aiChat?.recording }"
            @click="toggleMicrophone"
            :disabled="!isAIConnected"
          >
            <el-icon><Microphone /></el-icon>
          </button>
          <button class="voice-minimal__control is-cancel" @click="stopAudioCall">
            <el-icon><Close /></el-icon>
          </button>
        </div>

        <div class="voice-minimal__hint">
          提示：点击录音按钮即可说话，当说完话时再次点击即可结束录音，等待 AI 回答
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { conversationApi } from '@/api/modules/conversation'
import { userApi } from '@/api/modules/user'
import { isMobile } from '@/utils/isMobile'
import { ElMessage } from 'element-plus'
import { computed, onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { ChatMessage } from '@/types/common'
import type { ConversationResponse, MessageResponse } from '@/types/api'
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
const currentConversation = ref<ConversationResponse | null>(null)

const userAvatar = ref('')
const userNickname = ref('')
const userDisplayName = computed(() => userNickname.value || '我')
const userInitials = computed(() => {
  const name = userDisplayName.value
  if (!name) return '我'
  return name.slice(0, 1).toUpperCase()
})

// AI对话相关状态
const aiChat = ref<VocaTaAIChat | null>(null)
const isAudioCallActive = ref(false)
const connectionStatus = ref('正在连接...')
const isAIConnected = ref(false) // 新增：追踪连接状态
const isAIThinking = ref(false)
const currentSTTText = ref('')
const currentStreamingMessage = ref<ChatMessage | null>(null)
const isAISpeaking = ref(false)

interface VoiceTranscriptEntry {
  speaker: 'user' | 'ai'
  text: string
  timestamp: number
}

const voiceTranscripts = ref<VoiceTranscriptEntry[]>([])
const hasShownGreeting = ref(false)

interface TypewriterState {
  message: ChatMessage
  targetText: string
  currentIndex: number
  intervalId: number | null
  isComplete: boolean
  started: boolean
  fallbackTimeoutId: number | null
}

const typewriterState = ref<TypewriterState | null>(null)
const TYPEWRITER_SPEED = 35

// VAD相关状态
const vadActive = ref(false)
const vadCheckInterval = ref<number | null>(null)


// 引用
const chatContainer = ref<HTMLElement>()

onMounted(async () => {
  await loadUserProfile()

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
  resetTypewriterState()
  // 清理AI对话系统资源
  if (aiChat.value) {
    aiChat.value.destroy()
  }
})

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
      isAIConnected.value = false // 重置连接状态
      currentSTTText.value = ''
      isAIThinking.value = false
      resetTypewriterState()
      currentStreamingMessage.value = null
      isAISpeaking.value = false
      voiceTranscripts.value = []
      hasShownGreeting.value = false

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
const loadUserProfile = async () => {
  try {
    const res = await userApi.getUserInfo()
    if (res.code === 200 && res.data) {
      userAvatar.value = res.data.avatar || ''
      userNickname.value = res.data.nickname || ''
    }
  } catch (error) {
    console.error('❌ 获取用户信息失败:', error)
  }
}

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

      if (messages.length > 0) {
        hasShownGreeting.value = true
      } else if (!hasShownGreeting.value) {
        const greetingText = currentConversation.value?.greeting?.trim()
        if (greetingText) {
          hasShownGreeting.value = true
          chats.value = [{
            type: 'receive',
            content: greetingText,
            senderType: 2,
            contentType: 1,
            createDate: new Date().toISOString(),
            characterName: currentConversation.value?.characterName || getCharacterName(),
            metadata: { isGreeting: true }
          }]
        }
      }

      console.log('📥 加载消息完成:', {
        messagesCount: messages.length,
        chatsLength: chats.value.length,
        hasMessages: chats.value.length > 0
      })

      // 修复: 无论是否有历史消息，都要确保滚动到底部
      // 使用延迟确保DOM完全渲染
      setTimeout(() => {
        scrollToBottomWithRetry()
      }, 100)
    }
  } catch (error) {
    console.error('加载消息失败:', error)
    ElMessage.error('加载消息失败')
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
  if (!input.value.trim() || !conversationUuid.value || !isAIConnected.value) return

  const messageContent = input.value.trim()
  input.value = ''

  if (aiChat.value) {
    try {
      await aiChat.value.prepareAudioPlayback()
    } catch (error) {
      console.warn('⚠️ 准备音频播放失败:', error)
    }
  }

  // 立即在界面上显示用户消息
  const userMessage: ChatMessage = {
    type: 'send',
    content: messageContent,
    senderType: 1,
    contentType: 1,
    createDate: new Date().toISOString()
  }
  chats.value.push(userMessage)
  scrollToBottomWithRetry()

  // 显示AI思考状态
  isAIThinking.value = true

  try {
    // 通过WebSocket发送消息给AI
    aiChat.value?.sendTextMessage(messageContent)
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
    isAIThinking.value = false

    // 发送失败时移除用户消息
    chats.value.pop()
    input.value = messageContent // 恢复输入内容
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
      const conversation = res.data.find((conv) => conv.conversationUuid === conversationUuid.value)

      if (!conversation) {
        console.warn('⚠️ 在对话列表中找不到当前对话UUID:', conversationUuid.value)
        console.log('📋 可用的对话列表:', res.data.map((c) => ({
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

const characterAvatar = computed(() => currentConversation.value?.characterAvatarUrl || '')

const characterInitials = computed(() => {
  const name = getCharacterName()
  if (!name) return 'AI'
  return name.slice(0, 2).toUpperCase()
})

const voiceStatusText = computed(() => {
  if (!isAIConnected.value) return '语音通道连接中…'
  if (aiChat.value?.recording) {
    return vadActive.value ? '正在收音…' : '准备开始说话'
  }
  if (isAISpeaking.value) return 'AI 正在回答'
  if (isAIThinking.value) return 'AI 正在思考…'
  return '点击下方按钮开启语音对话'
})

const visibleVoiceTranscripts = computed(() => voiceTranscripts.value.slice(-6))

// 初始化AI对话系统
const initializeAIChat = async () => {
  console.log('🔥 开始初始化AI对话系统...')
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
    console.log('🔐 Token存在，长度:', token.length)

    connectionStatus.value = '正在连接AI系统...'

    // 创建AI对话实例
    console.log('📦 创建VocaTaAIChat实例')
    aiChat.value = new VocaTaAIChat()

    // 设置回调函数
    console.log('🔗 设置AI对话回调函数')
    setupAIChatCallbacks()

    // 初始化AI对话系统
    console.log('⚡ 开始初始化AI对话系统，等待WebSocket连接...')
    await aiChat.value.initialize(conversationUuid.value)

    console.log('✅ AI对话系统初始化完成！')
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
  aiChat.value.onConnectionStatus((status) => {
    switch (status) {
      case 'connected':
        connectionStatus.value = '已连接到AI服务'
        isAIConnected.value = true // 更新连接状态
        break
      case 'disconnected':
        connectionStatus.value = '连接已断开，正在重连...'
        isAIConnected.value = false
        break
      case 'error':
        connectionStatus.value = '连接失败'
        isAIConnected.value = false
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
      voiceTranscripts.value.push({
        speaker: 'user',
        text,
        timestamp: Date.now()
      })
      if (voiceTranscripts.value.length > 12) {
        voiceTranscripts.value.splice(0, voiceTranscripts.value.length - 12)
      }
      scrollToBottomWithRetry()

      // 显示AI思考状态
      isAIThinking.value = true
      currentSTTText.value = '' // 清空显示
    }
  })

  // LLM流式文本回调
  aiChat.value.onLLMStream((text, isComplete, characterName) => {
    console.log('🤖 收到LLM流式消息:', {
      text: text?.substring(0, 50),
      textLength: text?.length,
      isComplete,
      characterName,
      currentStreamingExists: !!currentStreamingMessage.value
    })

    const content = text ?? ''
    const trimmed = content.trim()
    const previousState = typewriterState.value
    const wasComplete = previousState?.isComplete ?? false

    if (!trimmed) {
      if (isComplete) {
        isAIThinking.value = false
        if (previousState) {
          previousState.isComplete = true
          if (!previousState.started) {
            scheduleTypewriterFallback()
          }
        } else if (currentStreamingMessage.value) {
          currentStreamingMessage.value.isStreaming = false
          currentStreamingMessage.value = null
        }
      }
      return
    }

    let state = typewriterState.value

    if (!currentStreamingMessage.value) {
      const newMessage: ChatMessage = {
        type: 'receive',
        content: '',
        senderType: 2,
        contentType: 1,
        createDate: new Date().toISOString(),
        isStreaming: true,
        characterName: characterName || getCharacterName()
      }

      chats.value.push(newMessage)
      currentStreamingMessage.value = newMessage
      state = {
        message: newMessage,
        targetText: content,
        currentIndex: 0,
        intervalId: null,
        isComplete,
        started: false,
        fallbackTimeoutId: null
      }
      typewriterState.value = state
      isAIThinking.value = false
      scrollToBottomWithRetry()
    } else {
      currentStreamingMessage.value.isStreaming = true
      if (characterName) {
        currentStreamingMessage.value.characterName = characterName
      }

      if (!state || state.message !== currentStreamingMessage.value) {
        state = {
          message: currentStreamingMessage.value,
          targetText: content,
          currentIndex: currentStreamingMessage.value.content.length,
          intervalId: state?.intervalId ?? null,
          isComplete: state?.isComplete ?? false,
          started: state?.started ?? false,
          fallbackTimeoutId: state?.fallbackTimeoutId ?? null
        }
        typewriterState.value = state
      }
    }

    state = typewriterState.value
    if (!state) return

    state.targetText = content
    state.isComplete = isComplete
    state.currentIndex = Math.min(state.currentIndex, state.targetText.length)
    if (!state.started) {
      state.message.content = state.targetText.slice(0, state.currentIndex)
    }
    state.message.isStreaming = true

    if (isComplete && !wasComplete && trimmed) {
      voiceTranscripts.value.push({
        speaker: 'ai',
        text: trimmed,
        timestamp: Date.now()
      })
      if (voiceTranscripts.value.length > 12) {
        voiceTranscripts.value.splice(0, voiceTranscripts.value.length - 12)
      }
    }

    if (isAISpeaking.value && !state.started) {
      startTypewriterEffect()
    }

    if (isComplete) {
      scheduleTypewriterFallback()
    }
  })

  // 音频播放状态回调
  aiChat.value.onAudioPlay((isPlaying) => {
    console.log('🔊 音频播放状态:', isPlaying)
    isAISpeaking.value = isPlaying

    if (isPlaying) {
      startTypewriterEffect()
    } else if (typewriterState.value) {
      if (!typewriterState.value.started) {
        scheduleTypewriterFallback()
      } else if (typewriterState.value.isComplete) {
        finalizeTypewriter()
      }
    }
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
    await aiChat.value.prepareAudioPlayback()
    await aiChat.value.startAudioCall()
    isAudioCallActive.value = true
    voiceTranscripts.value = []

    // 启动VAD状态监控
    startVADMonitoring()

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
    isAISpeaking.value = false

    // 停止VAD监控
    stopVADMonitoring()

  } catch (error) {
    console.error('❌ 停止音频通话失败:', error)
  }
}

const toggleMicrophone = async () => {
  if (!aiChat.value || !isAudioCallActive.value) return

  try {
    if (aiChat.value.recording) {
      // 当前在录音，停止录音
      console.log('🛑 停止录音')
      aiChat.value.stopRecording()
    } else {
      // 当前没有录音，开始录音
      console.log('🎤 开始录音')
      aiChat.value.startRecording()
    }
  } catch (error) {
    console.error('❌ 切换麦克风状态失败:', error)
    ElMessage.error('切换麦克风状态失败')
  }
}

const clearTypewriterTimers = () => {
  const state = typewriterState.value
  if (!state) return

  if (state.intervalId !== null) {
    window.clearInterval(state.intervalId)
    state.intervalId = null
  }

  if (state.fallbackTimeoutId !== null) {
    window.clearTimeout(state.fallbackTimeoutId)
    state.fallbackTimeoutId = null
  }
}

const finalizeTypewriter = () => {
  const state = typewriterState.value
  if (!state) return

  clearTypewriterTimers()
  state.message.content = state.targetText
  state.message.isStreaming = false
  isAIThinking.value = false
  typewriterState.value = null
  currentStreamingMessage.value = null
  scrollToBottomWithRetry()
}

const startTypewriterEffect = () => {
  const state = typewriterState.value
  if (!state || state.started) return

  clearTypewriterTimers()
  state.started = true
  state.message.isStreaming = true
  state.intervalId = window.setInterval(() => {
    const targetLength = state.targetText.length
    if (state.currentIndex < targetLength) {
      state.currentIndex += 1
      state.message.content = state.targetText.slice(0, state.currentIndex)
      scrollToBottomWithRetry()
    } else if (state.isComplete) {
      finalizeTypewriter()
    }
  }, TYPEWRITER_SPEED)
}

const scheduleTypewriterFallback = () => {
  const state = typewriterState.value
  if (!state || state.started) return

  if (state.fallbackTimeoutId !== null) return

  state.fallbackTimeoutId = window.setTimeout(() => {
    state.fallbackTimeoutId = null
    startTypewriterEffect()
  }, 500)
}

const resetTypewriterState = () => {
  const state = typewriterState.value
  if (!state) return

  clearTypewriterTimers()
  state.message.isStreaming = false
  isAIThinking.value = false
  typewriterState.value = null
  currentStreamingMessage.value = null
}

// 修复: 强化版滚动到底部函数，带重试机制
const scrollToBottomWithRetry = (maxRetries: number = 3) => {
  let retries = 0
  
  const attempt = () => {
    try {
      if (chatContainer.value) {
        const container = chatContainer.value
        const isScrollable = container.scrollHeight > container.clientHeight
        
        console.log('📜 滚动信息:', {
          scrollHeight: container.scrollHeight,
          clientHeight: container.clientHeight,
          scrollTop: container.scrollTop,
          isScrollable,
          retries
        })
        
        if (isScrollable) {
          // 使用smooth滚动，确保用户能看到滚动效果
          container.scrollTo({
            top: container.scrollHeight,
            behavior: 'smooth'
          })
          
          // 备用方案：直接设置scrollTop
          container.scrollTop = container.scrollHeight
          
          console.log('✅ 滚动完成，新scrollTop:', container.scrollTop)
        } else {
          console.log('📜 容器不需要滚动')
        }
      } else {
        console.warn('⚠️ chatContainer引用为空')
      }
    } catch (error) {
      console.error('❌ 滚动操作失败:', error)
    }
  }
  
  const tryScroll = () => {
    attempt()
    
    // 检查是否需要重试
    if (retries < maxRetries) {
      retries++
      setTimeout(() => {
        if (chatContainer.value) {
          const isAtBottom = Math.abs(
            chatContainer.value.scrollHeight - 
            chatContainer.value.scrollTop - 
            chatContainer.value.clientHeight
          ) <= 5
          
          if (!isAtBottom) {
            console.log(`🔄 滚动重试 ${retries}/${maxRetries}`)
            tryScroll()
          }
        }
      }, 100)
    }
  }
  
  // 使用nextTick确保DOM更新后再滚动
  nextTick(() => {
    tryScroll()
  })
}

// VAD监控相关函数
const startVADMonitoring = () => {
  if (vadCheckInterval.value) {
    clearInterval(vadCheckInterval.value)
  }

  vadCheckInterval.value = window.setInterval(() => {
    vadActive.value = aiChat.value?.voiceActive ?? false
  }, 100) // 每100ms检查一次VAD状态
}

const stopVADMonitoring = () => {
  if (vadCheckInterval.value) {
    clearInterval(vadCheckInterval.value)
    vadCheckInterval.value = null
  }
  vadActive.value = false
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
  /* 修复: 确保滚动容器有正确的样式 */
  scroll-behavior: smooth;
  
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
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .avatar-placeholder {
        font-size: 0.2rem;
        font-weight: 600;
        color: #4a5568;
      }
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
      display: inline-flex;
      align-items: baseline;

      .text-content {
        white-space: pre-wrap;
      }

      // 流式文本显示效果
      &.streaming {
        .typing-cursor {
          animation: typewriter-blink 1.2s infinite;
          font-weight: bold;
          color: #007bff;
          margin-left: 0.02rem;
        }

        .text-content {
          animation: typewriter-appear 0.1s ease-out;
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
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    .avatar-placeholder {
      font-size: 0.2rem;
      font-weight: 600;
      color: #4a5568;
    }
  }

  .thinking-content {
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

@keyframes typewriter-blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

@keyframes typewriter-appear {
  from { opacity: 0.8; }
  to { opacity: 1; }
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

// ChatGPT风格语音通话界面样式 - 修复尺寸问题
.chatgpt-voice-chat {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #2D2D2D 0%, #1A1A1A 100%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  color: white;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;

  // 顶部状态栏
  .voice-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(10px);
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    height: 60px;
    flex-shrink: 0;

    .connection-indicator {
      display: flex;
      align-items: center;
      gap: 8px;

      .status-dot {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #ff6b6b;
        transition: background-color 0.3s;

        &.connected {
          background: #10B981;
        }
      }

      .status-text {
        font-size: 14px;
        opacity: 0.9;
        font-weight: 500;
      }
    }

    .close-btn {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.1);
      border: none;
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        transform: scale(1.1);
      }

      :deep(.el-icon) {
        font-size: 16px;
      }
    }
  }

  // 中央对话区域 - 适配屏幕尺寸
  .voice-conversation-area {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 20px;
    position: relative;
    min-height: 0; // 允许flex收缩

    // AI部分 - 相对定位，适应屏幕
    .ai-section {
      position: absolute;
      top: 10%;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .ai-avatar-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }

    // AI头像样式 - 缩小尺寸适配屏幕
    .ai-avatar {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #10B981 0%, #059669 100%);
      box-shadow: 0 8px 24px rgba(16, 185, 129, 0.4);
      transition: all 0.3s ease;

      .avatar-inner {
        width: 80px;
        height: 80px;
        border-radius: 50%;
        background: linear-gradient(135deg, #1F2937 0%, #374151 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 24px;
        font-weight: bold;
        color: white;

        &::before {
          content: "🤖";
          font-size: 32px;
        }
      }

      &.speaking {
        animation: ai-speaking 2s infinite;
        box-shadow: 0 8px 32px rgba(16, 185, 129, 0.6);
      }

      &.thinking {
        animation: ai-thinking 1.5s infinite alternate;
      }

      .voice-waves {
        position: absolute;
        width: 100%;
        height: 100%;
        top: 0;
        left: 0;

        .wave {
          position: absolute;
          top: 50%;
          left: 50%;
          width: 120%;
          height: 120%;
          border: 2px solid rgba(16, 185, 129, 0.4);
          border-radius: 50%;
          transform: translate(-50%, -50%);
          animation: voice-wave 1.5s infinite;

          &:nth-child(2) {
            animation-delay: 0.3s;
            width: 140%;
            height: 140%;
          }

          &:nth-child(3) {
            animation-delay: 0.6s;
            width: 160%;
            height: 160%;
          }
        }
      }
    }

    .ai-name {
      font-size: 16px;
      font-weight: 600;
      margin-top: 8px;
    }

    .ai-status {
      font-size: 12px;
      opacity: 0.7;
      margin-top: 4px;
    }

    // 用户部分 - 相对定位，适应屏幕
    .user-section {
      position: absolute;
      bottom: 20%;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .user-avatar-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
    }

    // 用户头像样式 - 缩小尺寸适配屏幕
    .user-avatar {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%);
      box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
      transition: all 0.3s ease;

      .avatar-inner {
        width: 64px;
        height: 64px;
        border-radius: 50%;
        background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 16px;
        color: white;

        &::before {
          content: "👨‍💻";
          font-size: 24px;
        }
      }

      &.listening {
        animation: user-listening 1s infinite alternate;
      }

      &.voice_active {
        animation: voice-active-pulse 0.8s infinite;
        box-shadow: 0 0 20px rgba(59, 130, 246, 0.8);
      }

      .vad-indicator {
        position: absolute;
        width: 120%;
        height: 120%;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);

        .vad-ring {
          position: absolute;
          width: 100%;
          height: 100%;
          border: 2px solid rgba(59, 130, 246, 0.6);
          border-radius: 50%;
          animation: vad-ring-pulse 1s infinite;
        }

        .vad-pulse {
          position: absolute;
          width: 100%;
          height: 100%;
          background: radial-gradient(circle, rgba(59, 130, 246, 0.3) 0%, transparent 70%);
          border-radius: 50%;
          animation: vad-pulse-effect 1s infinite;
        }
      }

      .mic-icon {
        position: absolute;
        bottom: -4px;
        right: -4px;
        width: 24px;
        height: 24px;
        background: #10B981;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 2px 8px rgba(16, 185, 129, 0.4);

        :deep(.el-icon) {
          font-size: 12px;
          color: white;
        }
      }
    }

    .user-name {
      font-size: 16px;
      font-weight: 600;
      margin-top: 8px;
    }

    .user-status {
      font-size: 12px;
      opacity: 0.7;
      margin-top: 4px;
    }
  }

  // STT实时显示 - 适配屏幕尺寸
  .stt-live-display {
    position: absolute;
    bottom: 35%;
    left: 50%;
    transform: translateX(-50%);
    background: rgba(0, 0, 0, 0.6);
    backdrop-filter: blur(20px);
    border-radius: 12px;
    padding: 12px 16px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    max-width: 80%;
    text-align: center;

    .stt-content {
      .stt-label {
        font-size: 10px;
        opacity: 0.7;
        margin-bottom: 4px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .stt-text {
        font-size: 14px;
        font-weight: 500;
        line-height: 1.4;
      }
    }
  }

  // 底部控制区域 - 固定高度
  .voice-controls {
    padding: 20px;
    display: flex;
    justify-content: center;
    background: rgba(0, 0, 0, 0.1);
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    height: 80px;
    flex-shrink: 0;

    .voice-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 12px;
      padding: 12px 20px;
      color: white;
      cursor: pointer;
      transition: all 0.3s;
      backdrop-filter: blur(10px);

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        transform: translateY(-2px);
      }

      &.active {
        background: rgba(16, 185, 129, 0.3);
        border-color: #10B981;
      }

      &.voice_active {
        background: rgba(59, 130, 246, 0.3);
        border-color: #3B82F6;
        animation: voice-control-pulse 1s infinite;
      }

      &.disabled {
        opacity: 0.5;
        cursor: not-allowed;

        &:hover {
          transform: none;
        }
      }

      .btn-icon {
        :deep(.el-icon) {
          font-size: 20px;
        }
      }

      .btn-text {
        font-size: 12px;
        font-weight: 500;
      }
    }
  }
}

// 动画定义 - ChatGPT风格
@keyframes ai-speaking {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 8px 32px rgba(16, 185, 129, 0.3);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 8px 32px rgba(16, 185, 129, 0.6);
  }
}

@keyframes ai-thinking {
  0% {
    transform: scale(1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1.03);
    opacity: 1;
  }
}

@keyframes user-listening {
  0% {
    transform: scale(1);
    box-shadow: 0 8px 32px rgba(59, 130, 246, 0.3);
  }
  100% {
    transform: scale(1.02);
    box-shadow: 0 8px 32px rgba(59, 130, 246, 0.5);
  }
}

@keyframes voice-active-pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 24px rgba(59, 130, 246, 0.8);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 32px rgba(59, 130, 246, 1);
  }
}

@keyframes voice-wave {
  0% {
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.6;
  }
  100% {
    transform: translate(-50%, -50%) scale(1.5);
    opacity: 0;
  }
}

@keyframes vad-ring-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.7;
  }
}

@keyframes vad-pulse-effect {
  0% {
    transform: scale(1);
    opacity: 0.6;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

@keyframes voice-control-pulse {
  0%, 100% {
    transform: scale(1) translateY(-2px);
    box-shadow: 0 4px 16px rgba(59, 130, 246, 0.4);
  }
  50% {
    transform: scale(1.05) translateY(-4px);
    box-shadow: 0 8px 24px rgba(59, 130, 246, 0.6);
  }
}

@keyframes mic-active-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.voice-minimal {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: space-between;
  padding: 12vh 0 8vh;
  background: linear-gradient(150deg, #fff5f7 0%, #ffeef3 100%);
  color: #475569;
  text-align: center;
  z-index: 1000;

  &__avatar {
    position: relative;
    width: 170px;
    height: 170px;
    border-radius: 50%;
    overflow: hidden;
    background: rgba(255, 255, 255, 0.65);
    box-shadow: 0 20px 48px rgba(148, 163, 184, 0.3);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.3s ease;

    .voice-minimal__pulse {
      position: absolute;
      inset: -28px;
      border-radius: 50%;
      background: radial-gradient(circle, rgba(255, 255, 255, 0.45) 0%, rgba(255, 255, 255, 0) 70%);
      opacity: 0;
      pointer-events: none;
    }

    &.is-speaking .voice-minimal__pulse {
      animation: voice-minimal-pulse 1.9s ease-out infinite;
      opacity: 1;
    }

    &.is-recording {
      animation: voice-minimal-record 1.4s ease-in-out infinite;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 50%;
    }

    span {
      font-size: 32px;
      font-weight: 600;
      color: rgba(148, 163, 184, 0.6);
    }
  }

  &__status {
    margin-top: 4vh;
    font-size: 16px;
    font-weight: 500;
  }

  &__transcripts {
    margin-top: 24px;
    width: min(520px, 80%);
    max-height: 220px;
    overflow-y: auto;
    text-align: left;
  }

  &__transcripts-list {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  &__transcript-item {
    padding: 12px 16px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 8px 20px rgba(148, 163, 184, 0.15);
    font-size: 14px;
    line-height: 1.6;
    display: flex;
    align-items: baseline;
    gap: 6px;

    &.is-user {
      background: rgba(59, 130, 246, 0.12);
      color: #1d4ed8;
    }

    &.is-ai {
      background: rgba(16, 185, 129, 0.12);
      color: #047857;
    }
  }

  &__transcript-speaker {
    font-weight: 600;
  }

  &__transcript-text {
    flex: 1;
    color: inherit;
  }

  &__transcripts-empty {
    padding: 16px;
    border-radius: 16px;
    background: rgba(255, 255, 255, 0.65);
    font-size: 14px;
    color: rgba(71, 85, 105, 0.75);
    text-align: center;
  }

  &__controls {
    display: flex;
    gap: 28px;
  }

  &__control {
    width: 76px;
    height: 76px;
    border-radius: 50%;
    border: none;
    background: rgba(255, 255, 255, 0.95);
    box-shadow: 0 14px 32px rgba(15, 23, 42, 0.12);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #111827;
    cursor: pointer;
    transition: transform 0.2s ease, box-shadow 0.2s ease;

    &:hover:not(:disabled) {
      transform: translateY(-4px);
      box-shadow: 0 16px 34px rgba(15, 23, 42, 0.16);
    }

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
      transform: none;
      box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
    }

    :deep(.el-icon) {
      font-size: 38px;
      color: currentColor;
      display: flex;
    }
  }

  &__control.is-mic.is-recording {
    color: #dc2626;
    background: rgba(255, 243, 241, 0.96);
  }

  &__control.is-cancel {
    color: #ef4444;
  }

  &__hint {
    font-size: 13px;
    color: rgba(71, 85, 105, 0.75);
    letter-spacing: 0.02em;
    padding: 0 24px;
  }
}

@keyframes voice-minimal-pulse {
  0% {
    transform: scale(0.9);
    opacity: 0.55;
  }
  60% {
    transform: scale(1.2);
    opacity: 0;
  }
  100% {
    transform: scale(1.3);
    opacity: 0;
  }
}

@keyframes voice-minimal-record {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 24px 40px rgba(248, 113, 113, 0.25);
  }
  50% {
    transform: scale(1.04);
    box-shadow: 0 24px 42px rgba(248, 113, 113, 0.35);
  }
}

@media screen and (max-width: 768px) {
  .voice-minimal {
    padding: 14vh 0 12vh;

    &__avatar {
      width: 140px;
      height: 140px;
    }

    &__controls {
      gap: 20px;
    }

    &__control {
      width: 64px;
      height: 64px;

      :deep(.el-icon) {
        font-size: 32px;
      }
    }

    &__transcripts {
      max-height: 180px;
    }
  }
}
</style>
