<template>
  <div :class="['login-page', layoutClass]">
    <div class="login-box">
      <div class="left">
        <div class="title">
          <img src="@/assets/images/logo-text.png" alt="语Ta Logo" />
        </div>

        <div class="main">
          <!-- 标签导航 -->
          <nav class="tab-nav">
            <div
              v-for="tab in tabs"
              :key="tab.value"
              :class="{ active: activeTab === tab.value }"
              @click="activeTab = tab.value"
            >
              {{ tab.label }}
            </div>
          </nav>

          <!-- 表单区域 -->
          <el-form class="form">
            <!-- 登录表单 -->
            <template v-if="isLoginTab">
              <el-input v-model="loginForm.loginName" placeholder="请输入用户名" size="large" />
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                size="large"
              />
              <el-checkbox v-model="loginForm.rememberMe" label="记住我" />
              <div
                class="confirm-btn"
                @click="handleLogin"
                v-loading.fullscreen.lock="fullscreenLoading"
                element-loading-text="请稍后..."
              >
                登录
              </div>
            </template>

            <!-- 注册表单 -->
            <template v-if="isRegisterTab">
              <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" />
              <el-input v-model="registerForm.email" placeholder="请输入邮箱" size="large" />
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
              />
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请确认密码"
                size="large"
              />
              <el-input
                v-model="registerForm.verificationCode"
                placeholder="请输入验证码"
                size="large"
              >
                <template #append>
                  <el-button @click="handleSendCode" :disabled="isCodeButtonDisabled">
                    {{ codeButtonText }}
                  </el-button>
                </template>
              </el-input>
              <el-checkbox
                v-model="registerForm.hasRead"
                label="我已阅读并同意用户协议和隐私政策"
              />
              <div
                class="confirm-btn"
                @click="handleRegister"
                v-loading.fullscreen.lock="fullscreenLoading"
                element-loading-text="请稍后..."
              >
                注册
              </div>
            </template>
          </el-form>
        </div>
      </div>

      <!-- 右侧图片（仅PC端显示） -->
      <div v-if="!isMobileDevice" class="right"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '@/api/modules/user'
import type { LoginParams, RegisterParams } from '@/types/api'
import { isMobile } from '@/utils/isMobile'
import { setToken } from '@/utils/token'

// 常量定义
const CODE_TIMEOUT = 60
const tabs = [
  { value: 'login', label: '登录' },
  { value: 'register', label: '注册' },
] as const

// 响应式数据
const isMobileDevice = computed(() => isMobile())
const layoutClass = computed(() => (isMobileDevice.value ? 'mobile' : 'pc'))
const activeTab = ref<'login' | 'register'>('login')

const loginForm = ref<LoginParams>({
  loginName: '',
  password: '',
  rememberMe: false,
})

const registerForm = ref<RegisterParams>({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  verificationCode: '',
  gender: 0,
  hasRead: false,
})

const codeButtonText = ref('发送')
const isCodeButtonDisabled = ref(false)
const fullscreenLoading = ref(false)
const countdownTimer = ref<number>()

// 计算属性
const isLoginTab = computed(() => activeTab.value === 'login')
const isRegisterTab = computed(() => activeTab.value === 'register')
const isPasswordMatch = computed(
  () => registerForm.value.password === registerForm.value.confirmPassword,
)

// 路由
const router = useRouter()

// 清理定时器
onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
})

// 登录相关方法

// 登录表单确认
const validateLoginForm = (): boolean => {
  if (!loginForm.value.loginName.trim()) {
    ElMessage.error('请输入用户名')
    return false
  }
  if (!loginForm.value.password.trim()) {
    ElMessage.error('请输入密码')
    return false
  }
  return true
}

