<template>
  <el-card class="page-shell">
    <div class="page-header">
      <h3 class="page-title">健康周报/月报</h3>
      <div class="page-actions">
        <el-radio-group v-model="range" @change="load">
          <el-radio-button value="week">周报</el-radio-button>
          <el-radio-button value="month">月报</el-radio-button>
        </el-radio-group>
        <el-button @click="exportCsv">导出报表</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <!-- Hero: Comprehensive Score -->
    <el-tooltip :content="scoreTooltip" placement="bottom" effect="dark" raw-content>
      <el-card shadow="hover" :class="['hero-score-card', heroScoreClass]">
        <div class="hero-score-body">
          <div class="hero-score-left">
            <div class="hero-score-label">综合风险评分</div>
            <div class="hero-score-number">{{ compScore.totalScore ?? '--' }}</div>
            <div class="hero-score-tag" :style="heroScoreTagStyle">{{ compScore.riskLevelLabel ?? '加载中' }}</div>
          </div>
          <div class="hero-score-right">
            <div
              v-for="d in compScore.details"
              :key="d.indicatorType"
              class="hero-indicator-item"
            >
              <span class="hero-indicator-name">{{ d.indicatorType }}</span>
              <span class="hero-indicator-score">{{ d.hasData ? d.riskScore + '分' : '无数据' }}</span>
            </div>
          </div>
        </div>
      </el-card>
    </el-tooltip>

    <!-- Stat cards -->
    <el-row :gutter="12" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon stat-icon--report">📋</div>
          <div class="stat-body">
            <div class="stat-label">上报次数</div>
            <div class="stat-value">{{ summary.reportCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon stat-icon--alert">⚠️</div>
          <div class="stat-body">
            <div class="stat-label">预警次数</div>
            <div class="stat-value stat-value--alert">{{ summary.alertCount ?? 0 }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon stat-icon--range">📅</div>
          <div class="stat-body">
            <div class="stat-label">统计范围</div>
            <div class="stat-value">{{ rangeLabel }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-icon stat-icon--type">📊</div>
          <div class="stat-body">
            <div class="stat-label">指标种类</div>
            <div class="stat-value">{{ indicatorTypeCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Chart: Daily Alert Count -->
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
        <el-select
          v-model="filters.indicatorType"
          class="w-140"
          clearable
          placeholder="指标筛选"
          @change="onFilterChange"
        >
          <el-option
            v-for="item in FILTER_INDICATORS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="filters.keyword"
          class="w-180"
          clearable
          placeholder="备注关键词"
          @input="onFilterChange"
        />
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
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import echarts from '../utils/echarts'
import { csvEscape, downloadCsv } from '../utils/csv'
import { getPatientReportSummaryApi, getPatientComprehensiveScoreApi } from '../api/modules'
import { CHART_PALETTE } from '../constants/chart-theme'

// ==================== Constants ====================

const FILTER_INDICATORS = [
  { label: '血压', value: '血压' },
  { label: '血糖', value: '血糖' },
  { label: '体重', value: '体重' },
  { label: '服药', value: '服药' },
]

const INDICATOR_LABEL_MAP = {
  BLOOD_PRESSURE: '血压',
  BLOOD_SUGAR: '血糖',
  WEIGHT: '体重',
  MEDICATION: '服药',
}

const RISK_LEVEL_STYLE = {
  HIGH: { cardClass: 'hero-score--danger', tagBg: '#f56c6c', tagColor: '#fff' },
  MEDIUM: { cardClass: 'hero-score--warn', tagBg: '#e6a23c', tagColor: '#fff' },
  LOW: { cardClass: 'hero-score--safe', tagBg: '#67c23a', tagColor: '#fff' },
}

const SCORE_TOOLTIP = `
  <div style="line-height:1.8;font-size:13px;">
    <b>综合风险评分说明</b><br/>
    <span style="color:#f56c6c;">80-100 分</span>：高风险 — 多项指标异常，需紧急干预<br/>
    <span style="color:#e6a23c;">50-79 分</span>：中风险 — 部分指标需关注<br/>
    <span style="color:#67c23a;">0-49 分</span>：低风险 — 各项指标基本正常
  </div>`

// ==================== Reactive state ====================

const summary = ref({})
const compScore = ref({})
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

// ==================== Computed ====================

const toLabel = (v) => INDICATOR_LABEL_MAP[String(v ?? '').trim()] || v

const indicatorTypeCount = computed(() => Object.keys(summary.value?.byType ?? {}).length)

const rangeLabel = computed(() => (range.value === 'week' ? '本周' : '本月'))

const riskStyle = computed(() => RISK_LEVEL_STYLE[compScore.value?.riskLevel] ?? RISK_LEVEL_STYLE.LOW)

const heroScoreClass = computed(() => riskStyle.value.cardClass)

const heroScoreTagStyle = computed(() => ({
  backgroundColor: riskStyle.value.tagBg,
  color: riskStyle.value.tagColor,
}))

const scoreTooltip = computed(() => SCORE_TOOLTIP)

const filteredLatestData = computed(() => {
  const list = summary.value?.latestData ?? []
  const keyword = (filters.keyword ?? '').trim().toLowerCase()
  return list.filter((item) => {
    if (filters.indicatorType && item.indicatorType !== filters.indicatorType) return false
    if (keyword && !(item.remark ?? '').toLowerCase().includes(keyword)) return false
    return true
  })
})

const pagedLatestData = computed(() => {
  const start = (latestPageNo.value - 1) * latestPageSize.value
  return filteredLatestData.value.slice(start, start + latestPageSize.value)
})

// ==================== Chart rendering ====================

function formatDateLabel(dateStr) {
  if (!dateStr || dateStr.length < 10) return dateStr ?? ''
  const month = parseInt(dateStr.slice(5, 7), 10)
  const day = parseInt(dateStr.slice(8, 10), 10)
  return `${month}月${day}日`
}

async function getOrInitChart(refEl, existing) {
  await nextTick()
  if (!refEl.value) return null
  if (existing && !existing.isDisposed()) return existing
  return echarts.init(refEl.value)
}

async function renderAlertCountChart() {
  const chart = await getOrInitChart(alertCountRef, alertCountChart)
  if (!chart) return
  alertCountChart = chart

  const rows = summary.value?.riskTrend ?? []
  const xData = rows.map((r) => formatDateLabel(r.date))
  const counts = rows.map((r) => r.alertCount)

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => `${params[0].axisValue}<br/>预警次数：<b>${params[0].value}</b> 次`,
    },
    grid: { left: 45, right: 20, top: 28, bottom: 30 },
    xAxis: { type: 'category', data: xData, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', name: '次数', nameTextStyle: { fontSize: 11 }, minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: counts,
        barMaxWidth: 36,
        itemStyle: {
          color: (params) => (counts[params.dataIndex] > 0 ? '#e6a23c' : '#c0ccda'),
          borderRadius: [4, 4, 0, 0],
        },
        label: {
          show: true,
          position: 'top',
          distance: 4,
          formatter: (p) => (p.value > 0 ? p.value : ''),
        },
      },
    ],
  })
}

async function renderPieChart() {
  const chart = await getOrInitChart(typePieRef, typePieChart)
  if (!chart) return
  typePieChart = chart

  const byType = summary.value?.byType ?? {}
  const pieData = Object.entries(byType).map(([name, value]) => ({
    name: toLabel(name),
    value,
  }))

  chart.setOption({
    color: CHART_PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 次 ({d}%)' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['38%', '65%'],
        center: ['50%', '42%'],
        data: pieData,
        label: { formatter: '{b}\n{d}%', fontSize: 12 },
        emphasis: { label: { fontSize: 16, fontWeight: 'bold' } },
      },
    ],
  })
}

