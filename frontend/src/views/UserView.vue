<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">管理员账号管理</h3>
        <p class="page-subtitle">按角色与状态筛选账号</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="openDialog()">新增账号</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">账号管理用于维护角色覆盖与账户状态健康</div>
        <div class="info-strip-desc">筛选：{{ activeFilterText }}</div>
      </div>
      <el-tag effect="light">总记录 {{ total }}</el-tag>
    </div>

    <div class="query-grid">
      <el-input v-model="query.keyword" placeholder="用户名/姓名/手机号" clearable @keyup.enter="load" />
      <el-select v-model="query.roleType" placeholder="角色" clearable>
        <el-option label="患者" value="PATIENT" />
        <el-option label="医生" value="DOCTOR" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable>
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <div class="query-actions">
        <el-button type="primary" plain @click="load">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">当前页列表</div>
        <div class="kpi-value">{{ users.length }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">启用账号</div>
        <div class="kpi-value">{{ enabledCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">禁用账号</div>
        <div class="kpi-value">{{ disabledCount }}</div>
      </div>
    </div>

    <el-card class="section-card" shadow="never">
      <template #header>账号列表</template>
      <el-table :data="users" border v-loading="loading" empty-text="暂无匹配账号，请调整筛选条件">
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

    <div class="pager-wrap">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="load"
        @size-change="handlePageSizeChange"
      />
    </div>
  </el-card>

  <el-dialog v-model="visible" :title="form.id ? '编辑账号' : '新增账号'" width="560px" class="app-dialog-style" :show-close="false" align-center>
    <el-form :model="form" label-width="100px">
      <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" show-password /></el-form-item>
      <el-form-item label="角色">
        <el-select v-model="form.roleType" class="w-full">
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
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addUserApi, getUsersApi, updateUserApi, updateUserStatusApi } from '../api/modules'

const users = ref([])
const total = ref(0)
const visible = ref(false)
const form = reactive({})
const query = reactive({ keyword: '', roleType: '', status: null, pageNo: 1, pageSize: 20 })
const loading = ref(false)
const saving = ref(false)
let timer = null

const enabledCount = computed(() => users.value.filter((u) => u.status === 1).length)
const disabledCount = computed(() => users.value.filter((u) => u.status !== 1).length)
const activeFilterText = computed(() => {
  const role = query.roleType || '全部角色'
  const status = query.status === null ? '全部状态' : (query.status === 1 ? '启用' : '禁用')
  const keyword = query.keyword || '无'
  return `${role} / ${status} / 关键词 ${keyword}`
})

const load = async () => {
  loading.value = true
  try {
    const res = await getUsersApi(query)
    users.value = res?.records || []
    total.value = res?.total || 0
  } catch (err) {
    ElMessage.error(err?.message || '账号列表加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', roleType: '', status: null, pageNo: 1, pageSize: 20 })
  await load()
}

const handlePageSizeChange = async () => {
  query.pageNo = 1
  await load()
}

const openDialog = (row) => {
  Object.assign(form, { id: null, username: '', name: '', phone: '', password: '123456', roleType: 'PATIENT', status: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  if (!String(form.username || '').trim() || !String(form.name || '').trim() || !String(form.phone || '').trim()) {
    ElMessage.warning('请完整填写用户名、姓名和手机号')
    return
  }
  saving.value = true
  try {
    if (form.id) {
      await updateUserApi(form)
      ElMessage.success('账号更新成功')
    } else {
      await addUserApi(form)
      ElMessage.success('账号创建成功')
    }
    visible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  await ElMessageBox.confirm(
    `确认${actionText}账号 ${row.username} 吗？`,
    '状态变更确认',
    {
      type: 'warning',
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false
    }
  )
  await updateUserStatusApi(row.id, nextStatus)
  ElMessage.success(`账号已${actionText}`)
  await load()
}

onMounted(() => {
  load()
  timer = window.setInterval(load, 10000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.query-grid {
  display: grid;
  grid-template-columns: minmax(180px, 2fr) 140px 140px auto;
  gap: 8px;
  margin-bottom: 12px;
}

.query-actions {
  display: inline-flex;
  gap: 8px;
}

.pager-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .query-grid {
    grid-template-columns: 1fr;
  }

  .pager-wrap {
    justify-content: center;
  }
}
</style>
