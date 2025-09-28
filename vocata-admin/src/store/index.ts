
import routes from '@/router/routes'
import { defineStore } from 'pinia'
export const user = defineStore('user', {
  state: () => {
    return {
      menuRoutes: routes,
    }
  }
})