<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">新建角色</div>
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
      <span class="form-label required"> 角色名称</span>
      <input placeholder="例如：张三" v-model="form.name" class="form-input" />
      <span class="form-label required"> 描述</span>
      <textarea
        placeholder="你的角色信息"
        v-model="form.description"
        class="form-textarea"
        rows="4"
      ></textarea>
      <span class="form-label required"> 开场白</span>
      <input placeholder="你好呀！" v-model="form.greeting" class="form-input" />
      <span class="form-label required">角色声音</span>
      <el-select v-model="form.voiceId" placeholder="请选择角色声音">
        <el-option v-for="item in options" :key="item.id" :label="item.name" :value="item.name">
        </el-option>
      </el-select>
      <el-checkbox label="是否公开角色 " v-model="form.isPublic" />
      <span class="form-label">
        注意：系统会根据您填写的信息生成角色的提示词，如果希望自定义添加请填写提示词。
      </span>
      <span class="form-label"> Prompt提示词</span>
      <textarea placeholder="你的角色提示词" class="form-textarea" rows="4"></textarea>
      <div class="newBtn" type="primary" @click="createRole">创建</div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadProps } from 'element-plus'
import { getToken } from '@/utils/token'
import { roleApi } from '@/api/modules/role'

const isM = computed(() => isMobile())
const baseUrl = import.meta.env.VITE_APP_URL
// 表单数据
const form = ref({
  name: '',
  description: '',
  greeting: '',
  isPublic: false,
  persona: '',
  voiceId: '',
})

// 音色
const options = ref([])

onMounted(() => {
  getVoice()
})

//获取音色
const getVoice = async () => {
  const res = await roleApi.getSoundList()
  options.value = res.data
}

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

// 创建角色
const createRole = async () => {
  if (form.value.name === '') {
    ElMessage.error('请输入角色名称')
    return
  }
  if (form.value.description === '') {
    ElMessage.error('请输入角色描述')
    return
  }
  if (form.value.greeting === '') {
    ElMessage.error('请输入角色开场白')
    return
  }
  if (form.value.voiceId === '') {
    ElMessage.error('请选择角色声音')
    return
  }
  const res = await roleApi.createRole(form.value)
}
</script>

<style lang="scss" scoped>
.main-container {
  padding: 0.3rem 0.5rem;
  &.mobile {
    padding: 0;
    .header {
      padding: 0 0.2rem 0;
    }
    .form {
      width: 80%;
    }
    .newBtn {
      width: 50%;
      padding: 0.1rem 0;
    }
  }
}
.header {
  font-size: 0.3rem;
  font-weight: bold;
  color: #333;
  // margin-bottom: 0.3rem;
}
.form {
  margin: 0.3rem auto;
  width: 55%;
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
</style>
