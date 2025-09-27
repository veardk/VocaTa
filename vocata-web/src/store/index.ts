// stores/index.ts
import { createPinia } from 'pinia'

// 创建 pinia 实例
const pinia = createPinia()


// 导出 store
export * from './modules/chatHistory'

// 默认导出 pinia 实例
export default pinia