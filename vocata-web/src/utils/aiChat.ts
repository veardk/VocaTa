/**
 * VocaTa AI对话系统 - WebSocket客户端和音频管理器
 * 基于文档 VocaTa-AI对话完整对接文档.md 实现
 */

import { getToken } from './token'

// WebSocket消息类型定义
interface WebSocketMessage {
  type: string
  [key: string]: any
}

interface STTResultMessage extends WebSocketMessage {
  type: 'stt_result'
  text: string
  isFinal: boolean
  confidence: number
  timestamp: number
}

interface LLMTextStreamMessage extends WebSocketMessage {
  type: 'llm_text_stream'
  text: string
  characterName: string
  isComplete: boolean
  timestamp: number
}

interface TTSAudioMetaMessage extends WebSocketMessage {
  type: 'tts_audio_meta'
  audioSize: number
  format: string
  sampleRate: number
  channels: number
  bitDepth: number
  timestamp: number
}

interface CompleteMessage extends WebSocketMessage {
  type: 'complete'
  message: string
  timestamp: number
}

interface ErrorMessage extends WebSocketMessage {
  type: 'error'
  error: string
  timestamp: number
}

// WebSocket客户端类
export class VocaTaWebSocketClient {
  private ws: WebSocket | null = null
  private conversationUuid: string
  private reconnectAttempts = 0
  private readonly maxReconnectAttempts = 5
  private callbacks: Map<string, Function[]> = new Map()

  constructor(conversationUuid: string) {
    this.conversationUuid = conversationUuid
  }

  connect(): void {
    console.log('🔄 开始建立WebSocket连接，conversationUuid:', this.conversationUuid)

    const token = getToken()
    if (!token) {
      console.error('❌ 未找到认证令牌，无法建立WebSocket连接')
      this.emit('error', new Error('认证令牌未找到'))
      return
    }

    const wsUrl = `ws://${import.meta.env.VITE_APP_URL.replace('http://', '')}/ws/chat/${this.conversationUuid}?token=${encodeURIComponent(token)}`
    console.log('🔌 尝试连接WebSocket:', wsUrl)
    console.log('🔐 使用Token:', token.substring(0, 20) + '...')

    try {
      this.ws = new WebSocket(wsUrl)
      this.setupEventHandlers()
    } catch (error) {
      console.error('❌ WebSocket连接创建失败:', error)
      this.emit('error', error)
    }
  }

  private setupEventHandlers(): void {
    if (!this.ws) return

    this.ws.onopen = (event) => {
      console.log('✅ WebSocket连接已建立')
      console.log('🔍 WebSocket状态检查:', {
        readyState: this.ws?.readyState,
        isOpen: this.ws?.readyState === WebSocket.OPEN,
        WebSocketOPEN: WebSocket.OPEN
      })
      this.reconnectAttempts = 0
      this.emit('connected', event)
    }

    this.ws.onmessage = (event) => {
      // 检查是否为二进制音频数据
      if (event.data instanceof ArrayBuffer) {
        console.log(`📦 收到音频数据(ArrayBuffer): ${event.data.byteLength} bytes`)
        this.emit('audioData', event.data)
        return
      }

      // 检查是否为Blob音频数据
      if (event.data instanceof Blob) {
        console.log(`📦 收到音频数据(Blob): ${event.data.size} bytes`)
        // 将Blob转换为ArrayBuffer
        event.data.arrayBuffer().then(arrayBuffer => {
          this.emit('audioData', arrayBuffer)
        }).catch(error => {
          console.error('❌ Blob转ArrayBuffer失败:', error)
        })
        return
      }

      // 否则按JSON消息处理
      try {
        const message: WebSocketMessage = JSON.parse(event.data)
        console.log(`📨 收到消息:`, message)
        this.emit('message', message)
      } catch (e) {
        console.error('❌ 解析消息失败:', event.data)
      }
    }

    this.ws.onclose = (event) => {
      console.log(`🔌 WebSocket连接关闭: code=${event.code}, reason="${event.reason}", wasClean=${event.wasClean}`)
      this.emit('disconnected', event)
      this.attemptReconnect()
    }

    this.ws.onerror = (error) => {
      console.error('❌ WebSocket错误:', error)
      console.error('WebSocket readyState:', this.ws?.readyState)
      this.emit('error', error)
    }
  }

