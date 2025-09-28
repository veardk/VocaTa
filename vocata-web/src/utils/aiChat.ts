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

  // VAD (语音活动检测) 相关属性 - 简化版本，解决乱发送问题
  private analyser: AnalyserNode | null = null
  private dataArray: Uint8Array | null = null
  private vadThreshold = 40 // 简单音量阈值
  private isVoiceActive = false
  private consecutiveActiveFrames = 0
  private consecutiveSilenceFrames = 0
  private minActiveFrames = 2 // 连续2帧检测到语音才确认
  private minSilenceFrames = 5 // 连续5帧静音才确认结束
  // 音频缓冲队列
  private audioBuffer: ArrayBuffer[] = []
  private maxBufferSize = 5 // 最大缓冲5个音频块
  private currentWsClient: VocaTaWebSocketClient | null = null

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

      // 创建完整的MediaDevices polyfill - 完全移除所有限制
      if (!navigator.mediaDevices) {
        console.warn('⚠️ 创建MediaDevices对象')
        navigator.mediaDevices = {
          getUserMedia: function(constraints: MediaStreamConstraints): Promise<MediaStream> {
            // 尝试所有可能的getUserMedia API实现
            const legacyGetUserMedia = (navigator as any).getUserMedia ||
                                     (navigator as any).webkitGetUserMedia ||
                                     (navigator as any).mozGetUserMedia ||
                                     (navigator as any).msGetUserMedia

            if (!legacyGetUserMedia) {
              console.error('❌ 浏览器完全不支持getUserMedia API')
              return Promise.reject(new Error('浏览器不支持麦克风功能'))
            }

            console.log('🔧 使用legacy getUserMedia API')
            return new Promise((resolve, reject) => {
              try {
                legacyGetUserMedia.call(navigator, constraints, resolve, reject)
              } catch (error) {
                console.error('❌ Legacy getUserMedia调用失败:', error)
                reject(new Error('无法访问麦克风设备'))
              }
            })
          }
        } as MediaDevices
      }

      // 如果MediaDevices存在但getUserMedia不存在，直接添加
      if (!navigator.mediaDevices.getUserMedia) {
        console.warn('⚠️ 添加getUserMedia方法到现有MediaDevices对象')

        // 尝试所有可能的getUserMedia API实现
        const legacyGetUserMedia = (navigator as any).getUserMedia ||
                                 (navigator as any).webkitGetUserMedia ||
                                 (navigator as any).mozGetUserMedia ||
                                 (navigator as any).msGetUserMedia

        if (!legacyGetUserMedia) {
          console.error('❌ 无法找到任何getUserMedia实现')
          throw new Error('浏览器不支持麦克风功能')
        }

        navigator.mediaDevices.getUserMedia = function(constraints: MediaStreamConstraints): Promise<MediaStream> {
          console.log('🔧 调用polyfill getUserMedia')
          return new Promise((resolve, reject) => {
            try {
              legacyGetUserMedia.call(navigator, constraints, resolve, reject)
            } catch (error) {
              console.error('❌ Polyfill getUserMedia调用失败:', error)
              reject(new Error('无法访问麦克风设备'))
            }
          })
        }
      }

      // 再次检查，不做安全限制
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        console.error('❌ 无法创建音频API polyfill')
        throw new Error('浏览器不支持音频功能')
      }

      // 直接尝试获取麦克风权限，完全移除所有安全检查
      console.log('🔍 浏览器环境检查:', {
        protocol: location.protocol,
        hostname: location.hostname,
        mediaDevices: !!navigator.mediaDevices,
        getUserMedia: !!navigator.mediaDevices?.getUserMedia,
        userAgent: navigator.userAgent.substring(0, 100)
      })

      console.info('✅ 已移除所有HTTPS和安全上下文限制，强制允许音频访问')

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

      // 关键修复：严格验证音频流和音频轨道
      const tracks = this.audioStream.getTracks()
      const audioTracks = tracks.filter(track => track.kind === 'audio')
      
      console.log('🔍 音频流详细信息:', {
        tracks: this.audioStream.getTracks().length,
        audioTracks: audioTracks.length,
        active: this.audioStream.active,
        trackDetails: tracks.map(track => ({
          kind: track.kind,
          enabled: track.enabled,
          readyState: track.readyState,
          label: track.label
        }))
      })

      // 验证音频轨道存在且有效
      if (audioTracks.length === 0) {
        throw new Error('未能获取有效的音频轨道，请检查麦克风权限或设备连接')
      }

      if (!this.audioStream.active) {
        throw new Error('音频流未激活，请检查麦克风设备状态')
      }

      // 验证音频轨道状态
      const activeAudioTracks = audioTracks.filter(track => track.readyState === 'live')
      if (activeAudioTracks.length === 0) {
        throw new Error('音频轨道未就绪，请重试或检查麦克风权限')
      }

      console.log('✅ 音频流验证通过:', {
        audioTracks: activeAudioTracks.length,
        firstTrackLabel: activeAudioTracks[0]?.label || 'unknown'
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

      // 创建MediaRecorder实例前进行最终验证
      console.log('🔧 创建MediaRecorder，配置:', mediaRecorderOptions)
      
      try {
        this.mediaRecorder = new MediaRecorder(this.audioStream, mediaRecorderOptions)
        console.log('✅ MediaRecorder创建成功')
      } catch (mediaRecorderError) {
        console.error('❌ MediaRecorder创建失败:', mediaRecorderError)
        // 尝试不带配置创建
        try {
          console.log('🔄 尝试使用默认配置创建MediaRecorder...')
          this.mediaRecorder = new MediaRecorder(this.audioStream)
          console.log('✅ 使用默认配置的MediaRecorder创建成功')
        } catch (fallbackError) {
          console.error('❌ 默认配置MediaRecorder也失败:', fallbackError)
          throw new Error(`MediaRecorder创建失败：${fallbackError.message}`)
        }
      }

      // 设置VAD音频分析
      await this.setupVAD()

      this.mediaRecorder.ondataavailable = (event) => {
        if (event.data.size > 0) {
          event.data.arrayBuffer().then(buffer => {
            // 使用简化的音频活动检测
            const hasValidSpeech = this.simpleAudioActivityCheck(buffer)

            if (hasValidSpeech && this.currentWsClient) {
              this.bufferAndSendAudio(buffer)
            }
          })
        }
      }

      this.mediaRecorder.start(500) // 每500ms记录一次数据，减少网络压力
      this.isRecording = true
      console.log('✅ 开始录音 (已启用严格的音频活动检测)')

      // 注意：不再使用复杂的VAD监控，直接在ondataavailable中进行检测

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

      // 重置VAD状态和缓冲区
      this.isVoiceActive = false
      this.consecutiveActiveFrames = 0
      this.consecutiveSilenceFrames = 0
      this.audioBuffer = []

      // 清理资源
      this.currentWsClient = null

      console.log('⏹️ 停止录音，VAD状态已重置')
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

      // 再次验证音频流中有有效的音频轨道
      const audioTracks = this.audioStream.getTracks().filter(track => track.kind === 'audio')
      if (audioTracks.length === 0) {
        console.warn('⚠️ 音频流中没有音频轨道，跳过VAD设置')
        return
      }

      // 检查音频轨道状态
      const liveAudioTracks = audioTracks.filter(track => track.readyState === 'live')
      if (liveAudioTracks.length === 0) {
        console.warn('⚠️ 音频轨道未激活，跳过VAD设置')
        return
      }

      console.log('🔧 开始初始化VAD，音频轨道状态:', {
        totalTracks: this.audioStream.getTracks().length,
        audioTracks: audioTracks.length,
        liveTracks: liveAudioTracks.length
      })

      // 创建音频分析器
      this.analyser = this.audioContext.createAnalyser()
      this.analyser.fftSize = 1024
      this.analyser.smoothingTimeConstant = 0.3

      // 创建音频源 - 增强错误处理
      console.log('🔧 创建MediaStreamSource...')
      const source = this.audioContext.createMediaStreamSource(this.audioStream)
      console.log('✅ MediaStreamSource创建成功')
      
      source.connect(this.analyser)
      console.log('✅ 音频源已连接到分析器')

      // 创建数据数组
      this.dataArray = new Uint8Array(this.analyser.frequencyBinCount)
      console.log('✅ VAD数据数组已创建，长度:', this.dataArray.length)

      console.log('✅ VAD语音活动检测已初始化')
    } catch (error) {
      console.error('❌ VAD初始化失败:', error)
      console.error('❌ 错误详情:', {
        errorName: error.name,
        errorMessage: error.message,
        audioContextState: this.audioContext?.state,
        audioStreamActive: this.audioStream?.active,
        audioStreamTracks: this.audioStream?.getTracks().length
      })
      console.warn('⚠️ 将继续录音，但不进行语音活动检测')
      // 不抛出错误，让录音继续，只是没有VAD功能
    }
  }

  /**
   * 简化的音频活动检测 - 基于音量阈值和连续帧检测
   */
  private simpleAudioActivityCheck(audioBuffer: ArrayBuffer): boolean {
    try {
      const view = new Uint8Array(audioBuffer)

      // 基础验证
      if (view.length < 100) {
        return false
      }

      // 计算平均音量
      let sum = 0
      for (let i = 0; i < view.length; i += 4) { // 降采样减少计算量
        sum += Math.abs(view[i] - 128)
      }
      const avgVolume = sum / (view.length / 4)

      // 简单阈值检测
      const hasVoice = avgVolume > this.vadThreshold

      if (hasVoice) {
        this.consecutiveActiveFrames++
        this.consecutiveSilenceFrames = 0
      } else {
        this.consecutiveActiveFrames = 0
        this.consecutiveSilenceFrames++
      }

      // 语音开始检测
      if (!this.isVoiceActive && this.consecutiveActiveFrames >= this.minActiveFrames) {
        this.isVoiceActive = true
        console.log('🎤 检测到语音开始')
        return true
      }

      // 语音持续检测
      if (this.isVoiceActive && hasVoice) {
        return true
      }

      // 语音结束检测
      if (this.isVoiceActive && this.consecutiveSilenceFrames >= this.minSilenceFrames) {
        this.isVoiceActive = false
        console.log('🔇 检测到语音结束')
        return false
      }

      return false

    } catch (error) {
      console.warn('⚠️ 音频检测失败:', error)
      return false
    }
  }

  /**
   * 智能音频缓冲和发送策略
   */
  private bufferAndSendAudio(audioBuffer: ArrayBuffer): void {
    // 将音频添加到缓冲区
    this.audioBuffer.push(audioBuffer)

    // 如果缓冲区满了，移除最老的数据
    if (this.audioBuffer.length > this.maxBufferSize) {
      this.audioBuffer.shift()
    }

    // 语音开始时，立即发送当前缓冲区的所有数据
    if (!this.isVoiceActive) {
      console.log('🎤 语音开始，批量发送缓冲音频')
      this.flushAudioBuffer()
    } else {
      // 语音持续期间，直接发送
      console.log(`🎵 发送音频: ${audioBuffer.byteLength} bytes`)
      this.currentWsClient?.sendAudioData(audioBuffer)
    }
  }

  /**
   * 发送缓冲区中的所有音频数据
   */
  private flushAudioBuffer(): void {
    if (this.audioBuffer.length > 0 && this.currentWsClient) {
      console.log(`📤 发送缓冲音频: ${this.audioBuffer.length} 个片段`)

      this.audioBuffer.forEach((buffer, index) => {
        // 按顺序发送，避免网络拥塞
        setTimeout(() => {
          this.currentWsClient?.sendAudioData(buffer)
        }, index * 50) // 每个片段间隔50ms
      })

      this.audioBuffer = []
    }
  }

  // VAD配置方法
  configureVAD(threshold: number, minActive: number, minSilence: number): void {
    this.vadThreshold = Math.max(20, Math.min(100, threshold))
    this.minActiveFrames = Math.max(1, minActive)
    this.minSilenceFrames = Math.max(2, minSilence)

    console.log(`⚙️ VAD配置更新: 阈值=${this.vadThreshold}, 最小活跃帧=${this.minActiveFrames}, 最小静音帧=${this.minSilenceFrames}`)
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