<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>系统操作日志</span>
        <div style="display:flex; gap:8px; align-items:center; margin-left:auto; margin-right:8px;">
          <el-input v-model="query.keyword" placeholder="用户/路径/信息关键字" clearable style="width:240px" />
          <el-select v-model="query.roleType" placeholder="角色" clearable style="width:120px">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="医生" value="DOCTOR" />
            <el-option label="患者" value="PATIENT" />
          </el-select>
          <el-select v-model="query.success" placeholder="结果" clearable style="width:120px">
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
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
          <el-button @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-input-number v-model="warnThreshold" :min="1" :max="99999" :step="100" controls-position="right" style="width:140px" @change="onThresholdChange" />
          <span style="font-size:12px;color:#6b7280;">偏慢阈值(ms)</span>
          <el-input-number v-model="dangerThreshold" :min="2" :max="99999" :step="100" controls-position="right" style="width:140px" @change="onThresholdChange" />
          <span style="font-size:12px;color:#6b7280;">严重阈值(ms)</span>
          <el-select v-model="exportLimit" style="width:130px">
            <el-option label="导出200条" :value="200" />
            <el-option label="导出1000条" :value="1000" />
            <el-option label="导出3000条" :value="3000" />
            <el-option label="导出5000条" :value="5000" />
          </el-select>
          <el-button type="primary" @click="exportLogs">导出CSV</el-button>
          <el-button type="info" plain @click="filterLogExport">导出日志</el-button>
          <el-button type="danger" plain @click="showSlowExports">慢导出优先</el-button>
          <el-button type="primary" plain @click="filterFeedbackBatch">反馈批量日志</el-button>
          <el-button type="success" plain @click="filterSelectionBatch">勾选批量</el-button>
          <el-button type="warning" plain @click="filterCrossPageBatch">跨页批量</el-button>
        </div>
        <el-button @click="load">刷新</el-button>
      </div>
    </template>

    <el-table :data="logs" border>
      <el-table-column prop="createTime" label="时间" width="180" />
      <el-table-column prop="username" label="用户" width="140" />
      <el-table-column prop="roleType" label="角色" width="120" />
      <el-table-column prop="requestMethod" label="方法" width="90" />
      <el-table-column prop="requestUri" label="路径" />
      <el-table-column label="批量类型" width="120">
        <template #default="scope">
          <el-tag v-if="getBatchType(scope.row) === 'SELECTION'" type="success">勾选批量</el-tag>
          <el-tag v-else-if="getBatchType(scope.row) === 'FILTER'" type="warning">跨页批量</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="导出请求/实际" width="140" :sort-method="sortByRequestedLimit" sortable>
        <template #default="scope">
          <span v-if="getExportMetrics(scope.row)">{{ getExportMetrics(scope.row).requestedLimit }}/{{ getExportMetrics(scope.row).effectiveLimit }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="导出条数" width="100" :sort-method="sortByExportedRows" sortable>
        <template #default="scope">
          <span v-if="getExportMetrics(scope.row)">{{ getExportMetrics(scope.row).exportedRows }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="耗时(ms)" width="100" :sort-method="sortByDurationMs" sortable>
        <template #default="scope">
          <el-tag v-if="getExportMetrics(scope.row)" :type="getDurationTagType(getExportMetrics(scope.row).durationMs)">
            {{ getExportMetrics(scope.row).durationMs }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.success === 1 ? 'success' : 'danger'">{{ scope.row.success === 1 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="信息" width="160" />
    </el-table>

    <div style="margin-top: 12px; display:flex; justify-content:flex-end;">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="load"
        @current-change="load"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOperationLogsPageApi } from '../api/modules'
import { authStore } from '../stores/auth'

const logs = ref([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const exportLimit = ref(1000)
const warnThreshold = ref(500)
const dangerThreshold = ref(1000)
const query = ref({ keyword: '', roleType: '', success: null, range: [] })
let timer = null

const LOG_WARN_THRESHOLD_KEY = 'logWarnThresholdMs'
const LOG_DANGER_THRESHOLD_KEY = 'logDangerThresholdMs'

const formatDateTime = (date) => {
  const y = date.getFullYear()
  const m = `${date.getMonth() + 1}`.padStart(2, '0')
  const d = `${date.getDate()}`.padStart(2, '0')
  const hh = `${date.getHours()}`.padStart(2, '0')
  const mm = `${date.getMinutes()}`.padStart(2, '0')
  const ss = `${date.getSeconds()}`.padStart(2, '0')
  return `${y}-${m}-${d} ${hh}:${mm}:${ss}`
}

const recent7DaysRange = () => {
  const end = new Date()
  const start = new Date(end.getTime() - 6 * 24 * 60 * 60 * 1000)
  start.setHours(0, 0, 0, 0)
  return [formatDateTime(start), formatDateTime(end)]
}

const load = async () => {
  const params = {
    pageNo: pageNo.value,
    pageSize: pageSize.value,
    keyword: query.value.keyword,
    roleType: query.value.roleType,
    success: query.value.success
  }
  if (query.value.range?.length === 2) {
    params.startTime = query.value.range[0]
    params.endTime = query.value.range[1]
  }

  const res = await listOperationLogsPageApi(params)
  logs.value = res.records || []
  total.value = res.total || 0
}

const search = async () => {
  pageNo.value = 1
  await load()
}

const resetQuery = async () => {
  query.value = { keyword: '', roleType: '', success: null, range: recent7DaysRange() }
  pageNo.value = 1
  await load()
}

const filterFeedbackBatch = async () => {
  query.value.keyword = 'feedback-batch'
  pageNo.value = 1
  await load()
}

const filterSelectionBatch = async () => {
  query.value.keyword = 'by-selection'
  pageNo.value = 1
  await load()
}

const filterCrossPageBatch = async () => {
  query.value.keyword = 'by-filter'
  pageNo.value = 1
  await load()
}

const filterLogExport = async () => {
  query.value.keyword = 'log-export'
  pageNo.value = 1
  await load()
}

const showSlowExports = async () => {
  query.value.keyword = 'log-export'
  pageNo.value = 1
  await load()
  logs.value = [...logs.value].sort((a, b) => metricNumber(b, 'durationMs') - metricNumber(a, 'durationMs'))
}

const getBatchType = (row) => {
  const message = row?.message || ''
  if (message.includes('by-selection')) {
    return 'SELECTION'
  }
  if (message.includes('by-filter')) {
    return 'FILTER'
  }
  return ''
}

const getExportMetrics = (row) => {
  const message = row?.message || ''
  if (!message.includes('log-export')) {
    return null
  }

  const requestedLimit = (message.match(/requestedLimit=(\d+)/) || [])[1]
  const effectiveLimit = (message.match(/effectiveLimit=(\d+)/) || [])[1]
  const exportedRows = (message.match(/exportedRows=(\d+)/) || [])[1]
  const durationMs = (message.match(/durationMs=(\d+)/) || [])[1]

  if (!requestedLimit || !effectiveLimit || !exportedRows || !durationMs) {
    return null
  }

  return {
    requestedLimit,
    effectiveLimit,
    exportedRows,
    durationMs
  }
}

const metricNumber = (row, key) => {
  const metrics = getExportMetrics(row)
  if (!metrics) {
    return -1
  }
  const n = Number(metrics[key])
  return Number.isNaN(n) ? -1 : n
}

const sortByRequestedLimit = (a, b) => metricNumber(a, 'requestedLimit') - metricNumber(b, 'requestedLimit')
const sortByExportedRows = (a, b) => metricNumber(a, 'exportedRows') - metricNumber(b, 'exportedRows')
const sortByDurationMs = (a, b) => metricNumber(a, 'durationMs') - metricNumber(b, 'durationMs')

const getDurationTagType = (duration) => {
  const n = Number(duration)
  if (Number.isNaN(n)) {
    return 'info'
  }
  if (n >= dangerThreshold.value) {
    return 'danger'
  }
  if (n >= warnThreshold.value) {
    return 'warning'
  }
  return 'success'
}

const onThresholdChange = () => {
  if (warnThreshold.value >= dangerThreshold.value) {
    dangerThreshold.value = warnThreshold.value + 1
  }
  localStorage.setItem(LOG_WARN_THRESHOLD_KEY, String(warnThreshold.value))
  localStorage.setItem(LOG_DANGER_THRESHOLD_KEY, String(dangerThreshold.value))
}

const exportLogs = async () => {
  if (exportLimit.value > 1000) {
    try {
      await ElMessageBox.confirm(
        `当前导出条数为 ${exportLimit.value}，可能耗时较长，是否继续？`,
        '导出确认',
        { type: 'warning' }
      )
    } catch {
      return
    }
  }

  const params = new URLSearchParams()
  params.append('limit', String(exportLimit.value))
  if (query.value.keyword) params.append('keyword', query.value.keyword)
  if (query.value.roleType) params.append('roleType', query.value.roleType)
  if (query.value.success !== null && query.value.success !== undefined) {
    params.append('success', String(query.value.success))
  }
  if (query.value.range?.length === 2) {
    params.append('startTime', query.value.range[0])
    params.append('endTime', query.value.range[1])
  }

  const url = `/api/admin/logs/export?${params.toString()}`
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
  const requestedLimit = Number(res.headers.get('X-Requested-Limit') || exportLimit.value)
  const effectiveLimit = Number(res.headers.get('X-Effective-Limit') || exportLimit.value)
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = `operation_logs_${new Date().toISOString().slice(0, 10)}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(objectUrl)
  if (effectiveLimit < requestedLimit) {
    ElMessage.warning(`导出成功，已按上限裁剪：请求 ${requestedLimit} 条，实际 ${effectiveLimit} 条`)
  } else {
    ElMessage.success(`导出成功：${effectiveLimit} 条`)
  }
}

onMounted(() => {
  const savedWarn = Number(localStorage.getItem(LOG_WARN_THRESHOLD_KEY))
  const savedDanger = Number(localStorage.getItem(LOG_DANGER_THRESHOLD_KEY))
  if (!Number.isNaN(savedWarn) && savedWarn > 0) {
    warnThreshold.value = savedWarn
  }
  if (!Number.isNaN(savedDanger) && savedDanger > warnThreshold.value) {
    dangerThreshold.value = savedDanger
  }
  query.value.range = recent7DaysRange()
  load()
  timer = window.setInterval(load, 5000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>