  // 发送文字消息
  sendTextMessage(text: string): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      console.error('❌ WebSocket未连接')
      return
    }

    const message = {
      type: 'text_message',
      data: { message: text }
    }

    console.log('📤 发送文字消息:', text)
    this.ws.send(JSON.stringify(message))
  }

  // 发送音频数据
  sendAudioData(audioBuffer: ArrayBuffer): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    this.ws.send(audioBuffer)
  }

  // 音频录制控制
  startAudioRecording(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    console.log('🎤 发送开始录音信号')
    this.ws.send(JSON.stringify({ type: 'audio_start' }))
  }

  stopAudioRecording(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    console.log('⏹️ 发送停止录音信号')
    this.ws.send(JSON.stringify({ type: 'audio_end' }))
  }

  // 发送心跳
  sendPing(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return
    }
    this.ws.send(JSON.stringify({ type: 'ping' }))
  }

  // 事件监听器
  on(event: string, callback: Function): void {
    if (!this.callbacks.has(event)) {
      this.callbacks.set(event, [])
    }
    this.callbacks.get(event)?.push(callback)
  }

  private emit(event: string, data?: any): void {
    const callbacks = this.callbacks.get(event)
    if (callbacks) {
      callbacks.forEach(callback => callback(data))
    }
  }

  // 自动重连
  private attemptReconnect(): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      const delay = Math.pow(2, this.reconnectAttempts) * 1000
      console.log(`🔄 尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts}) - ${delay}ms后`)

      setTimeout(() => {
        this.connect()
      }, delay)
    } else {
      console.error('❌ 重连次数已达上限')
      this.emit('reconnectFailed')
    }
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  // 获取连接状态
  get readyState(): number {
    return this.ws?.readyState || WebSocket.CLOSED
  }

  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 音频管理器类
export class AudioManager {
  private audioContext: AudioContext | null = null
  private mediaRecorder: MediaRecorder | null = null
  private audioQueue: ArrayBuffer[] = []
  private isPlaying = false
  private isRecording = false
  private audioStream: MediaStream | null = null

  // VAD (语音活动检测) 相关属性
  private analyser: AnalyserNode | null = null
  private dataArray: Uint8Array | null = null
  private vadThreshold = 30 // 语音检测阈值 (0-100)
  private vadSensitivity = 0.6 // 灵敏度 (0-1)
  private isVoiceActive = false
  private vadCheckInterval: number | null = null
  private voiceStartTime = 0
  private voiceEndTime = 0
  private silenceThreshold = 300 // 静音阈值，毫秒
  private minimumVoiceDuration = 200 // 最小语音持续时间，毫秒
  private currentWsClient: VocaTaWebSocketClient | null = null
  private audioBufferQueue: ArrayBuffer[] = [] // 临时存储音频数据的队列

  async initialize(): Promise<void> {
    try {
      console.log('🎵 音频管理器初始化完成（延迟初始化AudioContext）')
      // 不再在初始化时立即创建AudioContext，而是在需要时才创建
      // 这样避免了浏览器的安全策略限制
    } catch (error) {
      console.error('❌ 音频管理器初始化失败:', error)
      throw error
    }
  }