async function renderBarChart() {
  const chart = await getOrInitChart(typeBarRef, typeBarChart)
  if (!chart) return
  typeBarChart = chart

  const byType = summary.value?.byType ?? {}
  const entries = Object.entries(byType)
    .map(([name, value]) => [toLabel(name), value])
    .sort((a, b) => b[1] - a[1])

  chart.setOption({
    color: [CHART_PALETTE[0]],
    tooltip: { trigger: 'axis', formatter: (params) => `${params[0].name}：<b>${params[0].value}</b> 次` },
    grid: { left: 80, right: 40, top: 10, bottom: 20 },
    xAxis: { type: 'value', name: '次数', minInterval: 1 },
    yAxis: { type: 'category', data: entries.map((e) => e[0]) },
    series: [
      {
        type: 'bar',
        data: entries.map((e) => e[1]),
        barMaxWidth: 28,
        itemStyle: { borderRadius: [0, 4, 4, 0] },
        label: { show: true, position: 'right' },
      },
    ],
  })
}

async function renderAllCharts() {
  await Promise.all([renderAlertCountChart(), renderPieChart(), renderBarChart()])
}

// ==================== Data loading ====================

async function fetchScore() {
  try {
    const data = await getPatientComprehensiveScoreApi()
    const level = data?.riskLevel
    compScore.value = {
      ...data,
      riskLevelLabel: level === 'HIGH' ? '高风险' : level === 'MEDIUM' ? '中风险' : '低风险',
    }
  } catch {
    compScore.value = {}
  }
}

