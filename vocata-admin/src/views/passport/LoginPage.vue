<template>
  <main class="md:w-2/5 w-full mx-auto bg-white position md:mt-30 mt-10 p-7 rounded-lg shadow-md">
    <div class="text-center font-bold text-2xl">登录</div>

    <el-form label-width="80px">
      <el-form-item label-width="0" class="mt-3">
        <el-input
          name="account"
          placeholder="请输入账号"
          size="large"
          v-model="apiForm.loginName"
        ></el-input>
      </el-form-item>
      <el-form-item label-width="0">
        <el-input
          name="pwd"
          type="password"
          placeholder="请输入登录密码"
          size="large"
          v-model="apiForm.password"
        ></el-input>
      </el-form-item>
      <el-form-item label-width="0" class="flex">
        <el-button type="default" size="large" class="w-full" @click="activedBtn"
          >点击按钮验证</el-button
        >
      </el-form-item>
      <el-form-item label-width="0" class="mt-8">
        <el-button
          type="primary"
          size="large"
          class="w-full mx-auto"
          :disabled="!btnState"
          @click="login"
          v-loading.fullscreen.lock="fullscreenLoading"
          element-loading-text="请稍后..."
          >登录</el-button
        >
      </el-form-item>
    </el-form>
  </main>
</template>

<script setup lang="ts">
import { userApi } from '@/api/modules/user'
import { setToken } from '@/utils/token'
import { ElMessage, ElNotification } from 'element-plus'
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const router = useRouter()
const route = useRoute()
const btnState = ref(false)
const apiForm = reactive({
  loginName: '',
  password: '',
  rememberMe: false,
})
const fullscreenLoading = ref(false)
const activedBtn = () => {
  if (apiForm.password !== '') {
    btnState.value = true
  } else {
    ElMessage({
      message: '请先填写完表单',
      type: 'error',
    })
  }
}

const login = async () => {
  if (apiForm.password == '' || apiForm.password == '') {
    ElMessage({
      message: '请先填写完表单',
      type: 'error',
    })
  } else {
    fullscreenLoading.value = true
    const res = await userApi.login(apiForm)
    console.log(res)

    if (res.message == '登录成功') {
      ElNotification({
        title: '登录成功',
        message: '欢迎用户，' + res.data.user.nickname + '！',
        type: 'success',
      })
      setToken(res.data.token, res.data.expiresIn)
      router.push('/role')
    } else {
      ElMessage({
        message: '登录失败',
        type: 'error',
      })
    }
    fullscreenLoading.value = false
  }
}
</script>