  // 延迟初始化AudioContext，在用户交互后调用
  private async ensureAudioContext(): Promise<void> {
    if (!this.audioContext) {
      console.log('🎵 延迟初始化音频上下文...')
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)()

      // 检查音频上下文状态
      if (this.audioContext.state === 'suspended') {
        await this.audioContext.resume()
      }

      console.log('✅ 音频上下文初始化成功')
    }
  }

  async startRecording(wsClient: VocaTaWebSocketClient): Promise<void> {
    try {
      console.log('🎤 请求麦克风权限...')
      this.currentWsClient = wsClient

      // 确保AudioContext已初始化
      await this.ensureAudioContext()

      // 检查浏览器支持情况和兼容性处理

      console.log('🔍 初始浏览器检查:', {
        mediaDevices: !!navigator.mediaDevices,
        getUserMedia: !!navigator.getUserMedia,
        webkitGetUserMedia: !!(navigator as any).webkitGetUserMedia,
        mozGetUserMedia: !!(navigator as any).mozGetUserMedia,
        userAgent: navigator.userAgent
      })

      if (!navigator.mediaDevices) {
        throw new Error('浏览器不支持音频功能')
      }

      // 移除getUserMedia检查，因为我们已经在上面创建了polyfill


      // 检查是否在安全上下文中（HTTPS或localhost）
      const isSecureContext = location.protocol === 'https:' ||
                             location.hostname === 'localhost' ||
                             location.hostname === '127.0.0.1' ||
                             location.hostname === '0.0.0.0' ||
                             // 允许HTTP环境进行测试
                             location.protocol === 'http:'

      if (!isSecureContext) {
        console.warn('⚠️ 检测到非安全上下文，某些浏览器可能阻止麦克风访问')
      } else if (location.protocol === 'http:') {
        console.info('ℹ️ HTTP环境下测试音频功能，建议生产环境使用HTTPS')
      }

      console.log('🔍 浏览器环境检查:', {
        protocol: location.protocol,
        hostname: location.hostname,
        mediaDevices: !!navigator.mediaDevices,
        getUserMedia: !!navigator.mediaDevices?.getUserMedia,
        userAgent: navigator.userAgent.substring(0, 100)
      })

      // 直接获取麦克风权限
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true

        }
      }

      // 检查MediaRecorder支持
      if (!window.MediaRecorder) {
        throw new Error('浏览器不支持MediaRecorder API，请使用Chrome、Firefox或Edge浏览器')
      }

      console.log('✅ 音频流获取成功:', {
        tracks: this.audioStream.getTracks().length,
        active: this.audioStream.active
      })

      // 检查MediaRecorder支持
      if (!window.MediaRecorder) {
        console.warn('⚠️ MediaRecorder不支持，创建模拟对象')
        ;(window as any).MediaRecorder = class MockMediaRecorder {
          constructor(stream: any, options?: any) {
            this.stream = stream
            this.ondataavailable = null
          }
          start(timeslice?: number) {
            console.log('模拟录音开始')
            setTimeout(() => {
              if (this.ondataavailable) {
                this.ondataavailable({ data: new Blob() })
              }
            }, timeslice || 1000)
          }
          stop() { console.log('模拟录音停止') }
          static isTypeSupported() { return true }
        }
      }

      // 检查MediaRecorder支持的格式
      let mimeType = 'audio/webm;codecs=opus'
      if (!MediaRecorder.isTypeSupported(mimeType)) {
        mimeType = 'audio/webm'
        if (!MediaRecorder.isTypeSupported(mimeType)) {
          mimeType = 'audio/ogg;codecs=opus'
          if (!MediaRecorder.isTypeSupported(mimeType)) {
            mimeType = 'audio/wav'
            if (!MediaRecorder.isTypeSupported(mimeType)) {
              // 最后的兜底方案
              mimeType = 'audio/mpeg'
              if (!MediaRecorder.isTypeSupported(mimeType)) {
                console.warn('⚠️ 未找到完全支持的音频格式，使用默认设置')
                mimeType = '' // 使用浏览器默认格式
              }
            }
          }
        }
      }

      console.log('🎵 使用音频格式:', mimeType)

      // 创建MediaRecorder实例，使用兼容性更好的配置
      const mediaRecorderOptions: MediaRecorderOptions = {}
      if (mimeType) {
        mediaRecorderOptions.mimeType = mimeType
      }
      // 只在支持的情况下设置音频比特率
      try {
        if (mimeType && MediaRecorder.isTypeSupported(mimeType)) {
          mediaRecorderOptions.audioBitsPerSecond = 16000
        }
      } catch (e) {
        console.warn('⚠️ 设置音频比特率失败，使用默认设置:', e)
      }

      this.mediaRecorder = new MediaRecorder(this.audioStream, mediaRecorderOptions)


      // 设置VAD音频分析
      await this.setupVAD()


      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          event.data.arrayBuffer().then(buffer => {
            // 将音频数据添加到缓冲队列，而不是立即发送
            this.audioBufferQueue.push(buffer)
            console.log(`🎵 音频数据已缓存: ${buffer.byteLength} bytes (${mimeType})，等待VAD检测`)
          })
        }
      }

      this.mediaRecorder.start(200) // 每200ms记录一次数据
      this.isRecording = true
      console.log('✅ 开始录音 (已启用VAD语音活动检测)')

      // 启动VAD检测
      this.startVADMonitoring()

    } catch (error) {
      console.error('❌ 录音启动失败:', error)
      throw error
    }
  }

  stopRecording(): void {
    if (this.mediaRecorder && this.isRecording) {
      this.mediaRecorder.stop()
      if (this.audioStream) {
        this.audioStream.getTracks().forEach(track => track.stop())
      }
      this.isRecording = false

      // 停止VAD监控
      this.stopVADMonitoring()

      // 清空音频缓冲队列
      this.audioBufferQueue = []
      this.currentWsClient = null

      console.log('⏹️ 停止录音')
    }
  }

  async playAudio(audioBuffer: ArrayBuffer): Promise<void> {
    try {
      if (!this.audioContext) {
        await this.initialize()
      }

      const audioData = await this.audioContext!.decodeAudioData(audioBuffer.slice())
      const source = this.audioContext!.createBufferSource()
      source.buffer = audioData

      // 添加音量控制
      const gainNode = this.audioContext!.createGain()
      source.connect(gainNode)
      gainNode.connect(this.audioContext!.destination)

      source.start()
      console.log(`🔊 播放音频: 时长${audioData.duration.toFixed(2)}秒`)

      return new Promise((resolve) => {
        source.onended = () => resolve()
      })
    } catch (error) {
      console.error('❌ 音频播放失败:', error)
    }
  }

  // 音频队列管理
  addToQueue(audioBuffer: ArrayBuffer): void {
    this.audioQueue.push(audioBuffer)
    if (!this.isPlaying) {
      this.playQueue()
    }
  }

  private async playQueue(): Promise<void> {
    if (this.audioQueue.length === 0) {
      this.isPlaying = false
      return
    }

    this.isPlaying = true

    try {
      // 确保AudioContext已初始化
      await this.ensureAudioContext()
    } catch (error) {
      console.warn('⚠️ AudioContext初始化失败，跳过音频播放:', error)
      this.isPlaying = false
      return
    }

    const audioBuffer = this.audioQueue.shift()!

    try {
      await this.playAudio(audioBuffer)
    } catch (error) {
      console.error('❌ 队列音频播放失败:', error)
    }

    // 播放下一个
    this.playQueue()
  }

  clearQueue(): void {
    this.audioQueue = []
    this.isPlaying = false
    console.log('🗑️ 清除音频队列')
  }

  // 获取音量级别（用于可视化）
  getVolumeAnalyzer(): (() => number) | null {
    if (!this.audioStream || !this.audioContext) {
      return null
    }

    const analyser = this.audioContext.createAnalyser()
    const microphone = this.audioContext.createMediaStreamSource(this.audioStream)
    const dataArray = new Uint8Array(analyser.frequencyBinCount)

    microphone.connect(analyser)
    analyser.fftSize = 256

    return () => {
      analyser.getByteFrequencyData(dataArray)
      const average = dataArray.reduce((sum, value) => sum + value, 0) / dataArray.length
      return average / 255 // 标准化到0-1
    }
  }

  // 检查麦克风权限
  async checkMicrophonePermission(): Promise<PermissionState> {
    try {
      const result = await navigator.permissions.query({ name: 'microphone' as PermissionName })
      return result.state
    } catch {
      return 'prompt'
    }
  }

  get recording(): boolean {
    return this.isRecording
  }

  get playing(): boolean {
    return this.isPlaying
  }

  // VAD (语音活动检测) 相关方法
  private async setupVAD(): Promise<void> {
    try {
      if (!this.audioContext || !this.audioStream) {
        console.warn('⚠️ AudioContext或AudioStream未初始化，跳过VAD设置')
        return
      }

      // 创建音频分析器
      this.analyser = this.audioContext.createAnalyser()
      this.analyser.fftSize = 1024
      this.analyser.smoothingTimeConstant = 0.3

      // 创建音频源
      const source = this.audioContext.createMediaStreamSource(this.audioStream)
      source.connect(this.analyser)

      // 创建数据数组
      this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)

      console.log('✅ VAD语音活动检测已初始化')
    } catch (error) {
      console.warn('⚠️ VAD初始化失败，将跳过语音检测功能:', error)
    }
  }

  private startVADMonitoring(): void {
    if (this.vadCheckInterval) {
      clearInterval(this.vadCheckInterval)
    }

    this.vadCheckInterval = window.setInterval(() => {
      this.checkVoiceActivity()
    }, 50) // 每50ms检查一次语音活动

    console.log('🎯 VAD监控已启动')
  }

  private stopVADMonitoring(): void {
    if (this.vadCheckInterval) {
      clearInterval(this.vadCheckInterval)
      this.vadCheckInterval = null
    }

    // 如果当前有语音活动，发送结束信号
    if (this.isVoiceActive) {
      this.onVoiceEnd()
    }

    console.log('🛑 VAD监控已停止')
  }

  private checkVoiceActivity(): void {
    if (!this.analyser || !this.dataArray) {
      return
    }

    try {
      // 获取音频频域数据
      this.analyser.getByteFrequencyData(this.dataArray)

      // 计算音量级别 (使用频域数据)
      let sum = 0
      for (let i = 0; i < this.dataArray.length; i++) {
        sum += this.dataArray[i]
      }
      const averageLevel = sum / this.dataArray.length

      // 计算动态阈值 (基于最近的噪音水平)
      const dynamicThreshold = this.vadThreshold + (averageLevel * this.vadSensitivity * 0.1)

      // 检测语音活动
      const currentTime = Date.now()
      const hasVoice = averageLevel > dynamicThreshold

      if (hasVoice && !this.isVoiceActive) {
        // 语音开始
        this.voiceStartTime = currentTime
        this.isVoiceActive = true
        this.onVoiceStart()
        console.log(`🎤 检测到语音开始 (音量: ${averageLevel.toFixed(1)}, 阈值: ${dynamicThreshold.toFixed(1)})`)

      } else if (!hasVoice && this.isVoiceActive) {
        // 检查是否达到静音阈值
        if (currentTime - this.voiceStartTime > this.minimumVoiceDuration) {
          this.voiceEndTime = currentTime
          // 延迟检查，避免短暂静音导致的误判
          setTimeout(() => {
            if (this.isVoiceActive && Date.now() - this.voiceEndTime > this.silenceThreshold) {
              this.isVoiceActive = false
              this.onVoiceEnd()
              console.log(`🔇 检测到语音结束 (持续时间: ${this.voiceEndTime - this.voiceStartTime}ms)`)
            }
          }, this.silenceThreshold)
        }
      }

      // 可选：输出实时音量级别用于调试
      if (Math.random() < 0.05) { // 5%的概率输出，避免日志过多
        console.log(`🔊 实时音量: ${averageLevel.toFixed(1)} (阈值: ${dynamicThreshold.toFixed(1)}, 语音活动: ${this.isVoiceActive})`)
      }

    } catch (error) {
      console.error('❌ VAD检查失败:', error)
    }
  }

  private onVoiceStart(): void {
    console.log('🎙️ 语音活动开始，开始发送音频数据')

    // 通知WebSocket开始音频传输
    if (this.currentWsClient) {
      this.currentWsClient.startAudioRecording()
    }
  }

  private onVoiceEnd(): void {
    console.log('🔇 语音活动结束，停止发送音频数据')

    // 发送缓冲区中的所有音频数据
    this.flushAudioBuffer()

    // 通知WebSocket停止音频传输
    if (this.currentWsClient) {
      this.currentWsClient.stopAudioRecording()
    }
  }

  private flushAudioBuffer(): void {
    if (this.audioBufferQueue.length > 0 && this.currentWsClient) {
      console.log(`📤 发送缓冲的音频数据: ${this.audioBufferQueue.length} 个片段`)

      // 依次发送所有缓冲的音频数据
      this.audioBufferQueue.forEach((buffer, index) => {
        setTimeout(() => {
          if (this.currentWsClient) {
            this.currentWsClient.sendAudioData(buffer)
            console.log(`🎵 发送音频片段 ${index + 1}/${this.audioBufferQueue.length}: ${buffer.byteLength} bytes`)
          }
        }, index * 10) // 每个片段间隔10ms发送，避免网络拥塞
      })

      // 清空缓冲区
      this.audioBufferQueue = []
    }
  }

  // 获取VAD状态
  get voiceActive(): boolean {
    return this.isVoiceActive
  }

  // 配置VAD参数
  configureVAD(threshold: number, sensitivity: number, silenceMs: number, minVoiceMs: number): void {
    this.vadThreshold = Math.max(0, Math.min(100, threshold))
    this.vadSensitivity = Math.max(0, Math.min(1, sensitivity))
    this.silenceThreshold = Math.max(100, silenceMs)
    this.minimumVoiceDuration = Math.max(50, minVoiceMs)

    console.log(`⚙️ VAD配置更新: 阈值=${this.vadThreshold}, 灵敏度=${this.vadSensitivity}, 静音阈值=${this.silenceThreshold}ms, 最小语音时长=${this.minimumVoiceDuration}ms`)
  }
}

