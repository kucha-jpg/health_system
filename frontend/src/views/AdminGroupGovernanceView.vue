<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">群组治理</h3>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="toolbar-row">
      <el-input v-model="filters.keyword" clearable placeholder="按群组名称筛选" class="toolbar-item" @change="load" />
      <el-select v-model="filters.status" clearable placeholder="治理状态" class="toolbar-item" @change="load">
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="待归档" value="PENDING_ARCHIVE" />
        <el-option label="跨科室中" value="CROSS_DEPT" />
        <el-option label="运行中" value="ACTIVE" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-checkbox v-model="filters.operableOnly" @change="load">仅看可操作</el-checkbox>
      <el-button @click="resetFilters">重置筛选</el-button>
      <el-button :disabled="!tableRows.length" @click="exportCsv">导出当前结果</el-button>
      <el-button type="primary" :disabled="!selectedRows.length" @click="batchApprove">批量审核通过</el-button>
      <el-button type="warning" :disabled="!selectedRows.length" @click="batchCrossDept">批量跨科室</el-button>
      <el-button type="danger" :disabled="!selectedRows.length" @click="batchArchive">批量归档</el-button>
    </div>

    <div class="governance-grid">
      <el-card shadow="never" class="governance-card">
        <div class="governance-label">群组总量</div>
        <div class="governance-value">{{ summary.totalGroups }}</div>
      </el-card>
      <el-card shadow="never" class="governance-card">
        <div class="governance-label">待审核</div>
        <div class="governance-value">{{ summary.pendingReview }}</div>
      </el-card>
      <el-card shadow="never" class="governance-card">
        <div class="governance-label">待归档</div>
        <div class="governance-value">{{ summary.pendingArchive }}</div>
      </el-card>
      <el-card shadow="never" class="governance-card">
        <div class="governance-label">跨科室处理中</div>
        <div class="governance-value">{{ summary.crossDept }}</div>
      </el-card>
    </div>

    <el-table
      ref="tableRef"
      :data="tableRows"
      border
      v-loading="loading"
      empty-text="暂无群组治理数据"
      row-key="groupId"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="44" />
      <el-table-column prop="groupName" label="群组名称" min-width="180" />
      <el-table-column prop="doctorName" label="负责医生" width="120" />
      <el-table-column prop="patientCount" label="患者数" width="90" />
      <el-table-column prop="targetDept" label="目标科室" min-width="130">
        <template #default="scope">
          {{ scope.row.targetDept || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="治理状态" width="130">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.governanceStatus)">{{ statusText(scope.row.governanceStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastActionTime" label="最近操作" width="170">
        <template #default="scope">
          {{ scope.row.lastActionTime || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openDrawer(scope.row)">审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="handlePageSizeChange"
      />
    </div>
  </el-card>

  <GovernanceDrawer
    v-model="drawerVisible"
    :group="drawerGroup"
    :loading="drawerLoading"
    @approve="onDrawerApprove"
    @reject="onDrawerReject"
    @archive="onDrawerArchive"
    @cross-dept="onDrawerCrossDept"
  />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  approveGroupApi,
  rejectGroupApi,
  archiveGroupApi,
  batchApproveGroupsApi,
  batchArchiveGroupsApi,
  batchCrossDeptGroupsApi,
  crossDeptGroupApi,
  getAdminGroupStatsApi,
  listAdminGroupsApi
} from '../api/modules'
import GovernanceDrawer from './components/GovernanceDrawer.vue'

const loading = ref(false)
const drawerVisible = ref(false)
const drawerGroup = ref(null)
const drawerLoading = ref(false)
const groups = ref([])
const selectedRows = ref([])
const tableRef = ref(null)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = ref({
  keyword: '',
  status: '',
  operableOnly: false
})

const summary = ref({
  totalGroups: 0,
  pendingReview: 0,
  pendingArchive: 0,
  crossDept: 0
})

const tableRows = computed(() => groups.value)

const statusText = (status) => {
  const map = {
    PENDING_REVIEW: '待审核',
    REJECTED: '已驳回',
    PENDING_ARCHIVE: '待归档',
    CROSS_DEPT: '跨科室中',
    ARCHIVED: '已归档',
    ACTIVE: '运行中'
  }
  return map[status] || status || '运行中'
}

const statusTagType = (status) => {
  if (status === 'PENDING_REVIEW') return 'warning'
  if (status === 'REJECTED') return 'danger'
  if (status === 'PENDING_ARCHIVE') return 'info'
  if (status === 'CROSS_DEPT') return 'danger'
  if (status === 'ARCHIVED') return 'info'
  return 'success'
}

const load = async () => {
  loading.value = true
  try {
    const params = {
      pageNo: pageNo.value,
      pageSize: pageSize.value
    }
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.operableOnly) params.operableOnly = true

    const res = await listAdminGroupsApi(params)
    const list = Array.isArray(res?.list) ? res.list : Array.isArray(res) ? res : []
    groups.value = list
    total.value = res?.total || list.length
    selectedRows.value = []

    const stats = await getAdminGroupStatsApi()
    if (stats) {
      summary.value = {
        totalGroups: stats.totalGroups || 0,
        pendingReview: stats.pendingReview || 0,
        pendingArchive: stats.pendingArchive || 0,
        crossDept: stats.crossDept || 0
      }
    }
  } catch (err) {
    ElMessage.error(err?.message || '加载群组治理数据失败')
  } finally {
    loading.value = false
  }
}

const onSelectionChange = (rows) => {
  selectedRows.value = rows
}

const clearSelected = () => {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

const resetFilters = () => {
  filters.value = { keyword: '', status: '', operableOnly: false }
  pageNo.value = 1
  load()
}

const handlePageSizeChange = () => {
  pageNo.value = 1
  load()
}

const openDrawer = (row) => {
  drawerGroup.value = row
  drawerVisible.value = true
}

const onDrawerApprove = async ({ group, remark }) => {
  drawerLoading.value = true
  try {
    await approveGroupApi(group.groupId, remark)
    ElMessage.success(`群组「${group.groupName}」已审核通过`)
    drawerVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '审核操作失败')
  } finally {
    drawerLoading.value = false
  }
}

const onDrawerReject = async ({ group, reason }) => {
  drawerLoading.value = true
  try {
    await rejectGroupApi(group.groupId, reason)
    ElMessage.success(`群组「${group.groupName}」已驳回`)
    drawerVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '驳回操作失败')
  } finally {
    drawerLoading.value = false
  }
}

