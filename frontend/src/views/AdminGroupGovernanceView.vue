<template>
  <el-card class="page-shell fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">群组治理（管理员）</h3>
        <p class="page-subtitle">审核、归档、跨科室治理流程壳（待对接正式治理接口）</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="soft-tip">
      当前为前端流程壳：动作与操作记录已可演示，后续接入后端治理接口后可保留现有交互。
    </div>

    <div class="toolbar-row">
      <el-input
        v-model="filters.keyword"
        clearable
        placeholder="按群组名称筛选"
        class="toolbar-item"
      />
      <el-select v-model="filters.status" clearable placeholder="治理状态" class="toolbar-item">
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="待归档" value="PENDING_ARCHIVE" />
        <el-option label="跨科室中" value="CROSS_DEPT" />
        <el-option label="运行中" value="ACTIVE" />
        <el-option label="已归档" value="ARCHIVED" />
      </el-select>
      <el-checkbox v-model="filters.operableOnly">仅看可操作</el-checkbox>
      <el-button @click="resetFilters">重置筛选</el-button>
      <el-button :disabled="!tableRows.length" @click="selectPendingReview">全选待审核</el-button>
      <el-button :disabled="!tableRows.length" @click="selectOperable">全选可操作</el-button>
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
      row-key="_rowKey"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="44" reserve-selection />
      <el-table-column prop="groupName" label="群组名称" min-width="180" />
      <el-table-column prop="patientCount" label="患者数" width="90" />
      <el-table-column prop="targetDept" label="目标科室" min-width="130" />
      <el-table-column label="治理状态" width="150">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastActionTime" label="最近操作" width="170" />
      <el-table-column label="治理动作" min-width="280">
        <template #default="scope">
          <el-button link type="primary" :disabled="scope.row.status === 'ARCHIVED'" @click="approveGroup(scope.row)">审核通过</el-button>
          <el-button link type="warning" :disabled="scope.row.status === 'ARCHIVED'" @click="crossDeptGroup(scope.row)">跨科室</el-button>
          <el-button link type="danger" :disabled="scope.row.status === 'ARCHIVED'" @click="archiveGroup(scope.row)">归档</el-button>
          <el-button link @click="openLogs(scope.row)">操作记录</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-drawer v-model="logsVisible" title="治理操作记录" size="520px">
    <div v-if="activeGroupName" class="drawer-head">群组：{{ activeGroupName }}</div>
    <div class="log-filter-row">
      <el-select v-model="logFilters.action" clearable placeholder="动作类型" class="log-filter-item">
        <el-option v-for="action in logActions" :key="action" :label="action" :value="action" />
      </el-select>
      <el-input
        v-model="logFilters.keyword"
        clearable
        placeholder="输入关键词筛选记录"
        class="log-filter-item"
      />
      <el-button @click="resetLogFilters">重置</el-button>
    </div>
    <el-timeline>
      <el-timeline-item
        v-for="(item, idx) in visibleLogs"
        :key="`${item.time}-${idx}`"
        :timestamp="item.time"
        placement="top"
      >
        <div class="log-title">{{ item.action }}</div>
        <div class="log-desc">{{ item.detail }}</div>
      </el-timeline-item>
    </el-timeline>
    <div v-if="visibleLogs.length === 0" class="empty-log">暂无符合条件的治理记录</div>
  </el-drawer>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMonitorOverviewApi } from '../api/modules'

const loading = ref(false)
const logsVisible = ref(false)
const activeLogs = ref([])
const activeGroupName = ref('')
const groupRows = ref([])
const selectedRows = ref([])
const tableRef = ref(null)
const governanceMap = reactive({})
const logFilters = reactive({
  action: '',
  keyword: ''
})
const filters = reactive({
  keyword: '',
  status: '',
  operableOnly: false
})

const logActions = computed(() => {
  const set = new Set(activeLogs.value.map((item) => item.action).filter(Boolean))
  return Array.from(set)
})

const visibleLogs = computed(() => {
  const action = logFilters.action
  const keyword = String(logFilters.keyword || '').trim().toLowerCase()
  return activeLogs.value.filter((item) => {
    const hitAction = !action || item.action === action
    const merged = `${item.action || ''} ${item.detail || ''}`.toLowerCase()
    const hitKeyword = !keyword || merged.includes(keyword)
    return hitAction && hitKeyword
  })
})

const tableRows = computed(() => {
  const keyword = String(filters.keyword || '').trim().toLowerCase()
  const status = filters.status
  return groupRows.value.filter((item) => {
    const hitName = !keyword || String(item.groupName || '').toLowerCase().includes(keyword)
    const hitStatus = !status || item.status === status
    const hitOperable = !filters.operableOnly || item.status !== 'ARCHIVED'
    return hitName && hitStatus && hitOperable
  })
})

const summary = computed(() => {
  const rows = tableRows.value
  return {
    totalGroups: rows.length,
    pendingReview: rows.filter((item) => item.status === 'PENDING_REVIEW').length,
    pendingArchive: rows.filter((item) => item.status === 'PENDING_ARCHIVE').length,
    crossDept: rows.filter((item) => item.status === 'CROSS_DEPT').length
  }
})

