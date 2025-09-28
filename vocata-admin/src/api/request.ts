// src/utils/request.js
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/token'
import router from '@/router'

// 创建axios实例
const request = axios.create({
  baseURL: import.meta.env.VITE_APP_URL, // 从环境变量读取
  // baseURL: 'http://127.0.0.1:4523/m1/7166225-6890394-default/', // 从环境变量读取
  timeout: 10000 // 请求超时时间
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    if (getToken()) {
      config.headers['Authorization'] = 'Bearer ' + getToken()
    }
    return config
  },
  (error) => {
    console.error('API请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data

    // // 根据你的后端接口约定修改判断逻辑
    // if (res.code === 200) {
    return res
    // } else {
    //   ElMessage.error(res.message || '请求失败')
    //   return Promise.reject(new Error(res.message || 'Error'))
    // }
  },
  (error) => {
    // 处理HTTP错误状态码
    let message = ''
    if (error.response) {
      switch (error.response.status) {
        case 401:
          message = '未授权，请重新登录'
          removeToken()
          // 跳转到登录页
          router.push('/login')
          break
        case 403:
          message = '拒绝访问'
          break
        case 404:
          message = '请求地址错误'
          break
        case 500:
          message = '服务器内部错误'
          break
        default:
          message = '网络错误'
      }
    } else {
      message = '未知错误'
    }

    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request