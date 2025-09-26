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
  contentType?: number, // 1=文本, 2=图片, 3=音频
  audioUrl?: string | null,
  createDate?: string,
  metadata?: Record<string, any>
}