// 实时AI对话管理器
export class VocaTaAIChat {
  private wsClient: VocaTaWebSocketClient | null = null
  private audioManager: AudioManager
  private isAudioCallActive = false
  private currentConversation: any = null
  private currentCharacter: any = null

  // 临时消息存储，用于流式显示
  private currentLLMResponse = ''
  private currentSTTText = ''

  // 回调函数
  private onMessageCallback?: (message: any) => void
  private onSTTResultCallback?: (text: string, isFinal: boolean) => void
  private onLLMStreamCallback?: (text: string, isComplete: boolean, characterName?: string) => void
  private onAudioPlayCallback?: (isPlaying: boolean) => void
  private onConnectionStatusCallback?: (status: 'connected' | 'disconnected' | 'error', message?: string) => void

  constructor() {
    this.audioManager = new AudioManager()
  }

  async initialize(conversationUuid: string): Promise<void> {
    try {
      console.log('🚀 初始化AI对话系统...')

      // 初始化音频管理器
      await this.audioManager.initialize()

      // 建立WebSocket连接并等待连接成功
      await this.connectWebSocket(conversationUuid)

      console.log('✅ AI对话系统初始化完成')
    } catch (error) {
      console.error('❌ AI对话系统初始化失败:', error)
      throw error
    }
  }

