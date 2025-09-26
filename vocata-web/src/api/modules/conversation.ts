import type {
  CreateConversationRequest,
  ConversationResponse,
  UpdateConversationTitleRequest,
  MessageResponse,
  Response
} from '@/types/api'
import request from '../request'

export const conversationApi = {
  // 创建新的对话会话
  createConversation(params: CreateConversationRequest): Promise<Response<ConversationResponse>> {
    return request.post('/api/client/conversations', params)
  },

  // 获取当前用户的历史对话列表
  getConversationList(): Promise<Response<ConversationResponse[]>> {
    return request.get('/api/client/conversations')
  },

  // 删除对话
  deleteConversation(conversationUuid: string): Promise<Response<null>> {
    return request.delete(`/api/client/conversations/${conversationUuid}`)
  },

  // 更新对话标题
  updateConversationTitle(
    conversationUuid: string,
    params: UpdateConversationTitleRequest
  ): Promise<Response<null>> {
    return request.put(`/api/client/conversations/${conversationUuid}/title`, params)
  },

  // 获取最新消息（推荐）- 对话界面初始加载
  getRecentMessages(conversationUuid: string, limit?: number): Promise<Response<MessageResponse[]>> {
    const params = limit ? `?limit=${limit}` : ''
    return request.get(`/api/client/conversations/${conversationUuid}/messages/recent${params}`)
  },

  // 分页获取历史消息 - 向前翻页查看更多历史消息
  getHistoryMessages(
    conversationUuid: string,
    offset?: number,
    limit?: number
  ): Promise<Response<MessageResponse[]>> {
    const params = new URLSearchParams()
    if (offset !== undefined) params.append('offset', offset.toString())
    if (limit !== undefined) params.append('limit', limit.toString())
    const queryString = params.toString() ? `?${params.toString()}` : ''
    return request.get(`/api/client/conversations/${conversationUuid}/messages/history${queryString}`)
  },

  // 获取所有消息（已废弃，不建议使用）
  getAllMessages(conversationUuid: string): Promise<Response<MessageResponse[]>> {
    return request.get(`/api/client/conversations/${conversationUuid}/messages`)
  }
}