import request from '../request'
import type { PublicRoleQuery } from '@/types/api'

export const roleApi = {
  // 获取公开角色列表
  getPublicRoleList(params: PublicRoleQuery) {
    return request({
      url: '/api/open/character/list',
      method: 'get',
      params
    })
  },
  // 获取精选角色列表
  getChoiceRoleList(params: { limit: number }) {
    return request({
      url: '/api/open/character/featured',
      method: 'get',
      params
    })
  },
  // 搜索角色
  searchRole(params: { keyword: string }) {
    return request({
      url: '/api/client/character/search',
      method: 'get',
      params
    })
  }
}