---
title: Frontend Development Standards
description: "定义VocaTa前端开发的详细规范、架构模式和最佳实践。"
inclusion: always
---

# VocaTa前端开发规范

## 技术栈详情

### 核心框架和工具
- **Vue 3** - 采用Composition API作为主要开发模式
- **Element Plus** - UI组件库，提供丰富的界面组件
- **Axios** - HTTP客户端，处理API请求
- **SCSS** - CSS预处理器，支持变量和嵌套
- **Vite** - 现代前端构建工具，提供快速的开发体验
- **ESLint + Prettier** - 代码规范检查和自动格式化
- **Pinia** - Vue 3官方推荐的状态管理库
- **Vue Router 4** - Vue 3路由管理器

## 组件设计规范

### 通用组件设计原则
- **高内聚**：组件应专注于单一功能
- **低耦合**：组件应尽量减少对外界的依赖
- **可配置**：通过props提供灵活的配置选项
- **可扩展**：预留扩展点，方便定制化需求
- **可测试**：组件应易于单元测试

### 组件命名规范
- 组件文件名使用PascalCase格式：`UserProfile.vue`
- 组件名称与文件名保持一致
- 公共组件放在`components/common`目录下
- 业务组件放在`components/business`目录下，按业务模块组织

### 组件通信方式
- **父子组件通信**：使用props和emit
- **跨级组件通信**：使用provide/inject
- **全局状态共享**：使用Pinia
- **非父子组件通信**：使用事件总线或Pinia

## 样式规范

### SCSS组织方式
```scss
// 全局变量定义 - assets/styles/variables.scss
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;

// 字体变量
$font-size-small: 12px;
$font-size-base: 14px;
$font-size-large: 16px;

// 间距变量
$spacing-xs: 4px;
$spacing-sm: 8px;
$spacing-md: 16px;
$spacing-lg: 24px;
```

```scss
// 全局混合样式 - assets/styles/mixins.scss
@mixin flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

@mixin ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@mixin button-variant($color, $background, $border) {
  color: $color;
  background-color: $background;
  border-color: $border;
}
```

### BEM命名规范
使用BEM（Block Element Modifier）命名规范：
```scss
// Block: 独立的功能组件
.user-card { }

// Element: Block的子元素
.user-card__avatar { }
.user-card__name { }
.user-card__info { }

// Modifier: Block或Element的修饰符
.user-card--premium { }
.user-card__avatar--large { }
```

### 样式优先级管理
- 避免使用`!important`
- 使用更具体的选择器提高优先级
- 组件内样式通过`scoped`属性隔离
- 深度选择器使用`:deep()`语法

## 代码规范

### JavaScript/Vue规范

#### 变量命名
```javascript
// 常量使用UPPER_SNAKE_CASE
const MAX_RETRY_COUNT = 3;
const API_BASE_URL = 'https://api.vocata.com';

// 变量和函数使用camelCase
let userName = 'John';
const getUserInfo = () => {};

// 类名使用PascalCase
class UserService {}
```

#### Vue Composition API规范
```vue
<template>
  <div class="user-profile">
    <div class="user-profile__avatar">
      <img :src="userInfo.avatar" :alt="userInfo.name">
    </div>
    <div class="user-profile__info">
      <h3 class="user-profile__name">{{ userInfo.name }}</h3>
      <p class="user-profile__email">{{ userInfo.email }}</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useUserStore } from '@/store/modules/user'

// Props定义
const props = defineProps({
  userId: {
    type: String,
    required: true
  },
  showEmail: {
    type: Boolean,
    default: true
  }
})

// Emits定义
const emit = defineEmits(['update', 'delete'])

// 响应式数据
const userInfo = ref({})
const loading = ref(false)

// 计算属性
const displayName = computed(() => {
  return userInfo.value.name || '未知用户'
})

// 生命周期
onMounted(async () => {
  await fetchUserInfo()
})

// 方法
const fetchUserInfo = async () => {
  loading.value = true
  try {
    const response = await userApi.getUserInfo(props.userId)
    userInfo.value = response.data
  } catch (error) {
    console.error('获取用户信息失败:', error)
  } finally {
    loading.value = false
  }
}

const handleUpdate = () => {
  emit('update', userInfo.value)
}
</script>

<style lang="scss" scoped>
.user-profile {
  @include flex-center;
  padding: $spacing-md;

  &__avatar {
    margin-right: $spacing-md;

    img {
      width: 60px;
      height: 60px;
      border-radius: 50%;
    }
  }

  &__info {
    flex: 1;
  }

  &__name {
    margin: 0 0 $spacing-sm 0;
    font-size: $font-size-large;
    font-weight: 600;
  }

  &__email {
    margin: 0;
    color: #666;
    font-size: $font-size-small;
  }
}
</style>
```