const nowText = () => new Date().toLocaleString()

const statusText = (status) => {
  if (status === 'PENDING_REVIEW') return '待审核'
  if (status === 'PENDING_ARCHIVE') return '待归档'
  if (status === 'CROSS_DEPT') return '跨科室中'
  if (status === 'ARCHIVED') return '已归档'
  return '运行中'
}

const statusTagType = (status) => {
  if (status === 'PENDING_REVIEW') return 'warning'
  if (status === 'PENDING_ARCHIVE') return 'info'
  if (status === 'CROSS_DEPT') return 'danger'
  if (status === 'ARCHIVED') return 'info'
  return 'success'
}

const inferStatus = (patientCount) => {
  if (patientCount === 0) return 'PENDING_ARCHIVE'
  if (patientCount >= 80) return 'PENDING_REVIEW'
  return 'ACTIVE'
}

const rowKey = (row) => String(row.groupId ?? row.id ?? row.groupName)

const ensureState = (row) => {
  const key = rowKey(row)
  if (governanceMap[key]) return governanceMap[key]

  const initialStatus = inferStatus(Number(row.patientCount) || 0)
  const state = {
    status: initialStatus,
    targetDept: '-',
    lastActionTime: '-',
    logs: []
  }
  governanceMap[key] = state
  return state
}

const appendLog = (row, action, detail) => {
  const state = ensureState(row)
  const time = nowText()
  state.lastActionTime = time
  state.logs.unshift({ time, action, detail })
}

const mapRows = (groups) => {
  selectedRows.value = []
  groupRows.value = (groups || []).map((item) => {
    const state = ensureState(item)
    return {
      ...item,
      _rowKey: rowKey(item),
      status: state.status,
      targetDept: state.targetDept,
      lastActionTime: state.lastActionTime
    }
  })
}

const onSelectionChange = (rows) => {
  selectedRows.value = Array.isArray(rows) ? rows : []
}

const clearSelected = () => {
  selectedRows.value = []
  tableRef.value?.clearSelection?.()
}

const selectRowsBy = (matcher) => {
  const table = tableRef.value
  if (!table) return
  clearSelected()
  tableRows.value.forEach((row) => {
    if (matcher(row)) table.toggleRowSelection(row, true)
  })
}

const selectPendingReview = () => {
  selectRowsBy((row) => row.status === 'PENDING_REVIEW')
  if (!selectedRows.value.length) {
    ElMessage.info('当前筛选结果中无待审核群组')
  }
}

const selectOperable = () => {
  selectRowsBy((row) => row.status !== 'ARCHIVED')
  if (!selectedRows.value.length) {
    ElMessage.info('当前筛选结果中无可操作群组')
  }
}

const confirmBatchAction = async ({ title, selectedCount, executableCount }) => {
  const skipped = selectedCount - executableCount
  await ElMessageBox.confirm(
    `本次选中 ${selectedCount} 个群组，可处理 ${executableCount} 个，跳过 ${skipped} 个。确认继续吗？`,
    title,
    {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    }
  )
}

const resetFilters = () => {
  filters.keyword = ''
  filters.status = ''
  filters.operableOnly = false
}

const resetLogFilters = () => {
  logFilters.action = ''
  logFilters.keyword = ''
}

const updateRow = (targetRow, patcher) => {
  const key = rowKey(targetRow)
  groupRows.value = groupRows.value.map((item) => {
    if (rowKey(item) !== key) return item
    return patcher(item)
  })
}

