<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>管理员账号管理</span>
        <el-button type="primary" @click="openDialog()">新增账号</el-button>
      </div>
    </template>

    <el-table :data="users" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="name" label="姓名" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column prop="roleType" label="角色" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">{{ scope.row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
          <el-button link type="warning" @click="toggleStatus(scope.row)">{{ scope.row.status === 1 ? '禁用' : '启用' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" title="用户信息">
    <el-form :model="form" label-width="100px">
      <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" show-password /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleType" style="width:100%">
          <el-option label="患者" value="PATIENT" />
          <el-option label="医生" value="DOCTOR" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { addUserApi, getUsersApi, updateUserApi, updateUserStatusApi } from '../api/modules'

const users = ref([])
const visible = ref(false)
const form = reactive({})

const load = async () => {
  users.value = await getUsersApi()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, username: '', name: '', phone: '', password: '123456', roleType: 'PATIENT', status: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  if (form.id) {
    await updateUserApi(form)
  } else {
    await addUserApi(form)
  }
  visible.value = false
  await load()
}

const toggleStatus = async (row) => {
  await updateUserStatusApi(row.id, row.status === 1 ? 0 : 1)
  await load()
}

onMounted(load)
</script>