## 核心模块设计

### 入口文件配置
```javascript
// main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/styles/index.scss'
import { setupAxios } from './api/request'

const app = createApp(App)

// 配置Axios
setupAxios(app)

// 安装插件
app.use(router)
app.use(store)
app.use(ElementPlus)

// 挂载应用
app.mount('#app')
```

### API模块设计
```javascript
// api/request.js - Axios配置
import axios from 'axios'
import { ElMessage, ElLoading } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import router from '@/router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

let loadingInstance = null
let requestCount = 0

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 显示加载状态
    requestCount++
    if (!loadingInstance) {
      loadingInstance = ElLoading.service({
        lock: true,
        text: '加载中...',
        background: 'rgba(0, 0, 0, 0.1)'
      })
    }

    // 添加token
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    requestCount--
    if (requestCount === 0 && loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }
    ElMessage.error('请求错误: ' + error.message)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response) => {
    requestCount--
    if (requestCount === 0 && loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }

    const res = response.data

    if (res.code !== 200) {
      ElMessage.error(res.message || '操作失败')

      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/auth/login')
      }

      return Promise.reject(new Error(res.message || 'Error'))
    }

    return res
  },
  (error) => {
    requestCount--
    if (requestCount === 0 && loadingInstance) {
      loadingInstance.close()
      loadingInstance = null
    }

    ElMessage.error('网络错误: ' + (error.message || '连接失败'))
    return Promise.reject(error)
  }
)

export default service
```

```javascript
// api/modules/user.js - 用户API
import request from '../request'

export const userApi = {
  /**
   * 用户登录
   */
  login: (data) => {
    return request.post('/api/client/auth/login', data)
  },

  /**
   * 用户注册
   */
  register: (data) => {
    return request.post('/api/client/auth/register', data)
  },

  /**
   * 获取用户信息
   */
  getUserInfo: () => {
    return request.get('/api/client/user/info')
  },

  /**
   * 更新用户信息
   */
  updateUserInfo: (data) => {
    return request.put('/api/client/user/info', data)
  }
}
```

### 路由设计
```javascript
// router/routes.js
export const routes = [
  {
    path: '/',
    name: 'MainLayout',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home/index.vue'),
        meta: {
          title: '首页',
          icon: 'home'
        }
      },
      {
        path: '/characters',
        name: 'CharacterList',
        component: () => import('@/views/Character/List.vue'),
        meta: {
          title: '角色列表',
          icon: 'user'
        }
      },
      {
        path: '/characters/:id',
        name: 'CharacterDetail',
        component: () => import('@/views/Character/Detail.vue'),
        meta: {
          title: '角色详情'
        }
      },
      {
        path: '/conversations',
        name: 'ConversationList',
        component: () => import('@/views/Conversation/List.vue'),
        meta: {
          title: '对话历史',
          icon: 'chat'
        }
      },
      {
        path: '/chat/:characterId',
        name: 'Chat',
        component: () => import('@/views/Conversation/Chat.vue'),
        meta: {
          title: '对话'
        }
      }
    ]
  },
  {
    path: '/auth',
    name: 'AuthLayout',
    component: () => import('@/layouts/AuthLayout.vue'),
    children: [
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/User/Login.vue'),
        meta: {
          title: '登录'
        }
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/User/Register.vue'),
        meta: {
          title: '注册'
        }
      }
    ]
  }
]
```

### 状态管理设计
```javascript
// store/modules/user.js - Pinia用户状态
import { defineStore } from 'pinia'
import { userApi } from '@/api/modules/user'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getStorage('user_info') || null,
    token: getStorage('token') || null
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    userId: (state) => state.userInfo?.id,
    userName: (state) => state.userInfo?.name
  },

  actions: {
    async login(credentials) {
      try {
        const res = await userApi.login(credentials)
        const { token, userInfo } = res.data

        this.token = token
        this.userInfo = userInfo

        setStorage('token', token)
        setStorage('user_info', userInfo)

        return res
      } catch (error) {
        console.error('登录失败:', error)
        throw error
      }
    },

    logout() {
      this.token = null
      this.userInfo = null

      removeStorage('token')
      removeStorage('user_info')
    },

    async fetchUserInfo() {
      try {
        const res = await userApi.getUserInfo()
        this.userInfo = res.data
        setStorage('user_info', res.data)
        return res
      } catch (error) {
        console.error('获取用户信息失败:', error)
        throw error
      }
    }
  }
})
```