const load = async () => {
  loading.value = true
  try {
    const data = await getMonitorOverviewApi()
    const groups = Array.isArray(data?.groupStats) ? data.groupStats : []
    mapRows(groups)
  } catch (err) {
    ElMessage.error(err?.message || '群组治理数据加载失败，请稍后重试')
  } finally {
    loading.value = false
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

  const header = ['群组名称', '患者数', '目标科室', '治理状态', '最近操作']
  const lines = tableRows.value.map((row) => ([
    row.groupName,
    row.patientCount,
    row.targetDept,
    statusText(row.status),
    row.lastActionTime
  ].map(escapeCsv).join(',')))

  const csv = [header.join(','), ...lines].join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' })
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

const approveGroup = (row) => {
  const state = ensureState(row)
  state.status = 'ACTIVE'
  appendLog(row, '审核通过', '已确认群组配置，继续运行。')
  updateRow(row, (item) => ({ ...item, status: state.status, lastActionTime: state.lastActionTime }))
  ElMessage.success('审核状态已更新')
}

const archiveGroup = async (row) => {
  try {
    await ElMessageBox.confirm(`确认归档群组“${row.groupName}”吗？`, '归档确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const state = ensureState(row)
  state.status = 'ARCHIVED'
  appendLog(row, '归档', '群组已进入归档状态。')
  updateRow(row, (item) => ({ ...item, status: state.status, lastActionTime: state.lastActionTime }))
  ElMessage.success('群组已归档')
}

const crossDeptGroup = async (row) => {
  let result
  try {
    result = await ElMessageBox.prompt('请输入目标科室名称', `跨科室治理 - ${row.groupName}`, {
      inputPlaceholder: '例如：心内科二组',
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const dept = String(result.value || '').trim()
  if (!dept) {
    ElMessage.warning('目标科室不能为空')
    return
  }

  const state = ensureState(row)
  state.status = 'CROSS_DEPT'
  state.targetDept = dept
  appendLog(row, '发起跨科室', `已发起跨科室治理，目标科室：${dept}`)
  updateRow(row, (item) => ({ ...item, status: state.status, targetDept: state.targetDept, lastActionTime: state.lastActionTime }))
  ElMessage.success('跨科室流程已发起')
}

const batchApprove = () => {
  const selected = selectedRows.value
  const rows = selected.filter((item) => item.status !== 'ARCHIVED')
  const skipped = selected.length - rows.length
  if (!rows.length) {
    ElMessage.warning('已选群组均不可审核')
    return
  }
  confirmBatchAction({
    title: '批量审核确认',
    selectedCount: selected.length,
    executableCount: rows.length
  }).then(() => {
    rows.forEach((row) => {
      const state = ensureState(row)
      state.status = 'ACTIVE'
      appendLog(row, '批量审核通过', '已批量确认群组配置，继续运行。')
      updateRow(row, (item) => ({ ...item, status: state.status, lastActionTime: state.lastActionTime }))
    })
    clearSelected()
    ElMessageBox.alert(`本次处理 ${rows.length} 个群组，跳过 ${skipped} 个已归档群组。`, '批量审核结果', {
      confirmButtonText: '知道了'
    })
  }).catch(() => {})
}

const batchArchive = async () => {
  const selected = selectedRows.value
  const rows = selected.filter((item) => item.status !== 'ARCHIVED')
  const skipped = selected.length - rows.length
  if (!rows.length) {
    ElMessage.warning('已选群组均为已归档状态')
    return
  }

  try {
    await confirmBatchAction({
      title: '批量归档确认',
      selectedCount: selected.length,
      executableCount: rows.length
    })
  } catch {
    return
  }

  rows.forEach((row) => {
    const state = ensureState(row)
    state.status = 'ARCHIVED'
    appendLog(row, '批量归档', '群组已通过批量操作归档。')
    updateRow(row, (item) => ({ ...item, status: state.status, lastActionTime: state.lastActionTime }))
  })
  clearSelected()
  ElMessageBox.alert(`本次归档 ${rows.length} 个群组，跳过 ${skipped} 个已归档群组。`, '批量归档结果', {
    confirmButtonText: '知道了'
  })
}

const batchCrossDept = async () => {
  const selected = selectedRows.value
  const rows = selected.filter((item) => item.status !== 'ARCHIVED')
  const skipped = selected.length - rows.length
  if (!rows.length) {
    ElMessage.warning('已选群组均不可跨科室')
    return
  }

  try {
    await confirmBatchAction({
      title: '批量跨科室确认',
      selectedCount: selected.length,
      executableCount: rows.length
    })
  } catch {
    return
  }

  let result
  try {
    result = await ElMessageBox.prompt('请输入目标科室名称', `批量跨科室（${rows.length} 个群组）`, {
      inputPlaceholder: '例如：内分泌联合组',
      confirmButtonText: '保存',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }

  const dept = String(result.value || '').trim()
  if (!dept) {
    ElMessage.warning('目标科室不能为空')
    return
  }

  rows.forEach((row) => {
    const state = ensureState(row)
    state.status = 'CROSS_DEPT'
    state.targetDept = dept
    appendLog(row, '批量发起跨科室', `已批量发起跨科室治理，目标科室：${dept}`)
    updateRow(row, (item) => ({ ...item, status: state.status, targetDept: state.targetDept, lastActionTime: state.lastActionTime }))
  })
  clearSelected()
  ElMessageBox.alert(`本次发起 ${rows.length} 个群组跨科室流程，跳过 ${skipped} 个已归档群组。`, '批量跨科室结果', {
    confirmButtonText: '知道了'
  })
}

const openLogs = (row) => {
  const state = ensureState(row)
  activeGroupName.value = row.groupName || '-'
  activeLogs.value = [...state.logs]
  resetLogFilters()
  logsVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.governance-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
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

.log-filter-row {
  margin: 0 0 10px;
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.log-filter-item {
  width: 190px;
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

.drawer-head {
  margin-bottom: 10px;
  font-size: 13px;
  color: #5e757d;
}

.log-title {
  font-size: 13px;
  font-weight: 700;
  color: #224b54;
}

.log-desc {
  margin-top: 4px;
  font-size: 12px;
  color: #5f767d;
}

.empty-log {
  color: #70878e;
  font-size: 12px;
  padding: 12px 0;
}

@media (max-width: 900px) {
  .governance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar-item {
    width: 100%;
  }

  .log-filter-item {
    width: 100%;
  }
}

@media (max-width: 680px) {
  .governance-grid {
    grid-template-columns: 1fr;
  }
}
</style>
