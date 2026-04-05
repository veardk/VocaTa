import type {
  AdminProfileResponse,
  AdminUserInfo,
  LoginParams,
  LoginResponse,
  PaginatedList,
  RegisterParams,
  Response,
} from '@/types/api'
import request from '../request'

type UserListQuery = {
  pageNum: number
  pageSize: number
}

type UpdateStatusParams = {
  status: number
}

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
  getUserInfo(params: UserListQuery): Promise<Response<PaginatedList<AdminUserInfo>>> {
    return request.get('/api/admin/user/list', { params })
  },

  // 修改用户状态
  updateUserStatus(id: number, params: UpdateStatusParams): Promise<Response<null>> {
    return request.put(`/api/admin/user/${id}/status`, params)
  },

  // 获取管理员信息
  getAdminInfo(): Promise<Response<AdminProfileResponse>> {
    return request.get('/api/admin/auth/current')
  },
}
