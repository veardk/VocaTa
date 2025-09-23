// src/utils/isMobile.js - 基于userAgent的简版

// 判断是否为移动设备
export const isMobile = () => {
  const userAgent = navigator.userAgent.toLowerCase()
  return /iphone|ipod|android.*mobile|windows.*phone|blackberry.*mobile/i.test(userAgent)
}

// 直接导出一个布尔值（当前状态）
export const isMobileNow = isMobile()