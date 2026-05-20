<template>
  <el-card class="page-shell">
    <el-breadcrumb separator="/" class="page-breadcrumb">
      <el-breadcrumb-item :to="{ path: '/home' }">工作首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/doctor/groups' }">群组管理</el-breadcrumb-item>
      <el-breadcrumb-item>患者洞察</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="errorMessage" class="error-state">
      <div class="error-state-icon">!</div>
      <div class="error-state-title">{{ errorMessage }}</div>
      <div class="error-state-desc">该患者不在您的管辖范围内，或患者不存在</div>
      <el-button type="primary" @click="router.push('/doctor/groups')">返回群组管理</el-button>
    </div>

    <template v-else>
      <!-- Patient info row -->
      <div class="page-header">
        <h3 class="page-title">患者档案与趋势洞察</h3>
        <el-button :loading="loading" @click="load">刷新</el-button>
      </div>

      <el-row :gutter="10" class="info-row">
        <el-col :xs="12" :sm="6">
          <div class="info-card">👤 患者：<strong>{{ insight.patient?.name || '-' }}</strong></div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="info-card">📱 手机号：<strong>{{ insight.patient?.phone || '-' }}</strong></div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="info-card info-card--warn">⚠️ 未处理预警：<strong>{{ insight.openAlertCount || 0 }}</strong></div>
        </el-col>
        <el-col :xs="12" :sm="6">
          <div class="info-card">📊 趋势数据：<strong>{{ (insight.trendData || []).length }}</strong> 条</div>
        </el-col>
      </el-row>

      <PatientArchiveCard :archive="insight.archive" :patient-name="insight.patient?.name" />

      <!-- Filter toolbar -->
      <div class="insight-toolbar">
        <div class="toolbar-left">
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
        </div>
        <div class="toolbar-right">
          <span class="current-view">当前视图：{{ activeInsightText }}</span>
        </div>
      </div>

      <!-- Trend chart: full width -->
      <el-card class="section-card" shadow="never">
        <template #header>
          <span class="section-title">📈 连续健康数据趋势</span>
        </template>
        <div ref="trendRef" class="trend-chart"></div>
      </el-card>

      <!-- Pie + Bar side by side -->
      <el-row :gutter="12" class="chart-row">
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <span class="section-title">🥧 预警状态分布</span>
            </template>
            <div ref="alertPieRef" class="sub-chart"></div>
          </el-card>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <span class="section-title">📊 指标出现频次</span>
            </template>
            <div ref="indicatorBarRef" class="sub-chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Two tables side by side -->
      <el-row :gutter="12" class="tables-row">
        <!-- Left: Health data detail -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="table-header-row">
                <span class="section-title">📋 健康数据明细</span>
                <el-input
                  v-model="dataKeyword"
                  class="table-filter-input"
                  size="small"
                  clearable
                  placeholder="搜索备注..."
                  @input="onDataFilterChange"
                />
              </div>
            </template>
            <el-table :data="pagedTrendData" border size="small" empty-text="暂无匹配明细">
              <el-table-column prop="indicatorType" label="指标" width="80" />
              <el-table-column prop="value" label="数值" width="100" />
              <el-table-column prop="reportTime" label="上报时间" width="150" />
              <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            </el-table>
            <div class="mini-pager">
              <el-pagination
                v-model:current-page="dataPageNo"
                v-model:page-size="dataPageSize"
                :page-sizes="[5, 10, 20]"
                :total="filteredTrendData.length"
                layout="total, prev, pager, next"
                small
              />
            </div>
          </el-card>
        </el-col>

        <!-- Right: Recent alerts -->
        <el-col :xs="24" :lg="12">
          <el-card shadow="never">
            <template #header>
              <div class="table-header-row">
                <span class="section-title">🔔 最近预警记录</span>
                <el-select
                  v-model="alertStatusFilter"
                  class="table-filter-input"
                  size="small"
                  clearable
                  placeholder="预警状态"
                  @change="onAlertFilterChange"
                >
                  <el-option label="未处理" value="OPEN" />
                  <el-option label="已处理" value="CLOSED" />
                </el-select>
              </div>
            </template>
            <el-table :data="pagedAlerts" border size="small" empty-text="暂无匹配预警">
              <el-table-column prop="indicatorType" label="指标" width="80" />
              <el-table-column prop="value" label="数值" width="90" />
              <el-table-column label="等级" width="70">
                <template #default="scope">
                  <el-tag :type="scope.row.riskLevel === 'HIGH' ? 'danger' : scope.row.riskLevel === 'MEDIUM' ? 'warning' : 'info'" size="small">
                    {{ scope.row.riskLevel || scope.row.level }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="80">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 'OPEN' ? 'danger' : 'success'" size="small">
                    {{ scope.row.status === 'OPEN' ? '未处理' : '已处理' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" label="触发时间" width="150" />
            </el-table>
            <div class="mini-pager">
              <el-pagination
                v-model:current-page="alertPageNo"
                v-model:page-size="alertPageSize"
                :page-sizes="[5, 10, 20]"
                :total="filteredAlerts.length"
                layout="total, prev, pager, next"
                small
              />
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-button class="floating-top-btn" circle @click="scrollToTop">顶</el-button>
    </template>
  </el-card>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import echarts from '../utils/echarts'
import { getDoctorPatientInsightApi } from '../api/modules'
import { CHART_PALETTE, CHART_SPLIT_LINE, RISK_COLORS } from '../constants/chart-theme'
import PatientArchiveCard from './components/PatientArchiveCard.vue'

const route = useRoute()
const router = useRouter()
const insight = ref({})
const loading = ref(false)
const errorMessage = ref('')
const query = reactive({ indicatorType: '', timeRange: 'month' })
const trendChartType = ref('line')

// Independent table filters
const dataKeyword = ref('')
const alertStatusFilter = ref('')
const dataPageNo = ref(1)
const dataPageSize = ref(10)
const alertPageNo = ref(1)
const alertPageSize = ref(10)

const trendRef = ref(null)
const alertPieRef = ref(null)
const indicatorBarRef = ref(null)
let trendChart = null
let alertPieChart = null
let indicatorBarChart = null

const filteredTrendData = computed(() => {
  const keyword = dataKeyword.value.trim().toLowerCase()
  const source = insight.value?.trendData || []
  if (!keyword) return source
  return source.filter(item => String(item.remark || '').toLowerCase().includes(keyword))
})

const filteredAlerts = computed(() => {
  const source = insight.value?.recentAlerts || []
  if (!alertStatusFilter.value) return source
  return source.filter(item => item.status === alertStatusFilter.value)
})

const pagedTrendData = computed(() => {
  const start = (dataPageNo.value - 1) * dataPageSize.value
  return filteredTrendData.value.slice(start, start + dataPageSize.value)
})

const pagedAlerts = computed(() => {
  const start = (alertPageNo.value - 1) * alertPageSize.value
  return filteredAlerts.value.slice(start, start + alertPageSize.value)
})

const activeInsightText = computed(() => {
  const indicator = query.indicatorType || '全部指标'
  const time = query.timeRange || 'month'
  return `${indicator} / ${time === 'day' ? '最近一天' : time === 'week' ? '最近一周' : '最近一月'} / ${trendChartType.value}`
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
  if (!trendChart) trendChart = echarts.init(trendRef.value)
  const list = insight.value?.trendData || []
  const fmtDate = (d) => {
    if (!d || d.length < 10) return d
    return `${parseInt(d.slice(5, 7))}月${parseInt(d.slice(8, 10))}日`
  }
  const xAxis = list.map(item => fmtDate(item.reportTime))
  const yAxis = list.map(item => parseTrendValue(item))
  trendChart.clear()
  trendChart.setOption({
    color: [CHART_PALETTE[1]],
    tooltip: { trigger: 'axis' },
    toolbox: { feature: { saveAsImage: {} } },
    grid: { left: 70, right: 25, top: 28, bottom: 80 },
    xAxis: { type: 'category', data: xAxis, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', name: query.indicatorType === '血压' ? '收缩压(mmHg)' : '数值', nameTextStyle: { fontSize: 11 }, splitLine: CHART_SPLIT_LINE },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 12, height: 24 }],
    series: [{ type: trendChartType.value, smooth: trendChartType.value === 'line', data: yAxis }]
  })
}

const renderAlertPie = async () => {
  await nextTick()
  if (!alertPieRef.value) return
  if (!alertPieChart) alertPieChart = echarts.init(alertPieRef.value)
  const source = insight.value?.recentAlerts || []
  const open = source.filter(item => item.status === 'OPEN').length
  const closed = source.filter(item => item.status === 'CLOSED').length
  alertPieChart.clear()
  alertPieChart.setOption({
    color: [RISK_COLORS.OPEN, RISK_COLORS.CLOSED],
    tooltip: { trigger: 'item', formatter: '{b}: {c} 条 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['38%', '65%'],
      center: ['50%', '42%'],
      data: [
        { name: '未处理', value: open },
        { name: '已处理', value: closed }
      ],
      label: { formatter: '{b}\n{d}%' }
    }]
  })
}

const renderIndicatorBar = async () => {
  await nextTick()
  if (!indicatorBarRef.value) return
  if (!indicatorBarChart) indicatorBarChart = echarts.init(indicatorBarRef.value)
  const source = insight.value?.trendData || []
  const counts = {}
  source.forEach(item => {
    const key = item.indicatorType || '未知'
    counts[key] = (counts[key] || 0) + 1
  })
  const entries = Object.entries(counts).sort((a, b) => b[1] - a[1])
  const names = entries.map(e => e[0])
  const values = entries.map(e => e[1])
  indicatorBarChart.clear()
  indicatorBarChart.setOption({
    color: [CHART_PALETTE[0]],
    tooltip: { trigger: 'axis', formatter: (params) => `${params[0].name}：<b>${params[0].value}</b> 次` },
    grid: { left: 60, right: 20, top: 10, bottom: 20 },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value', name: '出现次数', splitLine: CHART_SPLIT_LINE, minInterval: 1 },
    series: [{
      type: 'bar', data: values, barMaxWidth: 36,
      itemStyle: { borderRadius: [4, 4, 0, 0] },
      label: { show: true, position: 'top' }
    }]
  })
}

const load = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    insight.value = await getDoctorPatientInsightApi(route.params.patientUserId, query)
    dataPageNo.value = 1
    alertPageNo.value = 1
    await renderChart()
    await renderAlertPie()
    await renderIndicatorBar()
  } catch (err) {
    insight.value = {}
    errorMessage.value = err?.response?.data?.msg || err?.message || '加载患者数据失败'
    if (trendChart) { trendChart.dispose(); trendChart = null }
    if (alertPieChart) { alertPieChart.dispose(); alertPieChart = null }
    if (indicatorBarChart) { indicatorBarChart.dispose(); indicatorBarChart = null }
  } finally {
    loading.value = false
  }
}

const onDataFilterChange = () => {
  dataPageNo.value = 1
}

const onAlertFilterChange = () => {
  alertPageNo.value = 1
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

watch(trendChartType, () => renderChart())

watch(filteredTrendData, () => {
  if ((dataPageNo.value - 1) * dataPageSize.value >= filteredTrendData.value.length) {
    dataPageNo.value = 1
  }
})

watch(filteredAlerts, () => {
  if ((alertPageNo.value - 1) * alertPageSize.value >= filteredAlerts.value.length) {
    alertPageNo.value = 1
  }
})

const handleResize = () => {
  trendChart?.resize()
  alertPieChart?.resize()
  indicatorBarChart?.resize()
}

watch(() => route.params.patientUserId, () => {
  if (route.params.patientUserId) load()
})

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose(); trendChart = null
  alertPieChart?.dispose(); alertPieChart = null
  indicatorBarChart?.dispose(); indicatorBarChart = null
})
</script>

<style scoped>
.page-breadcrumb {
  margin-bottom: 10px;
}

.info-row {
  margin-bottom: 12px;
}

.info-card {
  padding: 12px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.7);
  font-size: 13px;
  color: var(--ink-2);
}

