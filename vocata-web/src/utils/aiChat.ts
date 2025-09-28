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

interface TTSResultMessage extends WebSocketMessage {
  type: 'tts_result'
  text: string
  format: string
  sampleRate: number
  voiceId?: string
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

// 音频管理器类 - 批量录音模式
export class AudioManager {
  private audioContext: AudioContext | null = null
  private mediaRecorder: MediaRecorder | null = null
  private audioQueue: ArrayBuffer[] = []
  private isPlaying = false
  private isRecording = false
  private audioStream: MediaStream | null = null

  // 批量录音模式 - 收集完整音频段
  private recordedChunks: Blob[] = []
  private currentWsClient: VocaTaWebSocketClient | null = null
  private stopRecordingPromise: Promise<void> | null = null
  private stopRecordingResolve?: () => void
  private stopRecordingReject?: (reason?: any) => void
  private playbackStateListener?: (isPlaying: boolean) => void

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

  async preparePlayback(): Promise<void> {
    try {
      await this.ensureAudioContext()
    } catch (error) {
      console.warn('⚠️ 准备音频播放失败:', error)
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
    } else if (this.audioContext.state === 'suspended') {
      console.log('🔄 音频上下文处于挂起状态，尝试恢复...')
      await this.audioContext.resume()
    }
  }

  async startRecording(wsClient: VocaTaWebSocketClient): Promise<void> {
    try {
      console.log('🎤 开始批量录音模式...')
      this.currentWsClient = wsClient
      this.recordedChunks = [] // 重置录音数据
      this.stopRecordingPromise = null
      this.stopRecordingResolve = undefined
      this.stopRecordingReject = undefined

      // 确保AudioContext已初始化
      await this.ensureAudioContext()

      console.log('🎤 请求麦克风权限...')

      // 直接获取麦克风权限
      this.audioStream = await navigator.mediaDevices.getUserMedia({
        audio: {
          channelCount: 1,
          sampleRate: 16000,
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      })

      // 验证音频流
      const tracks = this.audioStream.getTracks()
      const audioTracks = tracks.filter(track => track.kind === 'audio')

      console.log('🔍 音频流详细信息:', {
        tracks: this.audioStream.getTracks().length,
        audioTracks: audioTracks.length,
        active: this.audioStream.active
      })

      if (audioTracks.length === 0 || !this.audioStream.active) {
        throw new Error('未能获取有效的音频轨道')
      }

      // 选择最佳音频格式
      let mimeType = 'audio/webm;codecs=opus'
      if (!MediaRecorder.isTypeSupported(mimeType)) {
        mimeType = 'audio/webm'
        if (!MediaRecorder.isTypeSupported(mimeType)) {
          mimeType = 'audio/wav'
          if (!MediaRecorder.isTypeSupported(mimeType)) {
            mimeType = 'audio/mpeg'
            if (!MediaRecorder.isTypeSupported(mimeType)) {
              mimeType = '' // 使用浏览器默认格式
            }
          }
        }
      }

      console.log('🎵 使用音频格式:', mimeType || '默认格式')

      // 创建MediaRecorder - 批量模式，不设置timeslice
      const mediaRecorderOptions: MediaRecorderOptions = {}
      if (mimeType) {
        mediaRecorderOptions.mimeType = mimeType
      }

      this.mediaRecorder = new MediaRecorder(this.audioStream, mediaRecorderOptions)

      // 批量录音 - 收集所有数据到chunks数组
      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          console.log(`🎤 收集音频块: ${event.data.size} bytes`)
          this.recordedChunks.push(event.data)
        }
      }

      // 录音结束时发送完整音频
      this.mediaRecorder.onstop = () => {
        this.handleMediaRecorderStop()
      }

      // 开始录音（不设置timeslice，收集完整音频）
      this.mediaRecorder.start()
      this.isRecording = true
      console.log('✅ 开始批量录音 (手动控制模式)')

    } catch (error) {
      console.error('❌ 录音启动失败:', error)
      throw error
    }
  }

  async stopRecording(): Promise<void> {
    if (!this.mediaRecorder || !this.isRecording) {
      return
    }

    if (!this.stopRecordingPromise) {
      this.stopRecordingPromise = new Promise<void>((resolve, reject) => {
        this.stopRecordingResolve = resolve
        this.stopRecordingReject = reject

        try {
          this.mediaRecorder!.stop()
          if (this.audioStream) {
            this.audioStream.getTracks().forEach(track => track.stop())
          }
          this.isRecording = false

          console.log('⏹️ 停止批量录音')
          // 注意：不要在这里清理currentWsClient，因为processBatchAudio还需要使用它
        } catch (error) {
          console.error('❌ 停止录音失败:', error)
          this.stopRecordingResolve = undefined
          this.stopRecordingReject = undefined
          this.stopRecordingPromise = null
          reject(error)
        }
      })
    }

    try {
      await this.stopRecordingPromise
    } finally {
      this.stopRecordingPromise = null
      this.stopRecordingResolve = undefined
      this.stopRecordingReject = undefined
    }
  }

