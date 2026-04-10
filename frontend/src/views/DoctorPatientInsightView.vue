<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">患者档案与趋势洞察</h3>
        <p class="page-subtitle">患者档案与风险趋势</p>
      </div>
      <div class="page-actions">
        <el-select v-model="query.indicatorType" class="w-130" clearable placeholder="指标类型" @change="load">
          <el-option label="血压" value="血压" />
          <el-option label="血糖" value="血糖" />
          <el-option label="体重" value="体重" />
          <el-option label="服药" value="服药" />
        </el-select>
        <el-select v-model="query.timeRange" class="w-130" placeholder="时间范围" @change="load">
          <el-option label="最近一天" value="day" />
          <el-option label="最近一周" value="week" />
          <el-option label="最近一月" value="month" />
        </el-select>
        <el-segmented v-model="trendChartType" :options="['line', 'bar', 'scatter']" />
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="filter-toolbar filter-toolbar-compact">
      <el-input v-model="filters.remarkKeyword" class="w-220" clearable placeholder="明细备注关键词" />
      <el-select v-model="filters.alertStatus" class="w-130" clearable placeholder="预警状态">
        <el-option label="未处理" value="OPEN" />
        <el-option label="已处理" value="CLOSED" />
      </el-select>
      <el-button @click="resetFilters">重置筛选</el-button>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">先看趋势，再看分布，最后在明细中确认风险来源</div>
        <div class="info-strip-desc">当前视图：{{ activeInsightText }}</div>
      </div>
      <el-tag effect="light">患者 {{ insight.patient?.name || '-' }}</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">患者</div>
        <div class="kpi-value">{{ insight.patient?.name || '-' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">手机号</div>
        <div class="kpi-value">{{ insight.patient?.phone || '-' }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">未处理预警</div>
        <div class="kpi-value">{{ insight.openAlertCount || 0 }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">趋势数据条数</div>
        <div class="kpi-value">{{ (insight.trendData || []).length }}</div>
      </div>
    </div>

    <el-card class="section-card" style="margin-bottom: 12px" shadow="never">
      <template #header>患者档案</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ insight.archive?.name || insight.patient?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ insight.archive?.age ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史">{{ insight.archive?.medicalHistory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用药史">{{ insight.archive?.medicationHistory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史">{{ insight.archive?.allergyHistory || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-row :gutter="12" class="chart-row">
      <el-col :xs="24" :lg="15">
        <el-card class="section-card" shadow="never">
          <template #header>连续健康数据趋势</template>
          <div ref="trendRef" class="trend-chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="9">
        <el-card class="section-card" shadow="never">
          <template #header>预警状态分布（饼图）</template>
          <div ref="alertPieRef" class="trend-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card" style="margin-bottom: 12px" shadow="never">
      <template #header>指标出现频次（柱状图）</template>
      <div ref="indicatorBarRef" class="trend-chart"></div>
    </el-card>

    <el-card class="section-card" style="margin-bottom: 12px" shadow="never">
      <template #header>健康数据明细</template>
      <el-table :data="filteredTrendData" border v-loading="loading" empty-text="暂无匹配明细">
        <el-table-column prop="indicatorType" label="指标" width="100" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-card class="section-card" shadow="never">
      <template #header>最近预警记录</template>
      <el-table :data="filteredAlerts" border v-loading="loading" empty-text="暂无匹配预警">
        <el-table-column prop="indicatorType" label="指标" width="90" />
        <el-table-column prop="value" label="数值" width="110" />
        <el-table-column prop="level" label="等级" width="90" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column prop="reasonText" label="原因" />
        <el-table-column prop="createTime" label="触发时间" width="180" />
      </el-table>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import echarts from '../utils/echarts'
import { getDoctorPatientInsightApi } from '../api/modules'
import { CHART_PALETTE, CHART_SPLIT_LINE, RISK_COLORS } from '../constants/chart-theme'
import { showFirstVisitGuide } from '../composables/useFirstVisitGuide'

const route = useRoute()
const patientUserId = route.params.patientUserId
const insight = ref({})
const loading = ref(false)
const query = reactive({ indicatorType: '', timeRange: 'month' })
const filters = reactive({ remarkKeyword: '', alertStatus: '' })
const trendChartType = ref('line')
const trendRef = ref(null)
const alertPieRef = ref(null)
const indicatorBarRef = ref(null)
let trendChart = null
let alertPieChart = null
let indicatorBarChart = null

const filteredTrendData = computed(() => {
  const keyword = (filters.remarkKeyword || '').trim().toLowerCase()
  const source = insight.value?.trendData || []
  if (!keyword) return source
  return source.filter((item) => String(item.remark || '').toLowerCase().includes(keyword))
})

const filteredAlerts = computed(() => {
  const source = insight.value?.recentAlerts || []
  if (!filters.alertStatus) return source
  return source.filter((item) => item.status === filters.alertStatus)
})

const activeInsightText = computed(() => {
  const indicator = query.indicatorType || '全部指标'
  const time = query.timeRange || 'month'
  const chartType = trendChartType.value
  return `${indicator} / ${time} / ${chartType}`
})

const parseTrendValue = (item) => {
  const type = item?.indicatorType
  const value = String(item?.value || '')
  if (type === '血压') {
    const parts = value.split('/')
    if (parts.length !== 2) return null
    const systolic = Number(parts[0])
    return Number.isFinite(systolic) ? systolic : null
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

const renderChart = async () => {
  await nextTick()
  if (!trendRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value)
  }
  const list = insight.value?.trendData || []
  const xAxis = list.map((item) => item.reportTime)
  const yAxis = list.map((item) => parseTrendValue(item))
  trendChart.setOption({
    color: [CHART_PALETTE[1]],
    tooltip: { trigger: 'axis' },
    toolbox: { feature: { saveAsImage: {}, dataZoom: { yAxisIndex: 'none' }, restore: {} } },
    dataZoom: [{ type: 'inside' }, { type: 'slider' }],
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value', name: query.indicatorType === '血压' ? '收缩压' : '数值', splitLine: CHART_SPLIT_LINE },
    series: [{ type: trendChartType.value, smooth: trendChartType.value === 'line', data: yAxis }]
  })
}

const renderAlertPie = async () => {
  await nextTick()
  if (!alertPieRef.value) return
  if (!alertPieChart) alertPieChart = echarts.init(alertPieRef.value)
  const source = insight.value?.recentAlerts || []
  const open = source.filter((item) => item.status === 'OPEN').length
  const closed = source.filter((item) => item.status === 'CLOSED').length
  alertPieChart.setOption({
    color: [RISK_COLORS.OPEN, RISK_COLORS.CLOSED],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['36%', '68%'],
      data: [
        { name: '未处理', value: open },
        { name: '已处理', value: closed }
      ]
    }]
  })
}

const renderIndicatorBar = async () => {
  await nextTick()
  if (!indicatorBarRef.value) return
  if (!indicatorBarChart) indicatorBarChart = echarts.init(indicatorBarRef.value)
  const source = insight.value?.trendData || []
  const counts = {}
  source.forEach((item) => {
    const key = item.indicatorType || '未知'
    counts[key] = (counts[key] || 0) + 1
  })
  const names = Object.keys(counts)
  const values = names.map((name) => counts[name])
  indicatorBarChart.setOption({
    color: [CHART_PALETTE[0]],
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value', name: '频次', splitLine: CHART_SPLIT_LINE },
    series: [{ type: 'bar', data: values, barMaxWidth: 34 }]
  })
}

const load = async () => {
  loading.value = true
  try {
    insight.value = await getDoctorPatientInsightApi(patientUserId, query)
    await renderChart()
    await renderAlertPie()
    await renderIndicatorBar()
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.remarkKeyword = ''
  filters.alertStatus = ''
}

watch(trendChartType, () => {
  renderChart()
})

const handleResize = () => {
  if (trendChart) trendChart.resize()
  if (alertPieChart) alertPieChart.resize()
  if (indicatorBarChart) indicatorBarChart.resize()
}

onMounted(() => {
  load()
  showFirstVisitGuide({
    storageKey: `guide_doctor_patient_insight_v1_${patientUserId}`,
    title: '患者洞察引导',
    message: '建议先按时间范围查看趋势，再结合预警状态分布与指标频次判断风险来源，最后在明细表过滤备注进行复核。'
  }).catch(() => {})
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  if (alertPieChart) {
    alertPieChart.dispose()
    alertPieChart = null
  }
  if (indicatorBarChart) {
    indicatorBarChart.dispose()
    indicatorBarChart = null
  }
})
</script>

<style scoped>
.chart-row {
  margin-bottom: 12px;
}

.trend-chart {
  width: 100%;
  height: 300px;
}
</style>
