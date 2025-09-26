<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">新建角色</div>
    <el-form ref="form" :model="form" label-width="80px" class="form">
      <el-upload
        class="avatar-uploader"
        action=""
        :show-file-list="false"
        :on-success="handleAvatarSuccess"
        :before-upload="beforeAvatarUpload"
      >
        <img v-if="imageUrl" :src="imageUrl" class="avatar" />
        <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
      </el-upload>
      <span class="form-label"> 角色名称：</span>
      <input placeholder="例如：张三" class="form-input" />
      <span class="form-label"> 简介：</span>
      <input placeholder="您的角色信息" class="form-input" />
      <span class="form-label"> 描述：</span>
      <textarea placeholder="角色介绍" class="form-textarea" rows="4"></textarea>
      <span class="form-label"> 开场白：</span>
      <input placeholder="你好呀！" class="form-input" />
      <el-checkbox label="是否公开角色" />
      <div class="newBtn" type="primary">创建</div>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
const isM = computed(() => isMobile())
// 这里可以放全局逻辑
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
  width: 50%;
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
</style>
