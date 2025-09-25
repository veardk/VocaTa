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

// 修改密码参数
export interface ChangePasswordParams {
  oldPassword: string,
  newPassword: string
}

// 返回参数
export interface Response<T> {
  code: number,
  message: string,
  data: T
}