// 登录
const handleLogin = async (): Promise<void> => {
  if (!validateLoginForm()) return

  try {
    fullscreenLoading.value = true
    const res = await userApi.login(loginForm.value)

    if (res.code === 200 && res.data) {
      setToken(res.data.token, res.data.expiresIn)
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error) {
    console.error('登录错误:', error)
    ElMessage.error('登录失败，请重试')
  } finally {
    fullscreenLoading.value = false
  }
}

// 注册相关方法

// 注册表单确认（发送验证码前）
const validateRegisterForm = (): boolean => {
  const { username, email, password, confirmPassword } = registerForm.value

  if (!username.trim()) {
    ElMessage.error('请输入用户名')
    return false
  }
  if (!email.trim()) {
    ElMessage.error('请输入邮箱')
    return false
  }
  if (!password.trim()) {
    ElMessage.error('请输入密码')
    return false
  }
  if (!confirmPassword.trim()) {
    ElMessage.error('请确认密码')
    return false
  }
  if (!isPasswordMatch.value) {
    ElMessage.error('两次密码不一致')
    return false
  }
  return true
}

// 发送验证码前验证
const validateBeforeSendCode = (): boolean => {
  if (!validateRegisterForm()) return false
  return true
}

// 发送验证码
const handleSendCode = async (): Promise<void> => {
  if (!validateBeforeSendCode()) return

  try {
    const res = await userApi.sendCode(registerForm.value.email)

    if (res.code === 200) {
      ElMessage.success('验证码发送成功')
      startCountdown()
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch (error) {
    console.error('发送验证码错误:', error)
    ElMessage.error('验证码发送失败，请重试')
    resetCodeButton()
  }
}

// 发送验证码读秒
const startCountdown = (): void => {
  let timeout = CODE_TIMEOUT
  isCodeButtonDisabled.value = true

  countdownTimer.value = setInterval(() => {
    codeButtonText.value = `${timeout}s后重新发送`
    timeout -= 1

    if (timeout < 0) {
      resetCodeButton()
    }
  }, 1000)
}

// 重置验证码按钮
const resetCodeButton = (): void => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
  codeButtonText.value = '发送'
  isCodeButtonDisabled.value = false
}

// 注册表单确认（发送验证码后）
const handleRegister = async (): Promise<void> => {
  if (!registerForm.value.verificationCode.trim()) {
    ElMessage.error('请输入验证码')
    return
  }
  if (!registerForm.value.hasRead) {
    ElMessage.error('请阅读并同意用户协议和隐私政策')
    return
  }
  try {
    fullscreenLoading.value = true
    const res = await userApi.register(registerForm.value)

    if (res.code === 200 && res.data) {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error) {
    console.error('注册错误:', error)
    ElMessage.error('注册失败，请重试')
  } finally {
    fullscreenLoading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f0f0;

  &.mobile {
    .login-box {
      width: 95%;
      height: 95%;
      overflow: auto;
    }

    .left {
      .title {
        padding: 0.2rem 0 0;
      }

      .form {
        padding: 0.1rem 0.5rem;
      }

      .tab-nav {
        padding: 0 0.3rem;
      }
    }
  }
}

.login-box {
  width: 70%;
  height: 70%;
  background-color: #fff;
  display: flex;
  border-radius: 0.2rem;
  overflow: hidden;
  box-shadow: 0.01rem 0.01rem 0.05rem 0.03rem #ddd;
}

.left {
  flex: 1;
  width: 50%;
  box-sizing: border-box;

  .title {
    font-size: 0.4rem;
    font-weight: bold;
    text-align: center;
    padding: 0.5rem 0 0;

    img {
      width: 1.2rem;
      height: 0.55rem;
      object-fit: contain;
    }
  }

  .tab-nav {
    display: flex;
    justify-content: center;
    padding: 0 0.5rem;

    div {
      width: 50%;
      text-align: center;
      padding: 0.1rem 0.2rem;
      font-size: 0.2rem;
      cursor: pointer;
      border-bottom: 0.01rem solid #ccc;
      transition: all 0.3s ease;

      &:hover {
        font-weight: bold;
        background-color: #f5f5f5;
      }

      &.active {
        border-bottom: 0.03rem solid #000;
        font-weight: bold;
        color: #000;
      }
    }
  }

  .form {
    display: flex;
    flex-direction: column;
    justify-content: center;
    padding: 0.15rem 0.7rem;

    :deep(.el-input__inner) {
      font-size: 0.16rem;
    }

    .el-input {
      margin-top: 0.3rem;
    }

    .el-checkbox {
      margin: 0.2rem 0;
    }

    .confirm-btn {
      width: 80%;
      text-align: center;
      margin: auto;
      height: 0.45rem;
      line-height: 0.45rem;
      font-size: 0.16rem;
      background-color: #000;
      color: #fff;
      border-radius: 0.1rem;
      cursor: pointer;
      transition: all 0.3s ease;

      &:hover {
        background-color: #333;
        transform: translateY(-1px);
      }

      &:active {
        transform: translateY(0);
      }
    }
  }
}

.right {
  flex: 1;
  background: url('../assets/images/loginPic.png') no-repeat center center;
  background-size: cover;
}
</style>
