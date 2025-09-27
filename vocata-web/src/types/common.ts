export interface roleInfo {
  "id"?: number,
  "characterCode"?: string,
  "name"?: string,
  "description"?: string,
  "greeting"?: string,
  "avatarUrl"?: string,
  "tags"?: string,
  "language"?: string,
  "status"?: number,
  "statusName"?: string,
  "isOfficial"?: number,
  "isFeatured"?: number,
  "isTrending"?: number,
  "trendingScore"?: number,
  "chatCount"?: number,
  "userCount"?: number,
  "isPrivate"?: boolean,
  "creatorId"?: number,
  "createdAt"?: string,
  "updatedAt"?: string
}

// 聊天历史项接口
export interface ChatHistoryItem {
  id: string,
  conversationUuid?: string,
  title?: string,
  lastTime?: Date | string,
  characterName?: string,
  characterAvatarUrl?: string,
  lastMessageSummary?: string | null,
  status?: number
}

// 聊天消息接口
export interface ChatMessage {
  messageUuid?: string,
  type: 'send' | 'receive',
  content: string,
  senderType?: number, // 1=用户, 2=AI角色
  contentType?: number, // 1=文本, 2=语音, 3=图片, 4=音频
  audioUrl?: string | null,
  createDate?: string,
  metadata?: Record<string, any>,
  // AI对话系统新增字段
  isStreaming?: boolean, // 是否为流式显示中的消息
  isRecognizing?: boolean, // 是否为语音识别中的消息
  characterName?: string, // AI角色名称
  confidence?: number // 语音识别置信度
}