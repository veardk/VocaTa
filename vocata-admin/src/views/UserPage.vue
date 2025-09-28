<template>
  <div class="p-5">
    <!--<div class="text-right">
      <el-button type="primary" @click="handleAddUser">新增用户</el-button>
    </div>-->
    <el-table :data="users" style="width: 100%" :border="true" class="mt-5">
      <el-table-column prop="username" label="用户名" align="center" />
      <el-table-column prop="email" label="用户邮箱" align="center" />
      <el-table-column prop="nickname" label="用户昵称" align="center" />
      <el-table-column prop="avatar" label=" 用户头像" align="center">
        <template #default="scope">
          <el-avatar :src="scope.row.avatar" />
        </template>
      </el-table-column>
      <el-table-column prop="gender" label="用户性别" align="center">
        <template #default="scope">
          <span v-if="scope.row.gender == 0">男</span>
          <span v-else>女</span>
        </template>
      </el-table-column>
      <el-table-column prop="birthday" label="用户生日" align="center" />
      <el-table-column prop="createDate" label="注册时间" align="center" />
      <el-table-column label="修改状态" align="center">
        <template #default="scope">
          <el-button
            type="primary"
            size="small"
            :plain="scope.row.status != 1"
            @click="updateStatus(scope.row.id, 1)"
            >正常</el-button
          >
          <el-button
            type="primary"
            size="small"
            :plain="scope.row.status != 2"
            @click="updateStatus(scope.row.id, 2)"
            >锁定</el-button
          >
          <el-button
            type="primary"
            size="small"
            :plain="scope.row.status != 3"
            @click="updateStatus(scope.row.id, 3)"
            >禁用</el-button
          >
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
            <el-input placeholder="请输入用户账号" v-model="formData.userAccount" />
          </el-form-item>
          <el-form-item label="用户密码" prop="description">
            <el-input placeholder="请输入用户密码" v-model="formData.userPassword" />
          </el-form-item>
          <el-form-item label="用户电话" prop="description">
            <el-input placeholder="请输入用户电话" v-model="formData.userPhone" />
          </el-form-item>
          <el-form-item label="请选择用户类型" prop="description">
            <el-radio-group v-model="formData.userType">
              <el-radio :value="0">普通用户</el-radio>
              <el-radio :value="1">商家</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-show="formData.userType == 1" label="店铺名称" prop="description">
            <el-input placeholder="请输入店铺名称" v-model="formData.sellerName" />
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
      :total="total"
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
import { userApi } from '@/api/modules/user'
import { ElMessage } from 'element-plus'
import { onMounted, ref } from 'vue'
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const users = ref([])
const formData = ref({
  userId: '',
  userAccount: '',
  userPassword: '',
  userPhone: '',
  userType: '',
  sellerName: '',
  status: '',
})
const query = ref({
  pageNum: 1,
  pageSize: 10,
})
const total = ref(0)
const getUsers = async () => {
  try {
    const res = await userApi.getUserInfo(query.value)
    users.value = res.data.list
    total.value = res.data.total
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
}

// 新增用户
const handleAddUser = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  formData.value = {
    userId: '',
    userAccount: '',
    userPassword: '',
    userPhone: '',
    userType: 0,
    sellerName: '',
  }
}
// 编辑用户
const handleEditUser = (user) => {
  dialogType.value = 'edit'
  dialogVisible.value = true
  const userType = user.userType == '普通用户' ? 0 : 1
  formData.value = { ...user, userType }
}
const confirm = async () => {
  if (!formData.value.userId) {
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

// 修改用户状态
const updateStatus = async (id, status) => {
  try {
    await userApi.updateUserStatus(id, { status })
    ElMessage.success('修改成功')
    getUsers()
  } catch (error) {
    ElMessage.error('修改失败')
  }
}
const deleteUser = async (id) => {
  try {
    ElMessage.success('删除成功')
    getUsers()
  } catch (error) {
    ElMessage.error('删除数据失败')
  }
}
onMounted(() => {
  getUsers()
})
</script>

<style lang="scss" scoped></style>
