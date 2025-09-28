<template>
  <div :class="isM ? 'mobile' : 'pc'" class="user-info-modal">
    <div class="modal-header">
      <span class="modal-title">用户信息</span>
      <div class="close-btn" @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </div>
    </div>

    <div class="modal-content">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <el-upload
          class="avatar-uploader"
          :action="baseUrl + '/api/client/file/upload'"
          :headers="{ Authorization: 'Bearer ' + getToken() }"
          :show-file-list="false"
          :on-success="handleAvatarSuccess"
          :before-upload="beforeAvatarUpload"
        >
          <div class="avatar-wrapper">
            <img v-if="form.avatar || imageUrl" :src="form.avatar || imageUrl" class="user-avatar" />
            <div v-else class="avatar-placeholder">
              <el-icon class="plus-icon"><Plus /></el-icon>
            </div>
          </div>
        </el-upload>
      </div>

      <!-- 表单区域 -->
      <div class="form-container">
        <!-- 昵称 -->
        <div class="form-group">
          <label class="form-label">昵称</label>
          <input
            v-model="form.nickname"
            class="form-input"
            placeholder="例如：张三"
            type="text"
          />
        </div>

        <!-- 性别 -->
        <div class="form-group">
          <label class="form-label">性别</label>
          <div class="gender-options">
            <label class="gender-item" :class="{ active: form.gender === 1 }" @click="form.gender = 1">
              <span class="radio-dot"></span>
              <span class="gender-text">男</span>
            </label>
            <label class="gender-item" :class="{ active: form.gender === 0 }" @click="form.gender = 0">
              <span class="radio-dot"></span>
              <span class="gender-text">女</span>
            </label>
          </div>
        </div>

        <!-- 电话 -->
        <div class="form-group">
          <label class="form-label">电话</label>
          <input
            v-model="form.phone"
            class="form-input"
            placeholder="你的电话"
            type="tel"
          />
        </div>

        <!-- 生日 -->
        <div class="form-group">
          <label class="form-label">生日</label>
          <input
            v-model="form.birthday"
            class="form-input date-input"
            placeholder="年/月/日"
            type="date"
          />
        </div>

        <!-- 保存按钮 -->
        <div class="form-actions">
          <button
            class="save-button"
            @click="saveUserInfo"
            :disabled="saving"
          >
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </div>
  <div class="modal-overlay" @click="$emit('close')"></div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { ElMessage, type UploadProps } from 'element-plus'
import { computed, onMounted, ref } from 'vue'
import { getToken } from '@/utils/token'
import { userApi } from '@/api/modules/user'
import type { UpdateUserInfoParams } from '@/types/api'

const isM = computed(() => isMobile())
const emit = defineEmits(['close'])

const baseUrl = import.meta.env.VITE_APP_URL
const saving = ref(false)

// 表单数据
const form = ref({
  nickname: '',
  gender: 1,
  phone: '',
  birthday: '',
  avatar: '',
})

onMounted(() => {
  getUserInfo()
})

// 头像相关
const imageUrl = ref('')

