<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">新建角色</div>
    <el-form label-width="80px" class="form">
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
        <el-option v-for="item in options" :key="item.name" :label="item.name" :value="item.name">
        </el-option>
      </el-select>

      <el-checkbox label="是否公开角色 " v-model="form.isPublic" />

      <span class="form-label">
        注意：系统会根据您填写的信息生成角色的提示词，如果希望自定义添加请填写提示词。
      </span>

      <span class="form-label"> Prompt提示词</span>
      <div class="prompt-container">
        <textarea
          placeholder="你的角色提示词"
          v-model="form.persona"
          class="form-textarea"
          rows="4"
        ></textarea>
        <div class="generate-btn" @click="generatePrompt" :disabled="isGenerating">
          {{ isGenerating ? '生成中...' : 'AI生成' }}
        </div>
      </div>
      <div class="newBtn" type="primary" @click="createRole">创建</div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadProps } from 'element-plus'
import { getToken } from '@/utils/token'
import { roleApi } from '@/api/modules/role'
import { chatHistoryStore } from '@/store'
import { useRouter } from 'vue-router'

const isM = computed(() => isMobile())
const baseUrl = import.meta.env.VITE_APP_URL
// 头像相关
const imageUrl = ref('')
// AI生成状态
const isGenerating = ref(false)
// 表单数据
const form = ref({
  name: '',
  description: '',
  greeting: '',
  isPublic: false,
  persona: '',
  voiceId: '',
  avatarUrl: '',
})

// 音色
const options = ref()
const router = useRouter()
//获取音色
const getVoice = async () => {
  const res = await roleApi.getSoundList()
  options.value = res.data
  console.log(options.value)
}
getVoice()

const handleAvatarSuccess: UploadProps['onSuccess'] = (response, uploadFile) => {
  imageUrl.value = response.data.fileUrl
  form.value.avatarUrl = response.data.fileUrl
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

// AI生成角色提示词
const generatePrompt = async () => {
  // 检查必填字段
  if (!form.value.name.trim()) {
    ElMessage.error('请先填写角色名称')
    return
  }
  if (!form.value.description.trim()) {
    ElMessage.error('请先填写角色描述')
    return
  }
  if (!form.value.greeting.trim()) {
    ElMessage.error('请先填写开场白')
    return
  }

  try {
    isGenerating.value = true
    ElMessage.info('正在生成角色提示词，请稍候...')

    const res = await roleApi.aiGenerate({
      name: form.value.name,
      description: form.value.description,
      greeting: form.value.greeting
    })

    if (res.code === 200) {
      // 将生成的persona填充到form中
      form.value.persona = res.data.persona
      ElMessage.success('AI生成完成！')
    } else {
      ElMessage.error(res.message || 'AI生成失败，请重试')
    }
  } catch (error) {
    console.error('AI生成出错:', error)
    ElMessage.error('AI生成出错，请检查网络连接后重试')
  } finally {
    isGenerating.value = false
  }
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
  if (res.code === 200) {
    ElMessage.success('创建成功')
  } else {
    ElMessage.error(res.message)
  }
  startConversation(res.data.id)
  // console.log('表单', form.value)
}

// 开始对话
const startConversation = async (characterId: string | number) => {
  try {
    console.log('点击开始对话，角色ID:', characterId)
    console.log('角色ID类型:', typeof characterId)

    if (!characterId) {
      console.error('角色ID为空')
      ElMessage.error('角色信息有误，请重试')
      return
    }

    // 添加加载状态
    const loadingMessage = ElMessage.info('正在创建对话...')

    // 调用创建对话接口，确保ID转换为字符串
    const conversationUuid = await chatHistoryStore().addChatHistory(characterId)

    loadingMessage.close()

    ElMessage.success('对话创建成功！')

    // 跳转到聊天页面
    router.push(`/chat/${conversationUuid}`)
  } catch (error) {
    console.error('创建对话失败:', error)
    ElMessage.error('创建对话失败，请稍后重试')
  }
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

.prompt-container {
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 0.1rem;
}

.generate-btn {
  align-self: flex-end;
  padding: 0.08rem 0.2rem;
  font-size: 0.18rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 0.08rem;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 0.04rem 0.15rem rgba(102, 126, 234, 0.4);

  &:hover:not([disabled]) {
    background: linear-gradient(135deg, #5a6fd8 0%, #6a4190 100%);
    box-shadow: 0 0.06rem 0.2rem rgba(102, 126, 234, 0.6);
    transform: translateY(-0.02rem);
  }

  &[disabled] {
    background: linear-gradient(135deg, #cccccc 0%, #999999 100%);
    cursor: not-allowed;
    box-shadow: none;
    transform: none;
  }
}
</style>