  private connectWebSocket(conversationUuid: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.wsClient = new VocaTaWebSocketClient(conversationUuid)
      let connectionResolved = false // 防止重复resolve

      // 设置事件监听器
      this.wsClient.on('connected', () => {
        console.log('🎉 WebSocket连接成功，等待服务器确认...')
        // 不在这里resolve，等待服务器状态消息
      })

      this.wsClient.on('message', (message: WebSocketMessage) => {
        this.handleWebSocketMessage(message)

        // 如果收到状态消息表示连接已建立，则resolve
        if (!connectionResolved && message.type === 'status' &&
          (message.message?.includes('连接已建立') || message.message?.includes('WebSocket连接已建立'))) {
          console.log('🎉 收到服务器连接确认，连接完全建立')
          connectionResolved = true
          this.onConnectionStatusCallback?.('connected', 'WebSocket连接已建立')
          resolve()
        }

        // 如果还没有连接确认，但收到了任何其他消息（AI回复等），也认为连接成功
        if (!connectionResolved && (message.type === 'llm_text_stream' || message.type === 'text_message')) {
          console.log('🎯 收到AI消息，连接确认成功')
          connectionResolved = true
          this.onConnectionStatusCallback?.('connected', 'AI系统连接成功')
          resolve()
        }
      })

      this.wsClient.on('audioData', (audioBuffer: ArrayBuffer) => {
        this.handleAudioData(audioBuffer)
      })

      this.wsClient.on('error', (error: any) => {
        console.error('❌ WebSocket错误:', error)
        this.onConnectionStatusCallback?.('error', 'WebSocket连接错误')
        if (!connectionResolved) {
          connectionResolved = true
          reject(error)
        }
      })

      this.wsClient.on('disconnected', () => {
        console.log('📡 WebSocket连接断开，正在重连...')
        this.onConnectionStatusCallback?.('disconnected', '连接已断开，正在重连...')
      })

      this.wsClient.on('reconnectFailed', () => {
        console.error('❌ WebSocket重连失败')
        this.onConnectionStatusCallback?.('error', '连接失败，请刷新页面重试')
      })

      // 启动连接
      this.wsClient.connect()

      // 设置超时，如果10秒内没有连接成功，则reject
      setTimeout(() => {
        if (!connectionResolved) {
          console.error('❌ WebSocket连接超时')
          connectionResolved = true
          reject(new Error('WebSocket连接超时'))
        }
      }, 10000)
    })
  }

  private handleWebSocketMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'stt_result':
        this.handleSTTResult(message as STTResultMessage)
        break

      case 'llm_text_stream':
        this.handleLLMTextStream(message as LLMTextStreamMessage)
        break

      case 'tts_audio_meta':
        this.handleTTSAudioMeta(message as TTSAudioMetaMessage)
        break

      case 'complete':
        this.handleProcessComplete(message as CompleteMessage)
        break

      case 'error':
        this.handleError(message as ErrorMessage)
        break

      default:
        console.log('🔄 收到其他类型消息:', message)
    }

    // 触发通用消息回调
    this.onMessageCallback?.(message)
  }

  private handleSTTResult(message: STTResultMessage): void {
    console.log(`🎤 STT识别: ${message.text} (${message.isFinal ? '最终' : '临时'})`)

    this.currentSTTText = message.text
    this.onSTTResultCallback?.(message.text, message.isFinal)
  }

  private handleLLMTextStream(message: LLMTextStreamMessage): void {
    console.log(`🤖 LLM响应: ${message.text} (${message.isComplete ? '完成' : '流式'})`)

    // 修复：始终累积文本，无论是否完成
    // 流式渲染应该累积所有收到的文本片段
    this.currentLLMResponse += message.text

    console.log(`🔍 当前累积文本长度: ${this.currentLLMResponse.length}`)

    this.onLLMStreamCallback?.(this.currentLLMResponse, message.isComplete, message.characterName)

    if (message.isComplete) {
      this.currentLLMResponse = '' // 重置
    }
  }

  private handleTTSAudioMeta(message: TTSAudioMetaMessage): void {
    console.log(`🔊 TTS音频元数据: ${message.audioSize} bytes, ${message.format}`)
  }

  private handleAudioData(audioBuffer: ArrayBuffer): void {
    console.log(`🔊 播放音频数据: ${audioBuffer.byteLength} bytes`)
    this.audioManager.addToQueue(audioBuffer)
    this.onAudioPlayCallback?.(true)
  }

  private handleProcessComplete(message: CompleteMessage): void {
    console.log('✅ 处理完成:', message.message)
  }

  private handleError(message: ErrorMessage): void {
    console.error('❌ 服务器错误:', message.error)
  }

  // 公开方法
  sendTextMessage(text: string): void {
    if (!this.wsClient) {
      console.error('❌ WebSocket客户端未初始化')
      return
    }

    this.wsClient.sendTextMessage(text)
  }

  async startAudioCall(): Promise<void> {
    try {
      this.isAudioCallActive = true
      console.log('📞 开始音频通话')

      await this.audioManager.startRecording(this.wsClient!)
      this.wsClient?.startAudioRecording()

    } catch (error) {
      console.error('❌ 无法启动音频通话:', error)
      this.isAudioCallActive = false
      throw error
    }
  }

  stopAudioCall(): void {
    console.log('📞 停止音频通话')
    this.isAudioCallActive = false

    this.audioManager.stopRecording()
    this.audioManager.clearQueue()
    this.wsClient?.stopAudioRecording()

    this.onAudioPlayCallback?.(false)
  }

  // 设置回调函数
  onMessage(callback: (message: any) => void): void {
    this.onMessageCallback = callback
  }

  onSTTResult(callback: (text: string, isFinal: boolean) => void): void {
    this.onSTTResultCallback = callback
  }

  onLLMStream(callback: (text: string, isComplete: boolean, characterName?: string) => void): void {
    this.onLLMStreamCallback = callback
  }

  onAudioPlay(callback: (isPlaying: boolean) => void): void {
    this.onAudioPlayCallback = callback
  }

  onConnectionStatus(callback: (status: 'connected' | 'disconnected' | 'error', message?: string) => void): void {
    this.onConnectionStatusCallback = callback
  }

  // 获取状态
  get connected(): boolean {
    const isConnected = this.wsClient?.isConnected || false
    console.log('🔍 检查连接状态:', {
      wsClient: !!this.wsClient,
      readyState: this.wsClient?.readyState,
      isConnected: isConnected,
      expectedReadyState: WebSocket.OPEN
    })
    return isConnected
  }

  get audioCallActive(): boolean {
    return this.isAudioCallActive
  }

  get recording(): boolean {
    return this.audioManager.recording
  }

  get playing(): boolean {
    return this.audioManager.playing
  }

  // 清理资源
  destroy(): void {
    console.log('🧹 清理AI对话系统资源')

    this.stopAudioCall()
    this.wsClient?.disconnect()
    this.audioManager.clearQueue()

    this.wsClient = null
    this.onMessageCallback = undefined
    this.onSTTResultCallback = undefined
    this.onLLMStreamCallback = undefined
    this.onAudioPlayCallback = undefined
    this.onConnectionStatusCallback = undefined
  }
}