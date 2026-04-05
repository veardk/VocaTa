import request from '../request'
import type {
  AiGenerateRoleRequest,
  AiGenerateRoleResponse,
  CreateCharacterRequest,
  CreateCharacterResponse,
  PublicRoleQuery,
  Response,
  TtsVoiceOption,
} from '@/types/api'
import type { roleInfo } from '@/types/common'

type RolePayload = Record<string, unknown>

export const roleApi = {
  // 获取公开角色列表
  getPublicRoleList(params: PublicRoleQuery): Promise<Response<{ list: roleInfo[]; total: number }>> {
    return request({
      url: '/api/open/character/list',
      method: 'get',
      params
    })
  },
  // 获取精选角色列表
  getChoiceRoleList(params: { limit: number }): Promise<Response<roleInfo[]>> {
    return request({
      url: '/api/open/character/featured',
      method: 'get',
      params
    })
  },
  // 搜索角色
  searchRole(params: { keyword: string }): Promise<Response<{ list: roleInfo[]; total: number }>> {
    return request({
      url: '/api/open/character/search',
      method: 'get',
      params
    })
  },
  // 获取我的角色列表
  getMyRoleList(
    params?: RolePayload
  ): Promise<Response<{ list: roleInfo[]; total: number }>> {
    return request({
      url: '/api/client/character/my',
      method: 'get',
      params
    })
  },
  // 创建角色
  createRole(data: CreateCharacterRequest): Promise<Response<CreateCharacterResponse>> {
    return request({
      url: '/api/client/character',
      method: 'post',
      data
    })
  },
  // 获取音色列表
  getSoundList(): Promise<Response<TtsVoiceOption[]>> {
    return request({
      url: '/api/client/tts-voice/list',
      method: 'get'
    })
  },
  // 获取角色详情
  getCharacterDetail(id: string | number): Promise<Response<roleInfo>> {
    return request({
      url: `/api/open/character/${id}`,
      method: 'get'
    })
  },
  // AI生成角色提示词
  aiGenerate(data: AiGenerateRoleRequest): Promise<Response<AiGenerateRoleResponse>> {
    return request({
      url: '/api/client/character/ai-generate',
      method: 'post',
      data
    })
  }
}
