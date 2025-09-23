<template>
  <div :class="isM ? 'mobile' : 'pc'" class="login-page">
    <div class="login-box">
      <div class="left">
        <div class="title">
          <img src="@/assets/images/logo-text.png" alt="" />
        </div>
        <div class="main">
          <nav class="tab-nav">
            <div :class="{ active: activeTab === 'login' }" @click="activeTab = 'login'">登录</div>
            <div :class="{ active: activeTab === 'register' }" @click="activeTab = 'register'">
              注册
            </div>
          </nav>
          <el-form class="form">
            <template v-if="activeTab === 'login'">
              <el-input
                v-model="loginform.username"
                placeholder="请输入用户名"
                size="large"
              ></el-input>
              <el-input
                v-model="loginform.password"
                show-password
                placeholder="请输入密码"
                type="password"
                size="large"
              ></el-input>
              <el-checkbox v-model="loginform.remember" label="记住我"></el-checkbox>
              <div class="confirm">登录</div>
            </template>
            <template v-if="activeTab === 'register'">
              <el-input
                v-model="registerform.username"
                placeholder="请输入用户名"
                size="large"
              ></el-input>
              <el-input
                v-model="registerform.email"
                placeholder="请输入邮箱"
                size="large"
              ></el-input>
              <el-input
                v-model="registerform.password"
                placeholder="请输入密码"
                type="password"
                size="large"
              ></el-input>
              <el-input
                v-model="registerform.password2"
                placeholder="请确认密码"
                type="password"
                size="large"
              ></el-input>
              <el-input v-model="validCode" placeholder="请输入验证码" size="large">
                <template #append>
                  <el-button>{{ validText }}</el-button>
                </template>
              </el-input>
              <el-checkbox
                v-model="registerform.remember"
                label="我已阅读并同意用户协议和隐私政策"
              ></el-checkbox>
              <div class="confirm">注册</div>
            </template>
          </el-form>
        </div>
      </div>
      <div class="right" v-if="!isM"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, ref } from 'vue'
const isM = computed(() => isMobile())

const activeTab = ref('login')
const loginform = ref({
  username: '',
  password: '',
  remember: false,
})
const registerform = ref({
  username: '',
  password: '',
  password2: '',
  email: '',
  remember: false,
})
const validCode = ref('')
const validText = ref('发送')
</script>

<style lang="scss">
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

        &:hover {
          font-weight: bold;
        }
      }
      .active {
        border-bottom: 0.03rem solid #000;
        font-weight: bold;
      }
    }
    .form {
      // margin: 0.3rem 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      padding: 0.15rem 0.7rem;

      .el-input {
        margin-top: 0.3rem;
      }
      .el-checkbox {
        margin: 0.2rem 0;
      }
      .confirm {
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
        transition: all 0.3s;
        &:hover {
          background-color: #333;
        }
      }
    }
  }
  .right {
    background: url('../assets/images/loginPic.png') no-repeat;
    background-size: 100% 100%;
    flex: 1;
  }
}
</style>
