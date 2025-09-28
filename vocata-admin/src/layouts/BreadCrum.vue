<template>
  <div class="py-3 px-5 border-b border-gray-200">
    <el-breadcrumb separator-icon="ArrowRight">
      <el-breadcrumb-item
        v-for="item in route.matched"
        :key="item.path"
        v-show="item.meta.title"
        :to="item.path"
      >
        <el-icon class="icon" v-if="item.meta.icon">
          <component :is="item.meta.icon"></component>
        </el-icon>
        <span class="text-lg m-1">{{ item.meta.title }}</span>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
console.log(route.matched)
</script>

<script lang="ts">
export default {
  name: 'BreadCrumb',
}
</script>

<style lang="scss" scoped></style>