const onDrawerArchive = async (group) => {
  drawerLoading.value = true
  try {
    await archiveGroupApi(group.groupId)
    ElMessage.success(`群组「${group.groupName}」已归档`)
    drawerVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '归档操作失败')
  } finally {
    drawerLoading.value = false
  }
}

const onDrawerCrossDept = async ({ group, targetDept }) => {
  drawerLoading.value = true
  try {
    await crossDeptGroupApi(group.groupId, { targetDept })
    ElMessage.success(`群组「${group.groupName}」已发起跨科室（${targetDept}）`)
    drawerVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '跨科室操作失败')
  } finally {
    drawerLoading.value = false
  }
}

const confirmBatch = async (title, selected, executable) => {
  const skipped = selected.length - executable.length
  await ElMessageBox.confirm(
    `选中 ${selected.length} 个群组，可处理 ${executable.length} 个${skipped > 0 ? `，跳过 ${skipped} 个已归档` : ''}。确认继续？`,
    title,
    {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    }
  )
}

const batchApprove = async () => {
  const executable = selectedRows.value.filter(r => r.governanceStatus === 'PENDING_REVIEW' || r.governanceStatus === 'CROSS_DEPT')
  if (!executable.length) { ElMessage.warning('已选群组无可审核的（仅待审核或跨科室状态可操作）'); return }
  try { await confirmBatch('批量审核确认', selectedRows.value, executable) } catch { return }
  try {
    const res = await batchApproveGroupsApi({ ids: executable.map(r => r.groupId) })
    ElMessage.success(`已审核通过 ${res?.processed || executable.length} 个群组，跳过 ${res?.skipped ?? 0} 个`)
    clearSelected()
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '批量审核失败')
  }
}

const batchArchive = async () => {
  const executable = selectedRows.value.filter(r => r.governanceStatus !== 'ARCHIVED')
  if (!executable.length) { ElMessage.warning('已选群组均为已归档状态'); return }
  try { await confirmBatch('批量归档确认', selectedRows.value, executable) } catch { return }
  try {
    const res = await batchArchiveGroupsApi({ ids: executable.map(r => r.groupId) })
    ElMessage.success(`已归档 ${res?.processed || executable.length} 个群组，跳过 ${res?.skipped ?? 0} 个`)
    clearSelected()
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '批量归档失败')
  }
}

const batchCrossDept = async () => {
  const executable = selectedRows.value.filter(r => r.governanceStatus !== 'ARCHIVED')
  if (!executable.length) { ElMessage.warning('已选群组均不可跨科室'); return }
  try { await confirmBatch('批量跨科室确认', selectedRows.value, executable) } catch { return }
  let result
  try {
    result = await ElMessageBox.prompt('请输入目标科室名称', `批量跨科室（${executable.length} 个群组）`, {
      inputPlaceholder: '例如：内分泌联合组',
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch { return }
  const dept = String(result.value || '').trim()
  if (!dept) { ElMessage.warning('目标科室不能为空'); return }
  try {
    const res = await batchCrossDeptGroupsApi({ ids: executable.map(r => r.groupId), targetDept: dept })
    ElMessage.success(`已发起 ${res?.processed || executable.length} 个群组跨科室流程，跳过 ${res?.skipped ?? 0} 个`)
    clearSelected()
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '批量跨科室失败')
  }
}

const escapeCsv = (value) => {
  const text = String(value ?? '')
  if (text.includes('"') || text.includes(',') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const exportCsv = () => {
  if (!tableRows.value.length) {
    ElMessage.info('当前没有可导出的治理数据')
    return
  }
  const header = ['群组名称', '负责医生', '患者数', '目标科室', '治理状态', '最近操作']
  const lines = tableRows.value.map(row => [
    row.groupName, row.doctorName, row.patientCount,
    row.targetDept || '-', statusText(row.governanceStatus), row.lastActionTime || '-'
  ].map(escapeCsv).join(','))
  const csv = [header.join(','), ...lines].join('\n')
  const blob = new Blob([`﻿${csv}`], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  const stamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
  link.download = `群组治理导出-${stamp}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success(`已导出 ${tableRows.value.length} 条记录`)
}

onMounted(load)
</script>

<style scoped>
.governance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.toolbar-row {
  margin: 12px 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-item {
  width: 220px;
}

.governance-card {
  min-height: 88px;
}

.governance-label {
  color: #60777d;
  font-size: 12px;
}

.governance-value {
  margin-top: 8px;
  color: #1e454f;
  font-size: 24px;
  font-weight: 700;
}

.pager-row {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 900px) {
  .governance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .toolbar-item {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .governance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
