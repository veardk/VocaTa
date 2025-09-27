<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main">
    <div class="header">
      <div class="title">用户信息</div>
      <div class="close" @click="$emit('close')">
        <el-icon>
          <Close />
        </el-icon>
      </div>
    </div>

    <div class="main-part">
      <el-form ref="form" :model="form" label-width="80px" class="form">
        <el-upload
          class="avatar-uploader"
          :action="baseUrl + '/api/client/character/upload-avatar'"
          :headers="{ Authorization: 'Bearer ' + getToken() }"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
        >
          <img v-if="imageUrl" :src="imageUrl" class="avatar" />
          <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
        </el-upload>
        <span class="form-label"> 昵称</span>
        <input placeholder="例如：张三" v-model="form.nickname" class="form-input" />
        <span class="form-label"> 性别</span>
        <el-radio-group v-model="form.gender">
          <el-radio value="1">男</el-radio>
          <el-radio value="0">女</el-radio>
        </el-radio-group>
        <span class="form-label"> 电话</span>
        <input placeholder="你的电话" v-model="form.phone" class="form-input" />
        <span class="form-label">生日</span>
        <input type="date" v-model="form.birthday" class="form-input" />
        <div class="newBtn" type="primary">创建</div>
      </el-form>
    </div>
  </div>
  <div class="bgc"></div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { ElMessage, type UploadProps } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { getToken } from '@/utils/token'
import { userApi } from '@/api/modules/user'

const isM = computed(() => isMobile())

const baseUrl = import.meta.env.VITE_APP_URL
// 表单数据
const form = ref({
  nickname: '',
  gender: '',
  phone: '',
  birthday: '',
})

onMounted(() => {
  getUserInfo()
})

// 头像相关
const imageUrl = ref('')

const handleAvatarSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  imageUrl.value = URL.createObjectURL(uploadFile.raw!)
}

const beforeAvatarUpload: UploadProps['beforeUpload'] = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('头像必须是JPG或PNG格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 2) {
    ElMessage.error('头像大小不能超过2MB!')
    return false
  }
  return true
}
// 这里可以放全局逻辑
const getUserInfo = async () => {
  try {
    const res = await userApi.getUserInfo()
    if (res.code === 200) {
      form.value = res.data
    }
  } catch (error) {
    console.log(error)
  }
}
</script>

<style lang="scss" scoped>
.bgc {
  width: 100%;
  height: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: rgba(0, 0, 0, 0.2);
  z-index: 5;
}
.main {
  width: 50%;
  max-height: 70%;
  overflow-y: auto;
  background-color: #f5f5f5;
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
  border-radius: 0.1rem;
  border: 1px solid #ccc;
  display: flex;
  flex-direction: column;
  padding: 0.3rem;
  &.mobile {
    width: 95%;
    position: fixed;
    .top {
      margin: 0.1rem 0;
      .avatar {
        margin-right: 0.1rem;
      }
    }
    .main-part {
      padding: 0;
      .flex {
        margin: 0.1rem 0;
      }
      .label,
      .value,
      .tag {
        font-size: 0.16rem;
      }
    }
  }
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  .title {
    font-size: 0.3rem;
    font-weight: bold;
  }
  .close {
    cursor: pointer;
  }
}
.main-part {
  display: flex;
  flex-direction: column;
  width: 100%;
  padding: 0 0.2rem;

  .form {
    margin: 0.3rem auto;
    width: 95%;
    display: flex;
    flex-direction: column;
    align-items: start;
  }
  .form-label {
    align-self: start;
    font-size: 0.2rem;
    color: #666;
    margin: 0.1rem;
  }
  .form-input {
    width: 100%;
    height: 0.5rem;
    border: 0.01rem solid #ccc;
    border-radius: 0.1rem;
    padding: 0 0.1rem;
    font-size: 0.2rem;
    color: #333;
    &::placeholder {
      font-size: 0.2rem;
      color: #aaa;
    }
  }
  .form-textarea {
    width: 100%;
    border: 0.01rem solid #ccc;
    border-radius: 0.1rem;
    padding: 0.1rem;
    font-size: 0.2rem;
    color: #333;
    resize: none;
    &::placeholder {
      font-size: 0.2rem;
      color: #aaa;
    }
  }
  :deep(.el-checkbox__label) {
    font-size: 0.2rem;
  }
  :deep(.el-checkbox) {
    margin: 0.2rem 0.1rem;
    display: flex;
    align-items: center;
  }

  .avatar-uploader :deep(.el-upload) {
    border: 0.01rem dashed #999;
    border-radius: 50%;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    transition: var(--el-transition-duration-fast);
  }
  .avatar {
    width: 1rem;
    height: 1rem;
  }

  .avatar-uploader .el-upload:hover {
    border-color: var(--el-color-primary);
  }

  :deep(.el-icon).avatar-uploader-icon {
    font-size: 0.3rem;
    color: #888;
    width: 1rem;
    height: 1rem;
    text-align: center;
  }
  .newBtn {
    width: 20%;
    text-align: center;
    margin-top: 0.2rem;
    padding: 0.15rem 0;
    font-size: 0.24rem;
    background-color: #000;
    color: #fff;
    border-radius: 0.1rem;
    cursor: pointer;
    transition: all 0.3s;
    align-self: end;
    &:hover {
      background-color: #333;
    }
  }
  .required {
    &::before {
      content: '*';
      color: red;
      margin-right: 0.05rem;
    }
  }
}
</style>