.info-card strong {
  color: var(--ink-1);
  font-size: 15px;
}

.info-card--warn strong {
  color: #8a4b28;
}

.insight-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.current-view {
  font-size: 12px;
  color: var(--ink-2);
  white-space: nowrap;
}

.section-card {
  margin-bottom: 12px;
}

.section-title {
  font-weight: 600;
  font-size: 14px;
}

.chart-row {
  margin-bottom: 12px;
}

.tables-row {
  margin-bottom: 0;
}

.trend-chart {
  width: 100%;
  height: 450px;
}

.sub-chart {
  width: 100%;
  height: 280px;
}

.table-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.table-filter-input {
  width: 140px;
}

.mini-pager {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

.error-state {
  min-height: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  text-align: center;
}

.error-state-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(245, 108, 108, 0.12);
  color: #f56c6c;
  font-size: 24px;
  font-weight: 700;
  display: grid;
  place-items: center;
}

.error-state-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink-1);
}

.error-state-desc {
  font-size: 13px;
  color: var(--ink-2);
}

.floating-top-btn {
  position: fixed;
  right: 20px;
  bottom: 120px;
  z-index: 20;
  border: 1px solid rgba(64, 158, 255, 0.55);
  background: rgba(255, 255, 255, 0.72);
  color: #2f4952;
  backdrop-filter: blur(4px);
}

@media (max-width: 900px) {
  .insight-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: flex-start;
  }
}
</style>
