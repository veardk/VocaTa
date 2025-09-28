
import request from '../request'

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
  getRoleList(params: any) {
    return request({
      url: '/api/admin/character',
      method: 'get',
      params
    })
  }
}