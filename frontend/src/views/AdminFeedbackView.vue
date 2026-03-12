<template>
  <div class="feedback-admin-page">
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-label">反馈总数</div>
        <div class="stat-value">{{ stats.totalCount }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">待处理</div>
        <div class="stat-value warn">{{ stats.pendingCount }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">今日新增</div>
        <div class="stat-value">{{ stats.todayNewCount }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">角色分布</div>
        <div class="stat-value role">
          <span>患者 {{ stats.roleDistribution.PATIENT || 0 }}</span>
          <span>医生 {{ stats.roleDistribution.DOCTOR || 0 }}</span>
        </div>
      </el-card>
    </div>

    <el-card>
      <template #header>
        <span>近7天反馈趋势</span>
      </template>
      <div ref="trendChartRef" class="trend-chart"></div>
    </el-card>

    <el-card>
    <template #header>
      <div class="toolbar">
        <span>反馈消息</span>
        <div style="display:flex; gap:8px; align-items:center; margin-left:auto;">
          <el-input v-model="query.keyword" placeholder="用户名/内容关键字" clearable style="width:220px" />
          <el-select v-model="query.roleType" placeholder="角色" clearable style="width:120px">
            <el-option label="患者" value="PATIENT" />
            <el-option label="医生" value="DOCTOR" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:120px">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
          <el-select v-model="query.replyStatus" placeholder="回复" clearable style="width:120px">
            <el-option label="未回复" :value="0" />
            <el-option label="已回复" :value="1" />
          </el-select>
          <el-date-picker
            v-model="query.range"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:320px"
          />
          <el-button @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button :disabled="rows.length === 0" @click="selectCurrentPending">全选本页未处理</el-button>
          <el-button :disabled="selectedRows.length === 0" @click="clearSelection">清空选择</el-button>
          <el-button type="success" plain :disabled="total === 0" @click="batchMarkByFilter(1)">筛选结果标记已处理</el-button>
          <el-button type="warning" plain :disabled="total === 0" @click="batchMarkByFilter(0)">筛选结果标记未处理</el-button>
          <el-button type="primary" plain @click="exportCsv">导出CSV</el-button>
          <el-button type="success" :disabled="selectedRows.length === 0" @click="batchMark(1)">批量标记已处理</el-button>
          <el-button type="warning" :disabled="selectedRows.length === 0" @click="batchMark(0)">批量标记未处理</el-button>
        </div>
      </div>
    </template>

    <el-table ref="tableRef" :data="rows" border @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="senderUsername" label="反馈账号" width="150" />
      <el-table-column prop="senderRoleType" label="角色" width="100" />
      <el-table-column prop="content" label="反馈内容" min-width="360" />
      <el-table-column label="处理回复" min-width="320">
        <template #default="scope">
          {{ scope.row.replyContent || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="repliedTime" label="回复时间" width="180" />
      <el-table-column prop="createTime" label="提交时间" width="180" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'warning'">
            {{ scope.row.status === 1 ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="scope">
          <el-button link type="primary" @click="openReply(scope.row)">回复</el-button>
          <el-button v-if="scope.row.status !== 1" link type="success" @click="mark(scope.row.id, 1)">标记处理</el-button>
          <el-button v-else link type="warning" @click="mark(scope.row.id, 0)">标记未处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 12px; display:flex; justify-content:flex-end;">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="replyVisible" title="回复反馈" width="520px">
      <el-form :model="replyForm" label-width="90px">
        <el-form-item label="反馈账号">
          <el-input v-model="replyForm.senderUsername" disabled />
        </el-form-item>
        <el-form-item label="反馈内容">
          <el-input v-model="replyForm.content" type="textarea" :rows="3" disabled />
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="replyForm.status" style="width:100%">
            <el-option label="未处理" :value="0" />
            <el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="replyForm.replyContent" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="请输入给用户的处理反馈" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReply">保存回复</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { batchUpdateFeedbackStatusApi, batchUpdateFeedbackStatusByFilterApi, getAdminFeedbackStatsApi, listAdminFeedbackPageApi, replyFeedbackApi, updateFeedbackStatusApi } from '../api/modules'
import { authStore } from '../stores/auth'

const tableRef = ref(null)
const rows = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])
const query = reactive({ keyword: '', roleType: '', status: null, replyStatus: null, range: [] })
const stats = reactive({
  totalCount: 0,
  pendingCount: 0,
  todayNewCount: 0,
  roleDistribution: {},
  recent7Days: []
})
const trendChartRef = ref(null)
const replyVisible = ref(false)
const replyForm = reactive({ id: null, senderUsername: '', content: '', status: 1, replyContent: '' })
let timer = null
let trendChart = null

const renderTrendChart = async () => {
  await nextTick()
  if (!trendChartRef.value) {
    return
  }
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const xData = (stats.recent7Days || []).map(item => item.date)
  const yData = (stats.recent7Days || []).map(item => item.count)

  trendChart.setOption({
    grid: { left: 36, right: 24, top: 24, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: xData,
      axisLabel: { color: '#4b5563', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#4b5563' },
      splitLine: { lineStyle: { color: '#e5e7eb' } }
    },
    series: [
      {
        name: '反馈数',
        type: 'line',
        smooth: true,
        data: yData,
        showSymbol: true,
        symbolSize: 6,
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(37,99,235,0.32)' },
              { offset: 1, color: 'rgba(37,99,235,0.06)' }
            ]
          }
        }
      }
    ]
  })
}

const handleResize = () => {
  if (trendChart) {
    trendChart.resize()
  }
}

const loadStats = async () => {
  const res = await getAdminFeedbackStatsApi()
  stats.totalCount = res.totalCount || 0
  stats.pendingCount = res.pendingCount || 0
  stats.todayNewCount = res.todayNewCount || 0
  stats.roleDistribution = res.roleDistribution || {}
  stats.recent7Days = res.recent7Days || []
  await renderTrendChart()
}

const load = async () => {
  const params = {
    keyword: query.keyword,
    roleType: query.roleType,
    status: query.status,
    replyStatus: query.replyStatus,
    pageNo: pageNo.value,
    pageSize: pageSize.value
  }
  if (query.range?.length === 2) {
    params.startTime = query.range[0]
    params.endTime = query.range[1]
  }
  const res = await listAdminFeedbackPageApi(params)
  rows.value = res.records || []
  total.value = res.total || 0
  selectedRows.value = []
  await nextTick()
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
}

const onSelectionChange = (selection) => {
  selectedRows.value = selection || []
}

const clearSelection = () => {
  selectedRows.value = []
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
}

const selectCurrentPending = async () => {
  const pendingRows = rows.value.filter(item => item.status !== 1)
  if (pendingRows.length === 0) {
    ElMessage.warning('当前页没有未处理数据')
    return
  }

  await nextTick()
  if (!tableRef.value) {
    return
  }
  tableRef.value.clearSelection()
  pendingRows.forEach((row) => {
    tableRef.value.toggleRowSelection(row, true)
  })
  ElMessage.success(`已选中本页未处理 ${pendingRows.length} 条`)
}

const refreshAll = async () => {
  await Promise.all([loadStats(), load()])
}

const exportCsv = async () => {
  const params = new URLSearchParams()
  if (query.keyword) params.append('keyword', query.keyword)
  if (query.roleType) params.append('roleType', query.roleType)
  if (query.status !== null && query.status !== undefined) params.append('status', String(query.status))
  if (query.replyStatus !== null && query.replyStatus !== undefined) params.append('replyStatus', String(query.replyStatus))
  if (query.range?.length === 2) {
    params.append('startTime', query.range[0])
    params.append('endTime', query.range[1])
  }

  const url = `/api/admin/feedback/export${params.toString() ? `?${params.toString()}` : ''}`
  const res = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${authStore.token}`
    }
  })

  if (!res.ok) {
    ElMessage.error('导出失败')
    return
  }

  const blob = await res.blob()
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)
  link.href = objectUrl
  link.download = `feedback_export_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
  ElMessage.success('导出成功')
}

const resetQuery = async () => {
  Object.assign(query, { keyword: '', roleType: '', status: null, replyStatus: null, range: [] })
  pageNo.value = 1
  await load()
}

const mark = async (id, status) => {
  await updateFeedbackStatusApi(id, status)
  ElMessage.success('状态更新成功')
  await refreshAll()
}

const batchMark = async (status) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择数据')
    return
  }

  const actionText = status === 1 ? '已处理' : '未处理'
  try {
    await ElMessageBox.confirm(
      `确认将选中的 ${selectedRows.value.length} 条反馈标记为${actionText}吗？`,
      '批量操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  const ids = selectedRows.value.map(item => item.id)
  const res = await batchUpdateFeedbackStatusApi({ ids, status })
  const requestedCount = res.requestedCount || ids.length
  const successCount = res.successCount || 0
  const skippedCount = res.skippedCount || 0
  const failedIds = Array.isArray(res.failedIds) ? res.failedIds : []
  if (failedIds.length > 0) {
    ElMessage.warning(`批量更新完成：请求 ${requestedCount} 条，成功 ${successCount} 条，跳过 ${skippedCount} 条，失败ID: ${failedIds.join(',')}`)
  } else {
    ElMessage.success(`批量更新完成：请求 ${requestedCount} 条，成功 ${successCount} 条，跳过 ${skippedCount} 条`)
  }
  await refreshAll()
}

const batchMarkByFilter = async (targetStatus) => {
  if (!total.value) {
    ElMessage.warning('当前筛选结果为空')
    return
  }

  const actionText = targetStatus === 1 ? '已处理' : '未处理'
  try {
    await ElMessageBox.confirm(
      `确认将当前筛选结果共 ${total.value} 条反馈标记为${actionText}吗？此操作会跨分页生效。`,
      '跨页批量操作确认',
      { type: 'warning' }
    )
  } catch {
    return
  }

  const params = {
    keyword: query.keyword,
    roleType: query.roleType,
    status: query.status,
    replyStatus: query.replyStatus,
    targetStatus
  }
  if (query.range?.length === 2) {
    params.startTime = query.range[0]
    params.endTime = query.range[1]
  }

  const res = await batchUpdateFeedbackStatusByFilterApi(params)
  const requestedCount = res.requestedCount || 0
  const successCount = res.successCount || 0
  const skippedCount = res.skippedCount || 0
  const failedIds = Array.isArray(res.failedIds) ? res.failedIds : []

  if (failedIds.length > 0) {
    ElMessage.warning(
      `跨页批量完成：请求 ${requestedCount} 条，成功 ${successCount} 条，跳过 ${skippedCount} 条，失败ID: ${failedIds.join(',')}`
    )
  } else {
    ElMessage.success(`跨页批量完成：请求 ${requestedCount} 条，成功 ${successCount} 条，跳过 ${skippedCount} 条`)
  }
  await refreshAll()
}

const openReply = (row) => {
  Object.assign(replyForm, {
    id: row.id,
    senderUsername: row.senderUsername,
    content: row.content,
    status: row.status,
    replyContent: row.replyContent || ''
  })
  replyVisible.value = true
}

const submitReply = async () => {
  const replyContent = (replyForm.replyContent || '').trim()
  if (!replyContent) {
    ElMessage.warning('回复内容不能为空')
    return
  }
  await replyFeedbackApi({ id: replyForm.id, status: replyForm.status, replyContent })
  ElMessage.success('回复成功')
  replyVisible.value = false
  await refreshAll()
}

onMounted(() => {
  refreshAll()
  timer = window.setInterval(refreshAll, 10000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped>
.feedback-admin-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.stat-card {
  min-height: 90px;
}

.stat-label {
  color: #6b7280;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  font-size: 26px;
  font-weight: 700;
  color: #111827;
}

.stat-value.warn {
  color: #d97706;
}

.stat-value.role {
  display: flex;
  gap: 16px;
  font-size: 18px;
}

.trend-chart {
  width: 100%;
  height: 260px;
}
</style>
