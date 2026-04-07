<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">健康周报/月报</h3>
        <p class="page-subtitle">支持多图表联动、维度筛选与自定义报表视角</p>
      </div>
      <div class="page-actions">
        <el-radio-group v-model="range" @change="load">
          <el-radio-button label="week">周报</el-radio-button>
          <el-radio-button label="month">月报</el-radio-button>
        </el-radio-group>
        <el-button @click="exportCsv">导出当前报表</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="filter-row">
      <el-select v-model="filters.indicatorType" clearable placeholder="指标筛选" style="width: 140px">
        <el-option label="血压" value="血压" />
        <el-option label="血糖" value="血糖" />
        <el-option label="体重" value="体重" />
        <el-option label="服药" value="服药" />
      </el-select>
      <el-input v-model="filters.keyword" clearable placeholder="备注关键词" style="width: 180px" />
      <el-segmented v-model="riskChartType" :options="['line', 'bar']" />
      <el-button @click="resetFilters">重置筛选</el-button>
    </div>

    <el-row :gutter="12" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card>上报总数：{{ summary.reportCount || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card>预警总数：{{ summary.alertCount || 0 }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card>统计范围：{{ summary.range || '-' }}</el-card></el-col>
    </el-row>

    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="14">
        <el-card>
          <template #header>风险趋势（交互缩放）</template>
          <div ref="riskTrendRef" class="chart-main"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card>
          <template #header>指标分布（饼图）</template>
          <div ref="typePieRef" class="chart-side"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-bottom: 12px">
      <template #header>指标分布（雷达图）</template>
      <div ref="typeRadarRef" class="chart-main"></div>
    </el-card>

    <el-card>
      <template #header>最近上报（自定义筛选）</template>
      <el-table :data="filteredLatestData" border v-loading="loading" empty-text="暂无匹配数据">
        <el-table-column prop="indicatorType" label="指标" width="100" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getPatientReportSummaryApi } from '../api/modules'
import { CHART_PALETTE, CHART_SPLIT_LINE, RISK_COLORS } from '../constants/chart-theme'
import { showFirstVisitGuide } from '../composables/useFirstVisitGuide'

const summary = ref({})
const range = ref('week')
const loading = ref(false)
const filters = reactive({ indicatorType: '', keyword: '' })
const riskChartType = ref('line')
const riskTrendRef = ref(null)
const typePieRef = ref(null)
const typeRadarRef = ref(null)
let riskTrendChart = null
let typePieChart = null
let typeRadarChart = null

const filteredLatestData = computed(() => {
  const list = summary.value?.latestData || []
  return list.filter((item) => {
    const indicatorOk = !filters.indicatorType || item.indicatorType === filters.indicatorType
    const keyword = (filters.keyword || '').trim().toLowerCase()
    const remark = String(item.remark || '').toLowerCase()
    const keywordOk = !keyword || remark.includes(keyword)
    return indicatorOk && keywordOk
  })
})

const renderRiskChart = async () => {
  await nextTick()
  if (!riskTrendRef.value) return
  if (!riskTrendChart) {
    riskTrendChart = echarts.init(riskTrendRef.value)
  }

  const rows = summary.value?.riskTrend || []
  const xAxis = rows.map((item) => item.date)
  const scoreSeries = rows.map((item) => item.avgRiskScore)
  const countSeries = rows.map((item) => item.alertCount)

  riskTrendChart.setOption({
    color: [CHART_PALETTE[1], RISK_COLORS.MEDIUM],
    tooltip: { trigger: 'axis' },
    toolbox: {
      feature: {
        dataZoom: { yAxisIndex: 'none' },
        restore: {},
        saveAsImage: {}
      }
    },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    legend: { data: ['平均风险分', '预警数'] },
    xAxis: { type: 'category', data: xAxis },
    yAxis: [
      { type: 'value', name: '平均风险分', min: 0, max: 100, splitLine: CHART_SPLIT_LINE },
      { type: 'value', name: '预警数', splitLine: CHART_SPLIT_LINE }
    ],
    series: [
      {
        name: '平均风险分',
        type: riskChartType.value,
        smooth: riskChartType.value === 'line',
        data: scoreSeries,
        itemStyle: { color: CHART_PALETTE[1] },
        lineStyle: { width: 3 }
      },
      {
        name: '预警数',
        type: 'bar',
        yAxisIndex: 1,
        data: countSeries,
        barMaxWidth: 24,
        itemStyle: { color: RISK_COLORS.MEDIUM }
      }
    ]
  })
}

const renderTypeCharts = async () => {
  await nextTick()
  const byType = summary.value?.byType || {}
  const entries = Object.entries(byType)
  if (!typePieRef.value || !typeRadarRef.value) return

  if (!typePieChart) typePieChart = echarts.init(typePieRef.value)
  if (!typeRadarChart) typeRadarChart = echarts.init(typeRadarRef.value)

  const pieData = entries.map(([name, value]) => ({ name, value }))
  typePieChart.setOption({
    color: CHART_PALETTE,
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['35%', '68%'],
      center: ['50%', '45%'],
      data: pieData,
      label: { formatter: '{b}: {d}%' }
    }]
  })

  const max = Math.max(...entries.map(([, value]) => Number(value)), 1)
  typeRadarChart.setOption({
    color: [CHART_PALETTE[0]],
    tooltip: {},
    radar: {
      radius: '65%',
      indicator: entries.map(([name]) => ({ name, max }))
    },
    series: [{
      type: 'radar',
      areaStyle: { opacity: 0.28 },
      data: [{ value: entries.map(([, value]) => value), name: '指标频次' }]
    }]
  })
}

