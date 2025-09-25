import type { LoginParams, RegisterParams, Response } from '@/types/api'
import request from '../request'

export const userApi = {
  // 登录
  login(params: LoginParams): Promise<Response<null>> {
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
}