## 性能优化策略

### 代码分割
```javascript
// 路由懒加载
const Home = () => import('@/views/Home/index.vue')
const UserProfile = () => import('@/views/User/Profile.vue')

// 组件懒加载
const LazyComponent = defineAsyncComponent(() =>
  import('@/components/business/CharacterCard.vue')
)
```

### 图片优化
```vue
<template>
  <!-- 懒加载图片 -->
  <img
    v-lazy="imageUrl"
    :alt="imageAlt"
    loading="lazy"
  />

  <!-- 响应式图片 -->
  <picture>
    <source media="(max-width: 768px)" :srcset="mobileImage">
    <source media="(max-width: 1024px)" :srcset="tabletImage">
    <img :src="desktopImage" :alt="imageAlt">
  </picture>
</template>
```

### 虚拟列表处理
```vue
<!-- 大数据列表优化 -->
<template>
  <virtual-list
    :data-key="'id'"
    :data-sources="largeDataList"
    :data-component="itemComponent"
    :keeps="30"
    :estimate-size="80"
  />
</template>
```

## 构建与部署

### Vite配置
```javascript
// vite.config.js
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src')
      }
    },
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: env.VITE_API_BASE_URL,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    },
    build: {
      outDir: 'dist',
      sourcemap: mode === 'development',
      rollupOptions: {
        output: {
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: '[ext]/[name]-[hash].[ext]'
        }
      }
    }
  }
})
```

### 环境配置与安全策略

#### 环境变量文件结构
```
vocata-web/
├── .env.example               # 环境变量模板（提交到git）
├── .env.development          # 本地开发环境（不提交到git）
├── .env.staging              # 测试环境（不提交到git）
├── .env.production           # 生产环境（不提交到git）
└── .gitignore                # 忽略敏感环境文件
```

#### 安全的环境变量配置
```bash
# .env.example - 环境变量模板
VITE_API_BASE_URL=http://localhost:9009
VITE_APP_TITLE=VocaTa
VITE_APP_ENV=development
VITE_APP_DEBUG=true

# .env.development - 本地开发环境
VITE_API_BASE_URL=http://localhost:9009
VITE_APP_TITLE=VocaTa开发环境
VITE_APP_ENV=development
VITE_APP_DEBUG=true

# .env.staging - 测试环境（使用GitHub Secrets）
VITE_API_BASE_URL=https://${STAGING_HOST}
VITE_APP_TITLE=VocaTa测试环境
VITE_APP_ENV=staging
VITE_APP_DEBUG=false

# .env.production - 生产环境（使用GitHub Secrets）
VITE_API_BASE_URL=https://${PRODUCTION_HOST}
VITE_APP_TITLE=VocaTa
VITE_APP_ENV=production
VITE_APP_DEBUG=false
```

#### .gitignore 配置
```bash
# 环境配置文件 - 保护敏感信息
.env.local
.env.development
.env.staging
.env.test
.env.production
.env

# 但保留模板文件
!.env.example
```

#### TypeScript 环境配置支持
```typescript
// src/config/env.ts
interface EnvConfig {
  apiBaseUrl: string
  appTitle: string
  appEnv: string
  debug: boolean
}

const config: EnvConfig = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9009',
  appTitle: import.meta.env.VITE_APP_TITLE || 'VocaTa',
  appEnv: import.meta.env.VITE_APP_ENV || 'development',
  debug: import.meta.env.VITE_APP_DEBUG === 'true'
}

export default config
```

#### API 请求配置使用
```typescript
// api/request.js 使用配置
import config from '@/config/env'

const api = axios.create({
  baseURL: config.apiBaseUrl,
  timeout: 10000
})
```

### GitHub Actions CI/CD 配置

