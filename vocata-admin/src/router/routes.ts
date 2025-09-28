import type { RouteRecordRaw } from "vue-router"
import BasicLayout from '@/layouts/BasicLayout.vue'
const routes: RouteRecordRaw[] = [
  // 登录模块
  {
    path: '/passport',
    name: 'Passport',
    component: () => import('@/views/passport/PassPort.vue'),
    meta: { title: '通行证', hidden: true },
    children: [
      {
        path: '/passport/login',
        name: 'Login',
        component: () => import('@/views/passport/LoginPage.vue'),
        meta: { title: '登录', hidden: true }
      }
    ]
  },
  // 角色管理模块
  {
    path: '/role',
    name: 'Role',
    component: BasicLayout,

    meta: { title: '角色管理', icon: 'User' },
    children: [
      {
        path: '/role/roles',
        name: 'Roles',
        component: () => import('@/views/RolePage.vue'),
        meta: { title: '角色管理', icon: 'User' }
      }
    ]
  },
  // 角色管理模块
  {
    path: '/user',
    name: 'User',
    component: BasicLayout,

    meta: { title: '用户管理', icon: 'User' },
    children: [
      {
        path: '/user/users',
        name: 'Users',
        component: () => import('@/views/UserPage.vue'),
        meta: { title: '用户管理', icon: 'User' }
      }
    ]
  },
  // 根路由
  {
    path: '/',
    name: 'Root',
    component: () => import('@/layouts/BasicLayout.vue'),
    meta: { title: '首页', hidden: true },
    redirect: '/role/roles'
  },
  // 404页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/ErrorPage.vue'),
    meta: { title: '页面不存在', hidden: true }
  },
]
export default routes