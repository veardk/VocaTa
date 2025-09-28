import { getToken } from '@/utils/token'
import { ElMessage } from 'element-plus'
import type { Router } from 'vue-router'

const whiteList = ['/passport/login', '/404']
export default function setupRouterGuard(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const token = getToken()

    // 白名单检查
    if (whiteList.includes(to.path)) {
      // 如果已经登录，访问登录页时重定向到首页
      if (token && to.path === '/passport/login') {
        next('/')
        return
      }
      next()
      return
    }

    // 非白名单路径检查token
    if (!token) {
      ElMessage.warning('登录过期，请重新登录')
      next(`/passport/login?redirect=${encodeURIComponent(to.fullPath)}`)
      return
    }
    next()
  })
}