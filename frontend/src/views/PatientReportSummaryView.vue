<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">健康周报/月报</h3>
      </div>
      <div class="page-actions">
        <el-radio-group v-model="range" @change="load">
          <el-radio-button value="week">周报</el-radio-button>
          <el-radio-button value="month">月报</el-radio-button>
        </el-radio-group>
        <el-button @click="exportCsv">导出报表</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="10" class="summary-row">
      <el-col :xs="12" :sm="6"><el-card shadow="never" class="summary-stat-card">📋 上报次数：{{ summary.reportCount || 0 }}</el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never" class="summary-stat-card summary-stat-card--warn">⚠️ 预警次数：{{ summary.alertCount || 0 }}</el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never" class="summary-stat-card">📅 统计范围：{{ range === 'week' ? '本周' : '本月' }}</el-card></el-col>
      <el-col :xs="12" :sm="6"><el-card shadow="never" class="summary-stat-card">📊 指标种类：{{ indicatorTypeCount }}</el-card></el-col>
    </el-row>

    <!-- Chart 1: Daily Alert Count (replaces "risk trend") -->
    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="chart-title-row">
          <span>📈 每日预警次数</span>
          <span class="chart-subtitle">最近{{ range === 'week' ? '7天' : '30天' }}每天的预警数量</span>
        </div>
      </template>
      <div ref="alertCountRef" class="chart-main"></div>
    </el-card>

    <el-row :gutter="12" class="chart-row">
      <!-- Chart 2: Indicator Distribution (simplified pie) -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="chart-title-row">
              <span>🥧 各指标上报占比</span>
              <span class="chart-subtitle">不同指标的上报次数分布</span>
            </div>
          </template>
          <div ref="typePieRef" class="chart-side"></div>
        </el-card>
      </el-col>

      <!-- Chart 3: Indicator Frequency (bar chart replacing radar) -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never">
          <template #header>
            <div class="chart-title-row">
              <span>📊 各指标上报次数</span>
              <span class="chart-subtitle">清晰对比各指标上报频率</span>
            </div>
          </template>
          <div ref="typeBarRef" class="chart-side"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Data table -->
    <el-card shadow="never">
      <template #header>📝 最近上报记录</template>
      <div class="filter-row">
        <el-select v-model="filters.indicatorType" class="w-140" clearable placeholder="指标筛选" @change="onFilterChange">
          <el-option label="血压" value="血压" />
          <el-option label="血糖" value="血糖" />
          <el-option label="体重" value="体重" />
          <el-option label="服药" value="服药" />
        </el-select>
        <el-input v-model="filters.keyword" class="w-180" clearable placeholder="备注关键词" @input="onFilterChange" />
        <el-button @click="resetFilters">重置筛选</el-button>
      </div>
      <el-table :data="pagedLatestData" border v-loading="loading" empty-text="暂无匹配数据">
        <el-table-column prop="indicatorType" label="指标" width="100" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="latestPageNo"
          v-model:page-size="latestPageSize"
          :page-sizes="[5, 10, 20]"
          :total="filteredLatestData.length"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import echarts from '../utils/echarts'
import { csvEscape, downloadCsv } from '../utils/csv'
import { ElMessage } from 'element-plus'
import { getPatientReportSummaryApi } from '../api/modules'
import { CHART_PALETTE } from '../constants/chart-theme'

const summary = ref({})
const range = ref('week')
const loading = ref(false)
const filters = reactive({ indicatorType: '', keyword: '' })
const latestPageNo = ref(1)
const latestPageSize = ref(10)
const alertCountRef = ref(null)
const typePieRef = ref(null)
const typeBarRef = ref(null)
let alertCountChart = null
let typePieChart = null
let typeBarChart = null

const INDICATOR_LABEL_MAP = {
  BLOOD_PRESSURE: '血压',
  BLOOD_SUGAR: '血糖',
  WEIGHT: '体重',
  MEDICATION: '服药'
}

const toLabel = (v) => INDICATOR_LABEL_MAP[String(v || '').trim()] || v

const indicatorTypeCount = computed(() => {
  const byType = summary.value?.byType || {}
  return Object.keys(byType).length
})

const filteredLatestData = computed(() => {
  const list = summary.value?.latestData || []
  return list.filter((item) => {
    const indicatorOk = !filters.indicatorType || item.indicatorType === filters.indicatorType
    const keyword = (filters.keyword || '').trim().toLowerCase()
    const remark = String(item.remark || '').toLowerCase()
    return indicatorOk && (!keyword || remark.includes(keyword))
  })
})

const pagedLatestData = computed(() => {
  const start = (latestPageNo.value - 1) * latestPageSize.value
  return filteredLatestData.value.slice(start, start + latestPageSize.value)
})