const load = async () => {
  loading.value = true
  try {
    summary.value = await getPatientReportSummaryApi({ range: range.value })
    await renderRiskChart()
    await renderTypeCharts()
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.indicatorType = ''
  filters.keyword = ''
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
  const now = new Date()
  const byType = summary.value?.byType || {}
  const riskTrend = summary.value?.riskTrend || []

  lines.push('报表名称,健康周报月报')
  lines.push(`导出时间,${now.toLocaleString()}`)
  lines.push(`统计范围,${summary.value?.range || '-'}`)
  lines.push(`上报总数,${summary.value?.reportCount || 0}`)
  lines.push(`预警总数,${summary.value?.alertCount || 0}`)
  lines.push('')

  lines.push('指标分布')
  lines.push('指标,次数')
  Object.entries(byType).forEach(([k, v]) => {
    lines.push(`${csvEscape(k)},${csvEscape(v)}`)
  })
  lines.push('')

  lines.push('风险趋势')
  lines.push('日期,平均风险分,预警数')
  riskTrend.forEach((row) => {
    lines.push(`${csvEscape(row.date)},${csvEscape(row.avgRiskScore)},${csvEscape(row.alertCount)}`)
  })
  lines.push('')

  lines.push('最近上报(当前筛选结果)')
  lines.push('指标,数值,上报时间,备注')
  filteredLatestData.value.forEach((row) => {
    lines.push([
      csvEscape(row.indicatorType),
      csvEscape(row.value),
      csvEscape(row.reportTime),
      csvEscape(row.remark)
    ].join(','))
  })

  const csvText = `\uFEFF${lines.join('\n')}`
  const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `patient_report_${summary.value?.range || 'week'}_${Date.now()}.csv`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  ElMessage.success('报表导出成功')
}

watch(riskChartType, () => {
  renderRiskChart()
})

const handleResize = () => {
  if (riskTrendChart) riskTrendChart.resize()
  if (typePieChart) typePieChart.resize()
  if (typeRadarChart) typeRadarChart.resize()
}

onMounted(() => {
  load()
  showFirstVisitGuide({
    storageKey: 'guide_patient_report_summary_v1',
    title: '周报/月报引导',
    message: '可先切换周报或月报，再用图表类型切换观察风险变化；筛选备注关键词后导出的 CSV 将与当前视图保持一致。'
  }).catch(() => {})
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (riskTrendChart) {
    riskTrendChart.dispose()
    riskTrendChart = null
  }
  if (typePieChart) {
    typePieChart.dispose()
    typePieChart = null
  }
  if (typeRadarChart) {
    typeRadarChart.dispose()
    typeRadarChart = null
  }
})
</script>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-row {
  margin: 12px 0;
}

.chart-row {
  margin-bottom: 12px;
}

.chart-main {
  width: 100%;
  height: 320px;
}

.chart-side {
  width: 100%;
  height: 320px;
}
</style>
