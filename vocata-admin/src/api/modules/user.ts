import type { LoginParams, RegisterParams, Response, LoginResponse } from '@/types/api'
import request from '../request'

export const userApi = {
  // 登录
  login(params: LoginParams): Promise<Response<LoginResponse>> {
    return request.post('/api/client/auth/login', params)
  },
  // 注册
  register(params: RegisterParams): Promise<Response<null>> {
    return request.post('/api/client/auth/register', params)
  },
  // 发送验证码
  sendCode(email: string): Promise<Response<null>> {
    return request.post('/api/client/auth/sendCode', { email })
  },

  // 退出登录
  logout(): Promise<Response<null>> {
    return request.post('/api/client/auth/logout')
  },
  // 获取用户信息
  getUserInfo(params): Promise<Response<null>> {
    return request.get('/api/admin/user/list', params)
  },

  // 修改用户状态
  updateUserStatus(id, params): Promise<Response<null>> {
    return request.put(`/api/admin/user/${id}/status`, params)
  },

  // 获取管理员信息
  getAdminInfo(): Promise<Response<null>> {
    return request.get('/api/admin/auth/current')
  },
}