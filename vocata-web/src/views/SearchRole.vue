<template>
  <div :class="isM ? 'mobile' : 'pc'" class="main-container">
    <div class="header">
      <div class="title">探索</div>
      <div class="search">
        <input type="text" v-model="searchInput" placeholder="搜索角色" />
        <el-icon>
          <Search />
        </el-icon>
      </div>
    </div>
    <div class="role-list">
      <div v-for="item in 10" :key="item" class="role-list-item">角色{{ item }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { isMobile } from '@/utils/isMobile'
import { computed, ref } from 'vue'
const isM = computed(() => isMobile())

const searchInput = ref('')
// 这里可以放全局逻辑
</script>

<style lang="scss">
.main-container {
  padding: 0.3rem 0.5rem;

  &.mobile {
    padding: 0 0.2rem 0;
    .header {
      .title {
        font-size: 0.2rem;
      }
      .search {
        width: 70%;
        height: 0.3rem;
        padding: 0 0.1rem;
        input {
          width: 50%;
          font-size: 0.2rem;
          margin: 0;
          padding: 0 0.05rem;
        }
        .el-icon {
          font-size: 0.2rem;
          margin: 0;
          padding: 0;
        }
      }
    }
    .role-list {
      margin: 0.2rem 0;
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.1rem;
      .role-list-item {
        width: 100%;
        height: 2rem;
        font-size: 0.2rem;
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

  .search {
    width: 50%;
    height: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #ccc;
    border-radius: 0.1rem;
    overflow: hidden;
    background-color: #f0f0f0;
    input {
      flex: 1;
      height: 95%;
      border: none;
      padding: 0.2rem;
      outline: none;
      font-size: 0.3rem;
      background-color: transparent;
    }
    .el-icon {
      font-size: 0.3rem;
      margin: 0.2rem;
    }
  }
}
.role-list {
  margin-top: 0.3rem;
  display: grid;
  grid-template-columns: repeat(5, 1fr); /* 一行五个 */
  gap: 0.1rem; /* 间距0.1rem */
  .role-list-item {
    width: 3rem;
    height: 3rem;
    font-size: 0.3rem;
    background-color: #eee;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  &:last-child {
    margin-right: 0;
  }
}
</style>