const handleAvatarSuccess: UploadProps['onSuccess'] = (response) => {
  if (response.code === 200) {
    form.value.avatar = response.data.url
    ElMessage.success('头像上传成功!')
  } else {
    ElMessage.error('头像上传失败!')
  }
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

// 获取用户信息
const getUserInfo = async () => {
  try {
    const res = await userApi.getUserInfo()
    if (res.code === 200) {
      const userData = res.data
      form.value = {
        nickname: userData.nickname || '',
        gender: userData.gender || 1,
        phone: userData.phone || '',
        birthday: userData.birthday || '',
        avatar: userData.avatar || '',
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败')
  }
}

// 保存用户信息
const saveUserInfo = async () => {
  if (!form.value.nickname?.trim()) {
    ElMessage.error('请输入昵称')
    return
  }

  saving.value = true
  try {
    const params: UpdateUserInfoParams = {
      nickname: form.value.nickname.trim(),
      gender: form.value.gender,
      phone: form.value.phone?.trim() || undefined,
      birthday: form.value.birthday || undefined,
      avatar: form.value.avatar || undefined,
    }

    const res = await userApi.updateUserInfo(params)
    if (res.code === 200) {
      ElMessage.success('保存成功!')
      emit('close')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error) {
    console.error('保存用户信息失败:', error)
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}
</script>

<style lang="scss" scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  z-index: 999;
}

.user-info-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 480px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;

  &.mobile {
    width: 90vw;
    max-width: 400px;
    max-height: 85vh;
    overflow-y: auto;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  border-bottom: 1px solid #f0f2f5;

  .modal-title {
    font-size: 20px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
  }

  .close-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s;

    &:hover {
      background-color: #f5f5f5;
    }

    .el-icon {
      font-size: 18px;
      color: #8c8c8c;
    }
  }
}

.modal-content {
  padding: 32px 28px 28px;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 36px;

  .avatar-uploader {
    :deep(.el-upload) {
      border: none;
      border-radius: 50%;
      cursor: pointer;
      overflow: hidden;
      transition: all 0.3s;

      &:hover {
        transform: scale(1.05);
      }
    }
  }

  .avatar-wrapper {
    position: relative;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    overflow: hidden;
    background-color: #f8f9fa;
    border: 3px solid #e9ecef;
    transition: border-color 0.3s;

    &:hover {
      border-color: #007bff;
    }
  }

  .user-avatar {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .avatar-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #f8f9fa;
  }

  .plus-icon {
    font-size: 28px;
    color: #adb5bd;
  }
}

.form-container {
  .form-group {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .form-label {
    display: block;
    margin-bottom: 10px;
    font-size: 15px;
    font-weight: 500;
    color: #333333;
    line-height: 1.4;
  }

  .form-input {
    width: 100%;
    height: 48px;
    padding: 0 16px;
    border: 2px solid #e1e5e9;
    border-radius: 8px;
    font-size: 15px;
    color: #333333;
    background-color: #ffffff;
    transition: all 0.3s;
    box-sizing: border-box;

    &:focus {
      outline: none;
      border-color: #007bff;
      box-shadow: 0 0 0 3px rgba(0, 123, 255, 0.1);
    }

    &::placeholder {
      color: #adb5bd;
    }

    &.date-input {
      position: relative;
    }
  }

  .gender-options {
    display: flex;
    gap: 32px;
  }

  .gender-item {
    display: flex;
    align-items: center;
    cursor: pointer;
    user-select: none;
    transition: all 0.3s;

    .radio-dot {
      position: relative;
      width: 18px;
      height: 18px;
      border: 2px solid #e1e5e9;
      border-radius: 50%;
      margin-right: 10px;
      transition: all 0.3s;

      &::after {
        content: '';
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 10px;
        height: 10px;
        background-color: #007bff;
        border-radius: 50%;
        opacity: 0;
        transition: opacity 0.3s;
      }
    }

    .gender-text {
      font-size: 15px;
      color: #333333;
    }

    &.active {
      .radio-dot {
        border-color: #007bff;

        &::after {
          opacity: 1;
        }
      }
    }

    &:hover {
      .radio-dot {
        border-color: #007bff;
      }
    }
  }

  .form-actions {
    margin-top: 40px;
    display: flex;
    justify-content: flex-end;
  }

  .save-button {
    padding: 12px 28px;
    background-color: #000000;
    color: #ffffff;
    border: none;
    border-radius: 8px;
    font-size: 15px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s;
    min-width: 100px;

    &:hover:not(:disabled) {
      background-color: #333333;
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    &:disabled {
      background-color: #e9ecef;
      color: #adb5bd;
      cursor: not-allowed;
      transform: none;
      box-shadow: none;
    }
  }
}

// 移动端适配
@media (max-width: 768px) {
  .user-info-modal {
    margin: 16px;
    width: calc(100vw - 32px);
    max-width: none;
  }

  .modal-header {
    padding: 20px 24px;

    .modal-title {
      font-size: 18px;
    }
  }

  .modal-content {
    padding: 28px 24px 24px;
  }

  .form-container {
    .form-group {
      margin-bottom: 20px;
    }

    .form-label {
      font-size: 14px;
    }

    .form-input {
      height: 44px;
      padding: 0 14px;
      font-size: 14px;
    }

    .gender-options {
      gap: 24px;
    }

    .save-button {
      padding: 10px 24px;
      font-size: 14px;
    }
  }

  .avatar-section {
    margin-bottom: 28px;

    .avatar-wrapper {
      width: 80px;
      height: 80px;
    }

    .plus-icon {
      font-size: 24px;
    }
  }
}
</style>