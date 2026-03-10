<template>
  <el-card>
    <template #header>角色权限管理</template>
    <el-table :data="roles" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色" width="120" />
      <el-table-column prop="permission" label="权限字符串" />
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="edit(scope.row)">编辑权限</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRolesApi, updateRolePermissionApi } from '../api/modules'

const roles = ref([])

const load = async () => {
  roles.value = await listRolesApi()
}

const edit = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入新的权限字符串', `编辑 ${row.roleName}`, { inputValue: row.permission })
  await updateRolePermissionApi({ id: row.id, permission: value })
  ElMessage.success('更新成功')
  await load()
}

onMounted(load)
</script>
