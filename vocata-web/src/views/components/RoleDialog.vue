<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main">
    <div class="header">
      <div class="title">角色详情</div>
      <div class="close" @click="$emit('close')">
        <el-icon>
          <Close />
        </el-icon>
      </div>
    </div>

    <div class="main-part">
      <div class="top">
        <div class="left">
          <div class="avatar">
            <img :src="item.avatarUrl" alt="" />
          </div>
          <div class="name">{{ item.name }}</div>
        </div>
        <div class="creator">作者：@ {{ item.creatorName || '未知' }}</div>
      </div>
      <div class="description flex">
        <div class="label">描述：</div>
        <div class="value">{{ item.description || '暂无描述' }}</div>
      </div>
      <div class="hello flex">
        <div class="label">开场白：</div>
        <div class="value">{{ item.greeting || '暂无开场白' }}</div>
      </div>
      <div class="tags flex">
        <div class="label">标签：</div>
        <div class="value tags">
          <span class="tag" v-for="(tag, index) in item.tagNames" :key="index"># {{ tag }}</span>
          <span v-if="!item.tagNames" class="tag">暂无标签</span>
        </div>
      </div>
      <div class="goto">开始对话</div>
    </div>
  </div>
  <div class="bgc"></div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed } from 'vue'
const { item } = defineProps({
  item: Object,
})
const isM = computed(() => isMobile())
// 这里可以放全局逻辑
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
    padding: 0.2rem;
    .top {
      margin: 0.1rem 0;
      .avatar {
        width: 0.7rem;
        height: 0.8rem;
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
}
.top {
  width: 100%;
  margin: 0.1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  .left {
    display: flex;
    align-items: center;
    width: 55%;

    .name {
      flex: 1;
      overflow: hidden;
      white-space: wrap;
    }
    .avatar {
      width: 1rem;
      height: 1rem;
      border-radius: 0.1rem;
      overflow: hidden;
      margin-right: 0.2rem;
      img {
        width: 100%;
        height: 100%;
      }
    }
  }
  .creator {
    font-size: 0.16rem;
  }
}

.label {
  text-align: right;
  white-space: nowrap;
  width: 4.5em;
}
.value {
  flex: 1;
}
.flex {
  width: 100%;
  display: flex;
  justify-content: start;
  margin: 0.1rem;
}
.tags {
  display: flex;
}
.tag {
  margin-right: 0.1rem;
  padding: 0.05rem 0.1rem;
  border-radius: 0.1rem;
  background-color: #fdf;
}
.goto {
  // width: 20%;
  align-self: end;
  font-size: 0.2rem;
  text-align: center;
  margin: 0.05rem 0.1rem;
  background-color: #000;
  border-radius: 0.1rem;
  padding: 0.1rem 0.2rem;
  color: #fff;
  cursor: pointer;
  &:hover {
    background-color: #333;
    transform: translateY(-1px);
  }
}
</style>
