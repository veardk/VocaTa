import request from '../request'
import type { PublicRoleQuery } from '@/types/api'

export const roleApi = {
  // 获取公开角色列表
  getPublicRoleList(params: PublicRoleQuery) {
    return request({
      url: '/client/character/public',
      method: 'get',
      params
    })
  },
  // 获取精选角色列表
  getChoiceRoleList(params: { limit: number }) {
    return request({
      url: '/client/character/featured',
      method: 'get',
      params
    })
  }
}