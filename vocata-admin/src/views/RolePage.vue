<template>
  <div class="p-5">
    <div class="text-right">
      <el-button type="primary" @click="handleAddUser">新增角色</el-button>
    </div>
    <el-table :data="users" style="width: 100%" :border="true" class="mt-5">
      <el-table-column prop="id" label="角色ID" align="center" width="80" />
      <el-table-column prop="name" label="角色名称" align="center" />
      <el-table-column prop="description" label="角色简介" align="center" />
      <el-table-column prop="greeting" label=" 开场白" align="center" />
      <el-table-column prop="userPhone" label="角色头像" align="center">
        <template #default="scope">
          <el-avatar :src="scope.row.avatarUrl" :size="50" />
        </template>
      </el-table-column>
      <el-table-column prop="userPhone" label="角色标签" align="center">
        <template #default="scope">
          <el-tag type="success" v-for="(item, index) in JSON.parse(scope.row.tags)" :key="index">
            #{{ item }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chatCount" label="对话次数" align="center" />
      <el-table-column prop="createdAt" label="创建时间" align="center" />
      <el-table-column label="操作" align="center">
        <template #default="scope">
          <el-button
            type="warning"
            size="small"
            icon="Edit"
            title="修改用户信息"
            @click="handleEditUser(scope.row)"
            >修改</el-button
          >
          <el-popconfirm
            :title="`你确定要删除属性吗`"
            width="200px"
            @confirm="deleteUser(scope.row.id)"
          >
            <template #reference>
              <el-button type="danger" icon="Delete" title="删除用户" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '新增用户' : '编辑用户'"
      v-model="dialogVisible"
      width="30%"
      align-center
    >
      <template #default>
        <el-form label-width="120px">
          <el-form-item label="用户账号" prop="name">
            <el-input placeholder="请输入用户账号" v-model="formData.name" />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <div class="flex justify-end gap-2 px-4 pb-4">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirm"> 确定 </el-button>
        </div>
      </template>
    </el-dialog>
    <el-pagination
      layout="prev, pager, next,total"
      background
      :total="50"
      style="display: flex; justify-content: end; margin-top: 0.5rem"
      @prev-click="query.pageNum--"
      @next-click="query.pageNum++"
      @current-change="query.pageNum = $event"
    >
    </el-pagination>
  </div>
</template>

<script setup lang="ts">
// import { deleteUserApi, getUsersApi, registerApi, updateUserApi } from '@/apis/user'
import { roleApi } from '@/api/modules/role'
import { ElMessage } from 'element-plus'
import { onMounted, ref, watch } from 'vue'
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const users = ref([])
const formData = ref({
  id: '',
  name: '',
})
const query = ref({
  pageNum: 1,
  pageSize: 10,
})
const total = ref(0)
const getRoles = async () => {
  try {
    const res = await roleApi.getRoleList(query.value)
    users.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}
watch(
  () => query.value,
  () => {
    getRoles()
  },
  { deep: true },
)

// 新增用户
const handleAddUser = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  formData.value = {}
}
// 编辑用户
const handleEditUser = (user) => {
  dialogType.value = 'edit'
  dialogVisible.value = true
  const userType = user.userType == '普通用户' ? 0 : 1
  formData.value = { ...user, userType }
}
const confirm = async () => {
  if (!formData.value.id) {
    try {
      // await registerApi(formData.value)
      // ElMessage.success('新增成功')
      // getUsers()
      // dialogVisible.value = false
    } catch (error) {
      console.log(error)
      ElMessage.error('新增失败')
      dialogVisible.value = false
    }
  } else {
    try {
      // await updateUserApi(formData.value)
      // ElMessage.success('修改成功')
      // getUsers()
      // dialogVisible.value = false
    } catch (error) {
      ElMessage.success('修改失败')
      dialogVisible.value = false
    }
  }
}
const deleteUser = async (id) => {
  try {
    await roleApi.deleteRole(id)
    ElMessage.success('删除成功')
    getRoles()
  } catch (error) {
    ElMessage.error('删除数据失败')
  }
}
onMounted(() => {
  getRoles()
})
</script>

<style lang="scss" scoped></style>
