<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">角色权限管理</h3>
      </div>
      <div class="page-actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="soft-tip">当前共 {{ roles.length }} 个角色，请谨慎修改权限字符串并及时回归关键接口。</div>

    <el-table :data="roles" border empty-text="暂无角色数据">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="roleName" label="角色" width="120" />
      <el-table-column prop="permission" label="权限字符串" />
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="edit(scope.row)">编辑权限</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无角色数据</div>
          <div class="empty-desc">请检查角色配置初始化或稍后重试。</div>
        </div>
      </template>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { showError } from '../utils/message'
import { listRolesApi, updateRolePermissionApi } from '../api/modules'

const roles = ref([])

const load = async () => {
  try {
    roles.value = await listRolesApi()
  } catch (err) {
    showError(err?.message || '加载角色数据失败，请稍后重试')
  }
}

const edit = async (row) => {
  let value
  try {
    const result = await ElMessageBox.prompt('请输入新的权限字符串', `编辑 ${row.roleName}`, {
      inputValue: row.permission,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
    value = result.value
  } catch {
    return
  }
  try {
    await updateRolePermissionApi({ id: row.id, permission: value })
    await load()
  } catch (err) {
    showError(err?.message || '更新权限失败，请稍后重试')
  }
}

onMounted(load)
</script>