#### 客户端前端部署 (.github/workflows/deploy-web.yml)
```yaml
name: Deploy Web Frontend

on:
  push:
    branches: [master, develop]
    paths: ['vocata-web/**']

jobs:
  deploy-staging:
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: vocata-web/package-lock.json

      - name: Install dependencies
        run: |
          cd vocata-web
          npm ci

      - name: Build for staging
        env:
          VITE_API_BASE_URL: https://${{ secrets.STAGING_HOST }}
          VITE_APP_TITLE: VocaTa测试环境
          VITE_APP_ENV: staging
          VITE_APP_DEBUG: false
        run: |
          cd vocata-web
          echo "Building with API URL: $VITE_API_BASE_URL"  # 验证注入成功
          npm run build

      - name: Deploy to staging
        # 部署到测试服务器的步骤

  deploy-production:
    if: github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: vocata-web/package-lock.json

      - name: Install dependencies
        run: |
          cd vocata-web
          npm ci

      - name: Build for production
        env:
          VITE_API_BASE_URL: https://${{ secrets.PRODUCTION_HOST }}
          VITE_APP_TITLE: VocaTa
          VITE_APP_ENV: production
          VITE_APP_DEBUG: false
        run: |
          cd vocata-web
          echo "Building with API URL: $VITE_API_BASE_URL"  # 验证注入成功
          npm run build

      - name: Deploy to production
        # 部署到生产服务器的步骤
```

#### 管理后台部署 (.github/workflows/deploy-admin.yml)
```yaml
name: Deploy Admin Frontend

on:
  push:
    branches: [master, develop]
    paths: ['vocata-admin/**']

jobs:
  deploy-staging:
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: vocata-admin/package-lock.json

      - name: Install dependencies
        run: |
          cd vocata-admin
          npm ci

      - name: Build for staging
        env:
          VITE_API_BASE_URL: https://${{ secrets.STAGING_HOST }}
          VITE_APP_TITLE: VocaTa管理后台测试环境
          VITE_APP_ENV: staging
        run: |
          cd vocata-admin
          npm run build:staging

  deploy-production:
    if: github.ref == 'refs/heads/master'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: vocata-admin/package-lock.json

      - name: Install dependencies
        run: |
          cd vocata-admin
          npm ci

      - name: Build for production
        env:
          VITE_API_BASE_URL: https://${{ secrets.PRODUCTION_HOST }}
          VITE_APP_TITLE: VocaTa管理后台
          VITE_APP_ENV: production
        run: |
          cd vocata-admin
          npm run build
```

#### 前端构建脚本配置
```json
// package.json 构建脚本
{
  "scripts": {
    "dev": "vite --mode development",
    "build": "vite build --mode production",
    "build:staging": "vite build --mode staging",
    "build:test": "vite build --mode staging",
    "preview": "vite preview"
  }
}
```

#### 本地开发环境搭建流程
```bash
# 1. 首次设置
cp .env.example .env.development

# 2. 编辑本地配置
vim .env.development

# 3. 启动开发
npm run dev  # 自动使用 .env.development

# 4. 构建测试
npm run build:staging  # 使用 .env.staging 配置
```

#### 环境变量安全优势
1. **敏感信息保护**: 真实服务器地址只存在于GitHub Secrets中
2. **环境隔离**: 不同环境使用不同的配置
3. **版本控制安全**: .env文件不提交到git，避免泄露
4. **CI/CD自动化**: 部署时自动注入正确的环境变量并验证
```javascript
// .eslintrc.js
module.exports = {
  extends: [
    '@vue/standard',
    'plugin:vue/vue3-recommended'
  ],
  rules: {
    'vue/multi-word-component-names': 'off',
    'vue/require-default-prop': 'off',
    'space-before-function-paren': 'off'
  }
}
```

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "printWidth": 100,
  "trailingComma": "none"
}
```

## 测试策略

### 单元测试
```javascript
// 组件测试示例
import { mount } from '@vue/test-utils'
import UserProfile from '@/components/business/UserProfile.vue'

describe('UserProfile.vue', () => {
  it('renders user info correctly', () => {
    const wrapper = mount(UserProfile, {
      props: {
        userId: '123',
        userInfo: {
          name: 'Test User',
          email: 'test@example.com'
        }
      }
    })

    expect(wrapper.find('.user-profile__name').text()).toBe('Test User')
    expect(wrapper.find('.user-profile__email').text()).toBe('test@example.com')
  })
})
```

### E2E测试
```javascript
// Cypress端到端测试
describe('User Login', () => {
  it('should login successfully', () => {
    cy.visit('/auth/login')
    cy.get('[data-cy=username]').type('testuser')
    cy.get('[data-cy=password]').type('password')
    cy.get('[data-cy=submit]').click()
    cy.url().should('include', '/')
  })
})
```