import type { RouteRecordRaw } from "vue-router"
import BasicLayout from '@/layouts/BasicLayout.vue'
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: BasicLayout,
    redirect: '/searchRole',
    children: [
      {
        path: '/searchRole',
        component: () => import('@/views/SearchRole.vue'),
        meta: {
          title: '探索'
        }
      },
      {
        path: '/newRole',
        component: () => import('@/views/NewRole.vue'),
        meta: {
          title: '新建角色'
        }
      }, {
        path: '/chat/:id',
        component: () => import('@/views/ChatPage.vue'),
        meta: {
          title: '对话'
        }
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/LoginPage.vue'),
    meta: {
      title: '登录'
    }
  }
]

export default routes