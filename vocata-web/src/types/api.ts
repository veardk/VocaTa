// 公开角色查询参数
export interface PublicRoleQuery {
  keywords?: string,
  status?: number,
  isFeatured?: number,
  isTrending?: number,
  tags?: string[],
  language?: string,
  creatorId?: number,
  pageNum: number,
  pageSize: number,
  orderBy?: string,
  orderDirection?: string
}

// 登录参数
export interface LoginParams {
  loginName: string,
  password: string,
  rememberMe: boolean
}

// 注册参数
export interface RegisterParams {
  nickname: string,
  password: string,
  email: string,
  confirmPassword: string,
  verificationCode: string,
  gender: number,
  hasRead?: boolean
}

// 登录响应数据
export interface LoginResponse {
  token: string,
  expiresIn: number
}

// 用户信息响应数据
export interface UserInfo {
  id: string,
  nickname: string,
  email: string,
  avatar: string,
  gender: number,
  createDate: string
}

// 修改密码参数
export interface ChangePasswordParams {
  oldPassword: string,
  newPassword: string
}

// 返回参数
export interface Response<T> {
  code: number,
  message: string,
  data: T,
  timestamp?: number
}

// 对话模块相关类型定义

// 创建对话请求参数
export interface CreateConversationRequest {
  characterId: string,
  title?: string
}

// 对话响应数据
export interface ConversationResponse {
  conversationUuid: string,
  characterId: string,
  characterName: string,
  characterAvatarUrl: string,
  title: string | null,
  lastMessageSummary: string | null,
  status: number,
  createDate: string,
  updateDate: string
}

// 更新对话标题请求参数
export interface UpdateConversationTitleRequest {
  title: string
}

// 消息响应数据
export interface MessageResponse {
  messageUuid: string,
  senderType: number, // 1=用户, 2=AI角色
  contentType: number, // 1=文本, 2=图片, 3=音频
  textContent: string,
  audioUrl: string | null,
  llmModelId: string,
  ttsVoiceId: string | null,
  metadata: Record<string, any>,
  createDate: string
}