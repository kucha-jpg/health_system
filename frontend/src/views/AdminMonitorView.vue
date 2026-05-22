<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">系统监控总览</h3>
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

    <el-row v-else :gutter="10" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card">系统用户总数：{{ overview.totalUsers || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card">健康上报总数：{{ overview.totalHealthData || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card summary-stat-card--warn">未处理预警：{{ overview.openAlerts || 0 }}</el-card></el-col>
    </el-row>

    <!-- Full-width 14-day trend -->
    <el-card class="section-card" shadow="never" v-loading="loading">
      <template #header>近14天上报趋势</template>
      <div ref="dailyTrendRef" class="chart-main chart-main--tall"></div>
    </el-card>

    <!-- Pie + Bar side by side -->
    <el-row :gutter="12" class="chart-row" v-loading="loading">
      <el-col :xs="24" :lg="12">
        <el-card class="section-card" shadow="never">
          <template #header>指标分布</template>
          <div ref="indicatorPieRef" class="chart-main"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card class="section-card" shadow="never">
          <template #header>活跃患者 Top</template>
          <div ref="userBarRef" class="chart-main"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Data table -->
    <el-card class="section-card" shadow="never">
      <template #header>最近上报数据</template>
      <el-table :data="pagedLatestHealthData" border v-loading="loading" empty-text="暂无数据">
        <el-table-column prop="userId" label="患者ID" width="100" />
        <el-table-column prop="indicatorType" label="指标" width="120" />
        <el-table-column prop="value" label="值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" />
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="latestPageNo"
          v-model:page-size="latestPageSize"
          :page-sizes="[5, 10, 20]"
          :total="latestHealthData.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import echarts from '../utils/echarts'
import { csvEscape, downloadCsv } from '../utils/csv'
import { showSuccess, showError } from '../utils/message'
import { getMonitorOverviewApi } from '../api/modules'
import { CHART_PALETTE, CHART_SPLIT_LINE, toIndicatorLabel } from '../constants/chart-theme'
import { showFirstVisitGuide } from '../composables/useFirstVisitGuide'

const overview = ref({})
const loading = ref(false)
const dailyTrendRef = ref(null)
const indicatorPieRef = ref(null)
const userBarRef = ref(null)
const latestPageNo = ref(1)
const latestPageSize = ref(10)
let dailyTrendChart = null
let indicatorPieChart = null
let userBarChart = null

const latestHealthData = computed(() => overview.value?.latestHealthData || [])
const pagedLatestHealthData = computed(() => {
  const start = (latestPageNo.value - 1) * latestPageSize.value
  return latestHealthData.value.slice(start, start + latestPageSize.value)
})

const renderCharts = async () => {
  await nextTick()

  if (dailyTrendRef.value) {
    if (!dailyTrendChart) dailyTrendChart = echarts.init(dailyTrendRef.value)
    dailyTrendChart.clear()
    const trend = overview.value?.dailyReportTrend || []
    dailyTrendChart.setOption({
      color: [CHART_PALETTE[1]],
      tooltip: { trigger: 'axis' },
      grid: { left: 55, right: 30, top: 28, bottom: 30 },
      xAxis: { type: 'category', data: trend.map((item) => {
        const d = item.date
        if (!d || d.length < 10) return d
        return `${parseInt(d.slice(5, 7))}月${parseInt(d.slice(8, 10))}日`
      }) },
      yAxis: { type: 'value', name: '上报数', nameTextStyle: { fontSize: 11 }, splitLine: CHART_SPLIT_LINE },
      series: [{ type: 'line', smooth: true, areaStyle: { opacity: 0.2 }, data: trend.map((item) => item.count) }]
    })
  }

  if (indicatorPieRef.value) {
    if (!indicatorPieChart) indicatorPieChart = echarts.init(indicatorPieRef.value)
    indicatorPieChart.clear()
    const source = overview.value?.indicatorDistribution || []
    indicatorPieChart.setOption({
      color: CHART_PALETTE,
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['35%', '68%'],
        center: ['50%', '45%'],
        data: source.map((item) => ({ name: toIndicatorLabel(item.indicatorType), value: item.count }))
      }]
    })
  }

  if (userBarRef.value) {
    if (!userBarChart) userBarChart = echarts.init(userBarRef.value)
    userBarChart.clear()
    const source = overview.value?.activeUserStats || []
    userBarChart.setOption({
      color: [CHART_PALETTE[2]],
      tooltip: { trigger: 'axis' },
      grid: { left: 60, right: 20, top: 28, bottom: 30 },
      xAxis: { type: 'category', data: source.map((item) => item.name || item.username) },
      yAxis: { type: 'value', name: '上报次数', nameTextStyle: { fontSize: 11 }, splitLine: CHART_SPLIT_LINE, minInterval: 1 },
      series: [{ type: 'bar', barMaxWidth: 34, data: source.map((item) => item.count), itemStyle: { borderRadius: [4, 4, 0, 0] } }]
    })
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMonitorOverviewApi()
    overview.value = res || {}
    latestPageNo.value = 1
    await renderCharts()
  } catch (err) {
    showError(err?.message || '加载监控数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
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

  lines.push('活跃患者Top')
  lines.push('用户ID,用户名,姓名,上报次数')
  ;(overview.value?.activeUserStats || []).forEach((row) => {
    lines.push(`${csvEscape(row.userId)},${csvEscape(row.username)},${csvEscape(row.name)},${csvEscape(row.count)}`)
  })

  downloadCsv(lines, `admin_monitor_${Date.now()}.csv`)
  showSuccess('监控报表导出成功')
}

const handleResize = () => {
  if (dailyTrendChart) dailyTrendChart.resize()
  if (indicatorPieChart) indicatorPieChart.resize()
  if (userBarChart) userBarChart.resize()
}

onMounted(() => {
  load()
  showFirstVisitGuide({
    storageKey: 'guide_admin_monitor_v1',
    title: '监控总览引导',
    message: '近14天趋势反映整体上报活跃度，指标分布展示各指标占比，活跃患者Top帮助快速锁定重点关注对象。'
  }).catch(() => {})
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (dailyTrendChart) { dailyTrendChart.dispose(); dailyTrendChart = null }
  if (indicatorPieChart) { indicatorPieChart.dispose(); indicatorPieChart = null }
  if (userBarChart) { userBarChart.dispose(); userBarChart = null }
})
</script>

<style scoped>
.summary-row,
.chart-row {
  margin-top: 16px;
  margin-bottom: 12px;
}

.chart-main {
  width: 100%;
  height: 300px;
}

.chart-main--tall {
  height: 360px;
}

.summary-stat-card {
  font-weight: 600;
  color: #2f4952;
}

.summary-stat-card--warn {
  color: #8a4b28;
}

.section-card {
  margin-bottom: 0;
}
</style>
