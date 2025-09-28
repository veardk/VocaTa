<template>
  <div>
    <template v-for="item in menuList" :key="item.path">
      <!-- 没有子路由的情况 -->
      <template v-if="!item.children">
        <el-menu-item :index="item.path" v-if="!item.meta.hidden" @click="goRoute">
          <el-icon>
            <component :is="item.meta.icon"></component>
          </el-icon>
          <template #title>
            <span>{{ item.meta.title }}</span>
          </template>
        </el-menu-item>
      </template>

      <!-- 有子路由但只有一个路由的情况 -->
      <template v-else-if="item.children && item.children.length == 1">
        <el-menu-item
          v-if="!item.children[0].meta.hidden"
          :index="item.children[0].path"
          @click="goRoute"
        >
          <el-icon>
            <component :is="item.children[0].meta.icon"></component>
          </el-icon>
          <template #title>
            <span>{{ item.children[0].meta.title }}</span>
          </template>
        </el-menu-item>
      </template>

      <!-- 有子路由且个数大于一个 -->
      <template v-else>
        <el-sub-menu :index="item.path" v-if="!item.meta.hidden">
          <template #title>
            <el-icon>
              <component :is="item.meta.icon"></component>
            </el-icon>
            <span v-show="!isCollapse">{{ item.meta.title }}</span>
          </template>
          <Menu :menuList="item.children"></Menu>
        </el-sub-menu>
      </template>
    </template>
  </div>
</template>

<script setup lang="ts">
import router from '@/router'

defineProps(['isCollapse', 'menuList'])
const goRoute = (vc) => {
  router.push(vc.index)
  console.log(vc.index)
}
</script>

<script lang="ts">
export default {
  name: 'Menu',
}
</script>

<style lang="css" scoped></style>
