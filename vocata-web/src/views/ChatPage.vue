<template>
  <div>
    <div :class="isM ? 'mobile' : 'pc'" class="main-container">
      <div class="chat-container">
        <div class="chat-item" v-for="(item, index) in chats" :key="index">
          <div v-if="item.type == 'receive'" class="receive">
            <div class="avatar"></div>
            <div class="right">
              <div class="name">AI</div>
              <div class="content">{{ item.content }}</div>
            </div>
          </div>
          <div v-if="item.type == 'send'" class="send">
            <div class="left">
              <div class="name">ME</div>
              <div class="content">{{ item.content }}</div>
            </div>
            <div class="avatar"></div>
          </div>
        </div>
      </div>
      <div class="input-container">
        <div class="send-box">
          <el-input
            type="textarea"
            v-model="input"
            :autosize="{ minRows: 1, maxRows: 5 }"
            placeholder="请输入内容"
            resize="none"
            class="chat-input"
          ></el-input>
          <button class="send-btn">
            <el-icon><Promotion /></el-icon>
          </button>
        </div>
        <button class="phone" @click="videoChat = !videoChat">
          <el-icon><PhoneFilled /></el-icon>
        </button>
      </div>
      <div class="video-chat" v-if="videoChat">
        <div class="ai-avatar">
          <div class="avatar"></div>
          <div class="loading"></div>
        </div>
        <div class="control">
          <div class="control-item">
            <el-icon><Microphone /></el-icon>
          </div>
          <div class="control-item close">
            <el-icon><Close /></el-icon>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
const isM = computed(() => isMobile())
const chats = ref([
  {
    type: 'receive',
    content: '你好，我是机器人，有什么可以帮助你的吗？',
  },
  {
    type: 'send',
    content: '你好，我想咨询一下关于贷款的问题。',
  },

  {
    type: 'receive',
    content:
      '我会根据您的需求为您推荐合适的贷款产品，请问您需要贷款多少？请问您需要贷款期限是多久？还有，请问您需要贷款的用途是什么？',
  },
  {
    type: 'send',
    content: '我想贷款10万元。',
  },
  {
    type: 'receive',
    content: '好的，请问您需要贷款期限是多久？',
  },
])
const input = ref('')
const router = useRouter()
const route = useRoute()
const videoChat = ref(false)
onMounted(() => {})
// 这里可以放全局逻辑
</script>

<style lang="scss" scoped>
.main-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  padding: 0.3rem 0.7rem;
  position: relative;
  &.mobile {
    height: calc(100vh - 0.6rem);
    padding: 0.1rem 0;
    width: 100%;

    .send-box {
      width: 100%;
    }
    .chat-input {
      width: 80%;
      padding: 0.05rem;
    }
    .phone {
      margin-left: 0.15rem;
    }
    .chat-container {
      width: 100%;
      padding: 0 0.1rem;
    }
    .avatar {
      width: 0.3rem;
      height: 0.3rem;
    }
    .left,
    .right {
      margin: 0 0.1rem;
    }
  }
}
.chat-container {
  width: 80%;
  flex: 1;
  overflow-y: auto;

  .chat-item {
    width: 100%;
    .receive {
      display: flex;
      width: 100%;
      justify-content: start;
      margin: 0.1rem 0;
    }
    .send {
      display: flex;
      justify-content: end;
      width: 100%;
      margin: 0.1rem 0;
    }
    .avatar {
      width: 0.5rem;
      height: 0.5rem;
      border-radius: 50%;
      background-color: #ddd;
    }
    .name {
      font-size: 0.14rem;
      color: #999;
    }
    .content {
      font-size: 0.16rem;
      color: #000;
      padding: 0.1rem 0.2rem;
      border-radius: 0.3rem;
      background-color: #f9f9f9;
      border: 0.01rem solid #eaeaea;
      margin-top: 0.1rem;
    }
    .left,
    .right {
      margin: 0 0.2rem;
      max-width: 50%;
    }
    .left {
      .name {
        text-align: right;
      }
      .content {
        border-top-right-radius: 0;
      }
    }
    .right {
      .content {
        border-top-left-radius: 0;
      }
    }
  }
}

.input-container {
  width: 80%;
  display: flex;
  justify-content: center;
  align-items: center;
  .send-box {
    border: 1px solid #ccc;
    width: 80%;
    // height: 0.5rem;
    border-radius: 0.3rem;
    overflow: hidden;
    display: flex;
    align-items: end;
    justify-content: center;
    .chat-input {
      resize: none;
      width: 90%;
      height: auto;
      padding: 0.1rem;
      border: none;
      &:focus {
        outline: none;
      }
      :deep(.el-textarea__inner) {
        background-color: transparent;
        font-size: 0.2rem;
        box-shadow: none;
      }
    }
    .send-btn {
      width: 0.5rem;
      height: 0.5rem;
      margin-bottom: 0.03rem;
      margin-left: 0.03rem;

      background-color: #000;
      border-radius: 50%;
      border: none;
      cursor: pointer;
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
      :deep(.el-icon) {
        font-size: 0.25rem;
        svg {
          font-size: 0.25rem;
          font-weight: bold;
        }
      }
    }
  }
  .phone {
    margin-left: 0.2rem;
    width: 0.5rem;
    height: 0.5rem;
    border-radius: 50%;
    background-color: #ddd;
    color: #000;
    display: flex;
    align-items: center;
    justify-content: center;
    border: none;
    cursor: pointer;
    &:hover {
      background-color: #ccc;
    }
    :deep(.el-icon) {
      font-size: 0.25rem;
      svg {
        font-size: 0.25rem;
        font-weight: bold;
      }
    }
  }
}

.video-chat {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(to top left, #fff, #fce9e9);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

  .header {
  }
  .ai-avatar {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    .avatar {
      width: 2rem;
      height: 2rem;
      border-radius: 50%;
      background-color: #ddd;
      margin: auto;
    }
  }
  .control {
    display: flex;
    justify-content: center;
    margin: 1rem auto;
    :deep(.el-icon) {
      font-size: 0.5rem;
      svg {
        font-size: 0.5rem;
      }
    }
    .control-item {
      background-color: #e5e5e5;
      width: 1rem;
      height: 1rem;
      border-radius: 50%;
      display: flex;
      justify-content: center;
      align-items: center;
      cursor: pointer;
      margin: 0 0.5rem;
      &:hover {
        background-color: #ddd;
      }
    }
    .close {
      color: #f00;
      font-weight: bold;
      margin-left: 1rem;
    }
  }
}
</style>
