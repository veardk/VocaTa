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
      url: '/api/open/character/search',
      method: 'get',
      params
    })
  },
  // 获取我的角色列表
  getMyRoleList(params?: any) {
    return request({
      url: '/api/client/character/my',
      method: 'get',
      params
    })
  },
  // 创建角色
  createRole(data: any) {
    return request({
      url: '/api/client/character',
      method: 'post',
      data
    })
  },
  // 获取音色列表
  getSoundList() {
    return request({
      url: '/api/client/tts-voice/list',
      method: 'get'
    })
  },
  // 获取角色详情
  getCharacterDetail(id: string | number) {
    return request({
      url: `/api/open/character/${id}`,
      method: 'get'
    })
  }
}
