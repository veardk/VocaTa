import { createRouter, createWebHashHistory } from 'vue-router'
import routes from './routes.ts'
import guard from './guards'

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes
})
guard(router)
export default router
