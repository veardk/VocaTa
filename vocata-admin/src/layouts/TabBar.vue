<template>
  <div class="h-full flex items-center justify-between text-4xl">
    <div class="cursor-pointer" @click="toggleCollapse">
      <el-icon>
        <Fold />
      </el-icon>
    </div>
    <div class="flex gap-8">
      <el-icon>
        <Setting />
      </el-icon>
      <el-icon @click="fullScreen" class="cursor-pointer">
        <FullScreen />
      </el-icon>
      <el-icon @click="$emit('refresh')" class="cursor-pointer">
        <RefreshRight />
      </el-icon>
      <el-popover placement="bottom" trigger="click">
        <template #reference>
          <div class="flex gap-3 items-center cursor-pointer">
            <span class="text-2xl">{{ userInfo.nickname }}</span>

            <div class="w-10 h-10 bg-pink-200 rounded-full" @click="showPop = !showPop">
              <el-avatar :src="userInfo.avatar"></el-avatar>
            </div>
          </div>
        </template>
        <div
          class="text-2xl bg-white p-2 hover:bg-gray-200 cursor-pointer"
          @click="logout"
          v-loading.fullscreen.lock="fullscreenLoading"
          element-loading-text="退出中..."
        >
          退出登录
        </div>
      </el-popover>
    </div>
  </div>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { removeToken } from '@/utils/token'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
const fullscreenLoading = ref(false)
const props = defineProps(['modelValue'])
const emit = defineEmits(['update:modelValue'])

const showPop = ref(false)
const router = useRouter()
const userInfo = ref({
  nickname: '',
  avatar: '',
})
const toggleCollapse = () => {
  // 使用 update:modelValue 事件更新 v-model
  emit('update:modelValue', !props.modelValue)
}

const getUserInfo = async () => {
  try {
    const res = await userApi.getAdminInfo()
    if (res.code === 200) {
      userInfo.value = res.data.user
    }
    console.log(userInfo.value)
  } catch (error) {
    console.log(error)
  }
}
getUserInfo()
const logout = async () => {
  try {
    fullscreenLoading.value = true
    const res = await userApi.logout()
    if (res.code === 200) {
      removeToken()
      ElMessage.success('退出成功')
      router.push('/passport/login')
    }
  } catch (error) {
    console.log(error)
    ElMessage.error('退出失败')
  } finally {
    fullscreenLoading.value = false
  }
}
// 全屏功能
const fullScreen = () => {
  const full = document.fullscreenElement
  if (!full) {
    document.documentElement.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}
</script>
<style scoped></style>
