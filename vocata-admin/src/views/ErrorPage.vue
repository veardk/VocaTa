<template>
  <div
    class="min-h-screen bg-gradient-to-b from-slate-50 to-slate-100 flex flex-col items-center justify-center overflow-hidden relative"
  >
    <!-- 背景装饰元素 -->
    <div
      class="absolute -top-24 -left-24 w-96 h-96 bg-pink-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"
    ></div>
    <div
      class="absolute -bottom-24 -right-24 w-96 h-96 bg-blue-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"
    ></div>
    <div
      class="absolute top-1/3 -right-24 w-72 h-72 bg-yellow-200 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"
    ></div>

    <!-- 404 主内容卡片 -->
    <div
      class="relative w-full max-w-md bg-white rounded-2xl shadow-xl p-8 md:p-10 z-10 transform transition-all duration-500 hover:shadow-2xl"
    >
      <!-- 404 数字展示 -->
      <div class="flex justify-center mb-6">
        <div class="relative">
          <span class="text-[100px] font-black text-slate-100 absolute -inset-1">404</span>
          <span
            class="text-[100px] font-black bg-clip-text text-transparent bg-gradient-to-r from-rose-500 to-violet-500 relative"
            >404</span
          >
        </div>
      </div>

      <!-- 信息提示 -->
      <div class="text-center mb-8">
        <h2 class="text-2xl md:text-3xl font-bold text-slate-800 mb-3">页面迷路了</h2>
        <p class="text-slate-600">
          抱歉，您访问的页面不存在或已被移动。<br />请检查网址是否正确，或返回首页重新开始。
        </p>
      </div>

      <!-- 返回首页按钮 -->
      <button
        @click="goToHome"
        class="cursor-pointer w-full py-3 px-6 bg-gradient-to-r from-rose-500 to-rose-600 text-white font-medium rounded-lg shadow-md hover:shadow-lg hover:from-rose-600 hover:to-rose-700 transition-all duration-300 transform hover:-translate-y-0.5 active:translate-y-0 flex items-center justify-center gap-2"
      >
        <span>返回首页</span>
        <svg
          class="w-5 h-5 transition-transform group-hover:translate-x-1"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M10 19l-7-7m0 0l7-7m-7 7h18"
          ></path>
        </svg>
      </button>
    </div>

    <!-- 底部辅助信息 -->
    <div class="mt-6 text-slate-500 text-sm z-10">
      <p>错误代码: 404 | 最后检查: {{ new Date().toLocaleDateString() }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

// 跳转到首页
const goToHome = () => {
  router.push('/')
}
</script>

<style scoped>
/* 自定义背景动画 */
@keyframes blob {
  0% {
    transform: translate(0px, 0px) scale(1);
  }

  33% {
    transform: translate(30px, -50px) scale(1.1);
  }

  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }

  100% {
    transform: translate(0px, 0px) scale(1);
  }
}

.animate-blob {
  animation: blob 7s infinite;
}

.animation-delay-2000 {
  animation-delay: 2s;
}

.animation-delay-4000 {
  animation-delay: 4s;
}

/* 按钮悬停效果增强
button {
  @apply group;
} */
</style>