  // 处理批量录音音频数据
  private async processBatchAudio(): Promise<void> {
    try {
      if (this.recordedChunks.length === 0) {
        console.warn('⚠️ 没有录音数据')
        return
      }

      console.log(`🎤 处理批量音频: ${this.recordedChunks.length} 个音频块`)

      // 合并所有音频块
      const audioBlob = new Blob(this.recordedChunks, { type: this.recordedChunks[0].type })
      const audioBuffer = await audioBlob.arrayBuffer()

      console.log(`📦 批量音频数据: ${audioBuffer.byteLength} bytes, 格式: ${audioBlob.type}`)

      // 发送完整音频到WebSocket
      if (this.currentWsClient?.isConnected) {
        this.currentWsClient.sendAudioData(audioBuffer)
        console.log(`📤 已发送批量音频到服务器: ${audioBuffer.byteLength} bytes`)
      } else {
        console.error('❌ WebSocket未连接，无法发送音频数据')
      }

      // 清理录音数据
      this.recordedChunks = []

      // 完成后清理WebSocket客户端引用
      this.currentWsClient = null

    } catch (error) {
      console.error('❌ 处理批量音频失败:', error)
    }
  }

  private async handleMediaRecorderStop(): Promise<void> {
    try {
      await this.processBatchAudio()
      this.stopRecordingResolve?.()
    } catch (error) {
      console.error('❌ 处理录音停止事件失败:', error)
      this.stopRecordingReject?.(error)
    } finally {
      this.stopRecordingResolve = undefined
      this.stopRecordingReject = undefined
      this.stopRecordingPromise = null
      this.mediaRecorder = null
      this.audioStream = null
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
      this.notifyPlaybackState(false)
      return
    }

    this.isPlaying = true
    this.notifyPlaybackState(true)

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
    this.notifyPlaybackState(false)
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

  setPlaybackStateListener(listener: (isPlaying: boolean) => void): void {
    this.playbackStateListener = listener
  }

  private notifyPlaybackState(isPlaying: boolean): void {
    this.playbackStateListener?.(isPlaying)
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
    this.audioManager.setPlaybackStateListener(isPlaying => {
      this.onAudioPlayCallback?.(isPlaying)
    })
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

      case 'tts_result':
        this.handleTTSResult(message as TTSResultMessage)
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

  private handleTTSResult(message: TTSResultMessage): void {
    console.log(`🗣️ TTS最终文字: ${message.text} (格式: ${message.format}, 采样率: ${message.sampleRate})`)

    if (message.text) {
      this.onLLMStreamCallback?.(message.text, true, message.voiceId)
    }
  }

  private handleTTSAudioMeta(message: TTSAudioMetaMessage): void {
    console.log(`🔊 TTS音频元数据: ${message.audioSize} bytes, ${message.format}`)
  }

  private handleAudioData(audioBuffer: ArrayBuffer): void {
    console.log(`🔊 播放音频数据: ${audioBuffer.byteLength} bytes`)
    this.audioManager.addToQueue(audioBuffer)
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

  // 开始录音
  async startRecording(): Promise<void> {
    try {
      console.log('📞 开始批量录音')

      await this.audioManager.startRecording(this.wsClient!)
      this.wsClient?.startAudioRecording()

    } catch (error) {
      console.error('❌ 无法启动录音:', error)
      throw error
    }
  }

  async prepareAudioPlayback(): Promise<void> {
    try {
      await this.audioManager.preparePlayback()
    } catch (error) {
      console.warn('⚠️ 准备音频上下文失败:', error)
    }
  }

  // 停止录音
  async stopRecording(): Promise<void> {
    console.log('📞 停止录音并发送批量音频')

    await this.audioManager.stopRecording()
    this.wsClient?.stopAudioRecording()
  }

  // 兼容旧的音频通话方法
  async startAudioCall(): Promise<void> {
    if (!this.wsClient || !this.wsClient.isConnected) {
      throw new Error('WebSocket未连接，无法启动音频通话')
    }

    console.log('📞 音频通话已激活，等待用户点击开始说话')
    this.isAudioCallActive = true

    // 清空残留的播放队列，确保新的通话段落从空状态开始
    this.audioManager.clearQueue()
    this.onAudioPlayCallback?.(false)
  }

  async stopAudioCall(): Promise<void> {
    if (this.recording) {
      await this.stopRecording()
    }
    this.isAudioCallActive = false
    this.audioManager.clearQueue()
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

    void this.stopAudioCall()
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
