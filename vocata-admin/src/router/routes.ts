import type { RouteRecordRaw } from "vue-router"
import BasicLayout from '@/layouts/BasicLayout.vue'
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: BasicLayout,
    // redirect: '/dashboard',
  }
]
export default routes