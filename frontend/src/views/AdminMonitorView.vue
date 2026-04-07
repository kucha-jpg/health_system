<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">系统监控总览</h3>
        <p class="page-subtitle">支持时间、群组、用户、指标四个维度的数据洞察</p>
      </div>
      <div class="page-actions">
        <el-button @click="exportCsv">导出监控概览</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-if="loading" class="skeleton-grid summary-row">
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
      <div class="skeleton-card"></div>
    </div>

    <el-row v-else :gutter="12" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card>系统用户总数：{{ overview.totalUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card>健康上报总数：{{ overview.totalHealthData || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card>未处理预警：{{ overview.openAlerts || 0 }}</el-card></el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row" v-loading="loading">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>近14天上报趋势（时间维度）</template>
          <div ref="dailyTrendRef" class="chart-main"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>指标分布（饼图）</template>
          <div ref="indicatorPieRef" class="chart-main"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row" v-loading="loading">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>群组患者规模（群组维度）</template>
          <div ref="groupBarRef" class="chart-main"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header>活跃患者 Top（用户维度）</template>
          <div ref="userBarRef" class="chart-main"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card>
      <template #header>最近上报数据</template>
      <el-table :data="overview.latestHealthData || []" border v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="userId" label="患者ID" width="100" />
        <el-table-column prop="indicatorType" label="指标" width="120" />
        <el-table-column prop="value" label="值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" />
      </el-table>
    </el-card>
  </el-card>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getMonitorOverviewApi } from '../api/modules'
import { CHART_PALETTE, CHART_SPLIT_LINE } from '../constants/chart-theme'
import { showFirstVisitGuide } from '../composables/useFirstVisitGuide'

const overview = ref({})
const loading = ref(false)
const dailyTrendRef = ref(null)
const indicatorPieRef = ref(null)
const groupBarRef = ref(null)
const userBarRef = ref(null)
let dailyTrendChart = null
let indicatorPieChart = null
let groupBarChart = null
let userBarChart = null

const renderCharts = async () => {
  await nextTick()

  if (dailyTrendRef.value) {
    if (!dailyTrendChart) dailyTrendChart = echarts.init(dailyTrendRef.value)
    const trend = overview.value?.dailyReportTrend || []
    dailyTrendChart.setOption({
      color: [CHART_PALETTE[1]],
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trend.map((item) => item.date) },
      yAxis: { type: 'value', name: '上报数', splitLine: CHART_SPLIT_LINE },
      series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.2 }, data: trend.map((item) => item.count) }]
    })
  }

  if (indicatorPieRef.value) {
    if (!indicatorPieChart) indicatorPieChart = echarts.init(indicatorPieRef.value)
    const source = overview.value?.indicatorDistribution || []
    indicatorPieChart.setOption({
      color: CHART_PALETTE,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '68%'],
        data: source.map((item) => ({ name: item.indicatorType, value: item.count }))
      }]
    })
  }

  if (groupBarRef.value) {
    if (!groupBarChart) groupBarChart = echarts.init(groupBarRef.value)
    const source = overview.value?.groupStats || []
    groupBarChart.setOption({
      color: [CHART_PALETTE[0]],
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: source.map((item) => item.groupName) },
      yAxis: { type: 'value', name: '患者数', splitLine: CHART_SPLIT_LINE },
      series: [{ type: 'bar', barMaxWidth: 34, data: source.map((item) => item.patientCount) }]
    })
  }

  if (userBarRef.value) {
    if (!userBarChart) userBarChart = echarts.init(userBarRef.value)
    const source = overview.value?.activeUserStats || []
    userBarChart.setOption({
      color: [CHART_PALETTE[2]],
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: source.map((item) => item.name || item.username) },
      yAxis: { type: 'value', name: '上报次数', splitLine: CHART_SPLIT_LINE },
      series: [{ type: 'bar', barMaxWidth: 34, data: source.map((item) => item.count) }]
    })
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMonitorOverviewApi()
    overview.value = res || {}
    await renderCharts()
  } finally {
    loading.value = false
  }
}

const csvEscape = (value) => {
  const text = String(value ?? '')
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

const exportCsv = () => {
  const lines = []
  const now = new Date().toLocaleString()
  lines.push('报表名称,系统监控概览')
  lines.push(`导出时间,${now}`)
  lines.push(`系统用户总数,${overview.value?.totalUsers || 0}`)
  lines.push(`健康上报总数,${overview.value?.totalHealthData || 0}`)
  lines.push(`未处理预警,${overview.value?.openAlerts || 0}`)
  lines.push('')

  lines.push('指标分布')
  lines.push('指标,次数')
  ;(overview.value?.indicatorDistribution || []).forEach((row) => {
    lines.push(`${csvEscape(row.indicatorType)},${csvEscape(row.count)}`)
  })
  lines.push('')

  lines.push('近14天上报趋势')
  lines.push('日期,上报数')
  ;(overview.value?.dailyReportTrend || []).forEach((row) => {
    lines.push(`${csvEscape(row.date)},${csvEscape(row.count)}`)
  })
  lines.push('')

  lines.push('群组规模')
  lines.push('群组ID,群组名称,患者数')
  ;(overview.value?.groupStats || []).forEach((row) => {
    lines.push(`${csvEscape(row.groupId)},${csvEscape(row.groupName)},${csvEscape(row.patientCount)}`)
  })
  lines.push('')

  lines.push('活跃患者Top')
  lines.push('用户ID,用户名,姓名,上报次数')
  ;(overview.value?.activeUserStats || []).forEach((row) => {
    lines.push(`${csvEscape(row.userId)},${csvEscape(row.username)},${csvEscape(row.name)},${csvEscape(row.count)}`)
  })

  const csvText = `\uFEFF${lines.join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `admin_monitor_${Date.now()}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('监控报表导出成功')
}

const handleResize = () => {
  if (dailyTrendChart) dailyTrendChart.resize()
  if (indicatorPieChart) indicatorPieChart.resize()
  if (groupBarChart) groupBarChart.resize()
  if (userBarChart) userBarChart.resize()
}

onMounted(() => {
  load()
  showFirstVisitGuide({
    storageKey: 'guide_admin_monitor_v1',
    title: '监控总览引导',
    message: '建议先看近14天趋势，再结合指标分布和群组规模定位异常区域，最后通过活跃患者Top快速锁定重点对象。'
  }).catch(() => {})
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (dailyTrendChart) dailyTrendChart.dispose()
  if (indicatorPieChart) indicatorPieChart.dispose()
  if (groupBarChart) groupBarChart.dispose()
  if (userBarChart) userBarChart.dispose()
})
</script>

<style scoped>
.summary-row,
.chart-row {
  margin-bottom: 12px;
}

.chart-main {
  width: 100%;
  height: 300px;
}
</style>
