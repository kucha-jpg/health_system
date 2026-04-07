<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">角色权限管理</h3>
        <p class="page-subtitle">统一维护系统角色权限字符串，保持鉴权规则清晰可控</p>
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