async function load() {
  loading.value = true
  try {
    const [reportData] = await Promise.all([
      getPatientReportSummaryApi({ range: range.value }),
      fetchScore(),
    ])
    summary.value = reportData ?? {}
    latestPageNo.value = 1
    await renderAllCharts()
  } finally {
    loading.value = false
  }
}

// ==================== Filters ====================

function onFilterChange() {
  latestPageNo.value = 1
}

function resetFilters() {
  filters.indicatorType = ''
  filters.keyword = ''
  latestPageNo.value = 1
}

// ==================== CSV export ====================

function buildCsvLines() {
  const byType = summary.value?.byType ?? {}
  const riskTrend = summary.value?.riskTrend ?? []
  const now = new Date()

  const lines = [
    ['报表名称', '健康周报月报'],
    ['导出时间', now.toLocaleString()],
    ['统计范围', rangeLabel.value],
    ['上报总数', summary.value?.reportCount ?? 0],
    ['预警总数', summary.value?.alertCount ?? 0],
    [],
    ['各指标上报次数'],
    ['指标', '次数'],
    ...Object.entries(byType).map(([k, v]) => [csvEscape(k), csvEscape(v)]),
    [],
    ['每日预警情况'],
    ['日期', '预警次数', '平均风险分'],
    ...riskTrend.map((row) => [csvEscape(row.date), csvEscape(row.alertCount), csvEscape(row.avgRiskScore)]),
    [],
    ['最近上报记录'],
    ['指标', '数值', '上报时间', '备注'],
    ...filteredLatestData.value.map((row) => [
      csvEscape(row.indicatorType),
      csvEscape(row.value),
      csvEscape(row.reportTime),
      csvEscape(row.remark),
    ]),
  ]

  return lines.map((row) => (Array.isArray(row) ? row.join(',') : row))
}

function exportCsv() {
  downloadCsv(buildCsvLines(), `health_report_${range.value}_${Date.now()}.csv`)
  ElMessage.success('报表导出成功')
}

// ==================== Resize handler ====================

function handleResize() {
  alertCountChart?.resize()
  typePieChart?.resize()
  typeBarChart?.resize()
}

function disposeAllCharts() {
  alertCountChart?.dispose()
  alertCountChart = null
  typePieChart?.dispose()
  typePieChart = null
  typeBarChart?.dispose()
  typeBarChart = null
}

// ==================== Lifecycle ====================

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeAllCharts()
})
</script>

<style scoped>
/* ==================== Page layout ==================== */

.page-shell {
  margin: 12px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.page-title {
  margin: 0;
  font-size: 18px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

/* ==================== Hero score card ==================== */

.hero-score-card {
  margin-bottom: 14px;
  cursor: default;
  transition: box-shadow 0.2s;
  border-left: 4px solid #909399;
}

.hero-score-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.hero-score--danger {
  border-left-color: #f56c6c;
}
.hero-score--warn {
  border-left-color: #e6a23c;
}
.hero-score--safe {
  border-left-color: #67c23a;
}

.hero-score-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.hero-score-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-width: 140px;
}

.hero-score-label {
  font-size: 14px;
  color: #909399;
  font-weight: 500;
}

.hero-score-number {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.1;
  color: #303133;
}

.hero-score-tag {
  display: inline-block;
  padding: 2px 14px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
}

.hero-score-right {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.hero-indicator-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 60px;
}

.hero-indicator-name {
  font-size: 12px;
  color: #909399;
}

.hero-indicator-score {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
}

/* ==================== Stat cards ==================== */

.stat-row {
  margin-bottom: 14px;
}

.stat-card {
  margin-bottom: 10px;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.stat-icon {
  font-size: 28px;
  line-height: 1;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.stat-value--alert {
  color: #e6a23c;
}

/* ==================== Charts ==================== */

.section-card {
  margin-bottom: 12px;
}

.chart-main {
  width: 100%;
  height: 340px;
}

.chart-row {
  margin-bottom: 12px;
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

/* ==================== Filter & table ==================== */

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.pager-row {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}
</style>