// Chart 1: Daily alert count (simple bar chart)
const renderAlertCountChart = async () => {
  await nextTick()
  if (!alertCountRef.value) return
  if (!alertCountChart) alertCountChart = echarts.init(alertCountRef.value)

  const rows = summary.value?.riskTrend || []
  const xData = rows.map(r => {
    const d = r.date
    if (!d || d.length < 10) return d
    return `${parseInt(d.slice(5, 7))}月${parseInt(d.slice(8, 10))}日`
  })
  const counts = rows.map(r => r.alertCount)

  alertCountChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => `${params[0].axisValue}<br/>预警次数：<b>${params[0].value}</b> 次`
    },
    grid: { left: 45, right: 20, top: 28, bottom: 30 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', name: '次数', nameTextStyle: { fontSize: 11 }, minInterval: 1 },
    series: [{
      type: 'bar', data: counts, barMaxWidth: 36,
      itemStyle: {
        color: (params) => counts[params.dataIndex] > 0 ? '#e6a23c' : '#c0ccda',
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', distance: 4, formatter: (p) => p.value > 0 ? p.value : '' }
    }]
  })
}

// Chart 2: Simplified pie chart
const renderPieChart = async () => {
  await nextTick()
  if (!typePieRef.value) return
  if (!typePieChart) typePieChart = echarts.init(typePieRef.value)

  const byType = summary.value?.byType || {}
  const entries = Object.entries(byType)
  const pieData = entries.map(([name, value]) => ({ name: toLabel(name), value }))

  typePieChart.setOption({
    color: CHART_PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['38%', '65%'],
      center: ['50%', '42%'],
      data: pieData,
      label: { formatter: '{b}\n{d}%', fontSize: 12 },
      emphasis: { label: { fontSize: 16, fontWeight: 'bold' } }
    }]
  })
}

// Chart 3: Simple horizontal bar chart (replaces radar)
const renderBarChart = async () => {
  await nextTick()
  if (!typeBarRef.value) return
  if (!typeBarChart) typeBarChart = echarts.init(typeBarRef.value)

  const byType = summary.value?.byType || {}
  const entries = Object.entries(byType).map(([name, value]) => [toLabel(name), value])
  entries.sort((a, b) => b[1] - a[1])
  const names = entries.map(e => e[0])
  const values = entries.map(e => e[1])

  typeBarChart.setOption({
    color: [CHART_PALETTE[0]],
    tooltip: { trigger: 'axis', formatter: (params) => `${params[0].name}：<b>${params[0].value}</b> 次` },
    grid: { left: 80, right: 40, top: 10, bottom: 20 },
    xAxis: { type: 'value', name: '次数', minInterval: 1 },
    yAxis: { type: 'category', data: names },
    series: [{
      type: 'bar', data: values, barMaxWidth: 28,
      itemStyle: { borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right' }
    }]
  })
}

const renderAllCharts = async () => {
  await renderAlertCountChart()
  await renderPieChart()
  await renderBarChart()
}

const load = async () => {
  loading.value = true
  try {
    summary.value = await getPatientReportSummaryApi({ range: range.value })
    latestPageNo.value = 1
    await renderAllCharts()
  } finally {
    loading.value = false
  }
}

const onFilterChange = () => {
  latestPageNo.value = 1
}

const resetFilters = () => {
  filters.indicatorType = ''
  filters.keyword = ''
  latestPageNo.value = 1
}

const exportCsv = () => {
  const lines = []
  const now = new Date()
  const byType = summary.value?.byType || {}
  const riskTrend = summary.value?.riskTrend || []

  lines.push('报表名称,健康周报月报')
  lines.push(`导出时间,${now.toLocaleString()}`)
  lines.push(`统计范围,${range.value === 'week' ? '周报' : '月报'}`)
  lines.push(`上报总数,${summary.value?.reportCount || 0}`)
  lines.push(`预警总数,${summary.value?.alertCount || 0}`)
  lines.push('')

  lines.push('各指标上报次数')
  lines.push('指标,次数')
  Object.entries(byType).forEach(([k, v]) => lines.push(`${csvEscape(k)},${csvEscape(v)}`))
  lines.push('')

  lines.push('每日预警情况')
  lines.push('日期,预警次数,平均风险分')
  riskTrend.forEach(row => lines.push(`${csvEscape(row.date)},${csvEscape(row.alertCount)},${csvEscape(row.avgRiskScore)}`))
  lines.push('')

  lines.push('最近上报记录')
  lines.push('指标,数值,上报时间,备注')
  filteredLatestData.value.forEach(row => {
    lines.push([csvEscape(row.indicatorType), csvEscape(row.value), csvEscape(row.reportTime), csvEscape(row.remark)].join(','))
  })

  const csvText = `﻿${lines.join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `health_report_${range.value}_${Date.now()}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('报表导出成功')
}

const handleResize = () => {
  alertCountChart?.resize()
  typePieChart?.resize()
  typeBarChart?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  alertCountChart?.dispose(); alertCountChart = null
  typePieChart?.dispose(); typePieChart = null
  typeBarChart?.dispose(); typeBarChart = null
})
</script>

<style scoped>
.summary-row {
  margin: 12px 0;
}

.summary-stat-card {
  font-weight: 600;
  color: #2f4952;
  font-size: 14px;
}

.summary-stat-card--warn {
  color: #8a4b28;
}

.chart-row {
  margin-bottom: 12px;
}

.chart-main {
  width: 100%;
  height: 340px;
}

.chart-side {
  width: 100%;
  height: 320px;
}

.chart-title-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.chart-subtitle {
  font-size: 12px;
  font-weight: 400;
  color: var(--ink-2);
}

.section-card {
  margin-bottom: 12px;
}

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
