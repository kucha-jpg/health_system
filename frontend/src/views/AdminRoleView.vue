<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">角色权限管理</h3>
        <p class="page-subtitle">维护角色权限</p>
      </div>
      <div class="page-actions">
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">权限字符串决定接口访问边界，修改后建议立刻回归验证</div>
        <div class="info-strip-desc">角色总数 {{ roles.length }}。</div>
      </div>
      <el-tag effect="light">可编辑角色 {{ roles.length }}</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">角色数量</div>
        <div class="kpi-value">{{ roles.length }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">已配置权限角色</div>
        <div class="kpi-value">{{ configuredCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">空权限角色</div>
        <div class="kpi-value">{{ emptyPermissionCount }}</div>
      </div>
    </div>

    <el-card class="section-card" shadow="never">
      <template #header>角色与权限</template>
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
  </el-card>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listRolesApi, updateRolePermissionApi } from '../api/modules'

const roles = ref([])
const configuredCount = computed(() => roles.value.filter((item) => String(item.permission || '').trim()).length)
const emptyPermissionCount = computed(() => roles.value.length - configuredCount.value)

const load = async () => {
  roles.value = await listRolesApi()
}

const edit = async (row) => {
  const { value } = await ElMessageBox.prompt(
    '请输入新的权限字符串',
    `编辑 ${row.roleName}`,
    {
      inputValue: row.permission,
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false
    }
  )
  await updateRolePermissionApi({ id: row.id, permission: value })
  ElMessage.success('更新成功')
  await load()
}

onMounted(load)
</script>
