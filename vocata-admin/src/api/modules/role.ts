
import request from '../request'

type RoleListQuery = Record<string, string | number | boolean | undefined>

export const roleApi = {
  //增

  // 删
  deleteRole(id: number) {
    return request({
      url: `/api/admin/character/${id}`,
      method: 'delete'
    })
  },

  //改

  //查
  getRoleList(params: RoleListQuery) {
    return request({
      url: '/api/admin/character',
      method: 'get',
      params
    })
  }
}
