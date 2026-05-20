<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">历史上报数据</h3>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button @click="resetFilter">重置筛选</el-button>
      </div>
    </div>

    <div class="soft-tip">
      以下展示您所有指标的历史趋势，可切换查看各指标变化情况。
    </div>

    <!-- 5 charts grid -->
    <div v-if="allData.length === 0 && !loading" class="chart-empty-banner">
      <div class="empty-illustration"></div>
      <div>暂无健康数据，请先上报</div>
    </div>

    <div v-else class="charts-grid">
      <!-- Chart 1: Blood Pressure -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon">💓</span>
            <span>血压趋势</span>
            <span class="chart-unit">(mmHg)</span>
          </div>
        </template>
        <div v-if="bpData.length === 0" class="chart-empty">暂无血压数据</div>
        <div v-else ref="bpRef" class="chart-box"></div>
      </el-card>

      <!-- Chart 2: Blood Sugar -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon">🩸</span>
            <span>血糖趋势</span>
            <span class="chart-unit">(mmol/L)</span>
          </div>
        </template>
        <div v-if="sugarData.length === 0" class="chart-empty">暂无血糖数据</div>
        <div v-else ref="sugarRef" class="chart-box"></div>
      </el-card>

      <!-- Chart 3: Weight -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon">⚖️</span>
            <span>体重趋势</span>
            <span class="chart-unit">(kg)</span>
          </div>
        </template>
        <div v-if="weightData.length === 0" class="chart-empty">暂无体重数据</div>
        <div v-else ref="weightRef" class="chart-box"></div>
      </el-card>

      <!-- Chart 4: Medication -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="chart-header">
            <span class="chart-icon">💊</span>
            <span>服药记录</span>
          </div>
        </template>
        <div v-if="medData.length === 0" class="chart-empty">暂无服药记录</div>
        <div v-else ref="medRef" class="chart-box"></div>
      </el-card>

    </div>

    <!-- Table filters and data table (unchanged logic) -->
    <div class="filter-row">
      <el-select v-model="query.indicator_type" placeholder="指标类型" clearable style="width: 140px" @change="onFilterChanged">
        <el-option label="血压" value="血压" />
        <el-option label="血糖" value="血糖" />
        <el-option label="体重" value="体重" />
        <el-option label="服药" value="服药" />
      </el-select>
      <el-select v-model="query.timeRange" placeholder="时间范围" clearable style="width: 140px" @change="onFilterChanged">
        <el-option label="最近一天" value="day" />
        <el-option label="最近一周" value="week" />
        <el-option label="最近一月" value="month" />
      </el-select>
      <span class="table-hint">表格筛选：{{ query.indicator_type || '全部指标' }} / {{ query.timeRange || '全部时间' }}</span>
      <el-button link type="primary" @click="resetFilter">清空条件</el-button>
    </div>

    <el-row :gutter="10" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never">当前列表数量：{{ list.length }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">总记录数：{{ total }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">筛选指标：{{ query.indicator_type || '全部' }}</el-card></el-col>
    </el-row>

    <el-table :data="list" border v-loading="loading" empty-text="暂无健康数据记录">
      <el-table-column prop="indicatorType" label="指标" width="100" />
      <el-table-column prop="value" label="数值" width="140" />
      <el-table-column prop="reportTime" label="上报时间" width="180" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="handlePageSizeChange"
      />
    </div>
  </el-card>

  <el-dialog
    v-model="visible"
    title="编辑健康数据"
    center
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <el-form :model="form" label-width="100px">
      <el-form-item label="指标"><el-input v-model="form.indicatorType" disabled /></el-form-item>
      <el-form-item label="数值"><el-input v-model="form.value" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteHealthDataApi, listHealthDataApi, updateHealthDataApi } from '../api/modules'

const list = ref([])
const allData = ref([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const query = reactive({ indicator_type: '', timeRange: '', pageNo: 1, pageSize: 20 })
const form = reactive({ id: null, indicatorType: '', value: '', reportTime: '', remark: '' })
const total = ref(0)

// Chart refs
const bpRef = ref(null)
const sugarRef = ref(null)
const weightRef = ref(null)
const medRef = ref(null)
let bpChart = null, sugarChart = null, weightChart = null, medChart = null

// Computed data subsets
const bpData = computed(() => allData.value.filter(d => d.indicatorType === '血压'))
const sugarData = computed(() => allData.value.filter(d => d.indicatorType === '血糖'))
const weightData = computed(() => allData.value.filter(d => d.indicatorType === '体重'))
const medData = computed(() => allData.value.filter(d => d.indicatorType === '服药'))

const fmtDate = (d) => {
  if (!d || d.length < 10) return d
  return `${parseInt(d.slice(5, 7))}月${parseInt(d.slice(8, 10))}日`
}

const sortByTime = (arr) => [...arr].sort((a, b) => String(a.reportTime).localeCompare(String(b.reportTime)))

const parseBp = (item) => {
  const parts = String(item.value || '').split('/')
  if (parts.length !== 2) return null
  const s = Number(parts[0])
  const d = Number(parts[1])
  return Number.isFinite(s) && Number.isFinite(d) ? { systolic: s, diastolic: d } : null
}

const parseNum = (item) => {
  const n = Number(item.value)
  return Number.isFinite(n) ? n : null
}

// Load ALL data for charts
const loadAllData = async () => {
  try {
    const res = await listHealthDataApi({ pageNo: 1, pageSize: 2000 })
    allData.value = res?.list || []
  } catch (e) {
    allData.value = []
  }
}

// Load filtered data for table
const load = async () => {
  loading.value = true
  try {
    const res = await listHealthDataApi(query)
    list.value = res?.list || []
    total.value = res?.total || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载健康数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const loadAll = async () => {
  await Promise.all([loadAllData(), load()])
  await nextTick()
  renderAllCharts()
}

const onFilterChanged = () => {
  query.pageNo = 1
  load()
}

const resetFilter = () => {
  query.indicator_type = ''
  query.timeRange = ''
  query.pageNo = 1
  load()
}

const renderAllCharts = () => {
  renderBpChart()
  renderSugarChart()
  renderWeightChart()
  renderMedChart()
}

// ---- Blood Pressure Chart ----
const renderBpChart = async () => {
  await nextTick()
  if (!bpRef.value) return
  if (!bpChart) bpChart = echarts.init(bpRef.value)
  const sorted = sortByTime(bpData.value)
  const xData = sorted.map(d => fmtDate(d.reportTime))
  const systolic = sorted.map(d => parseBp(d)?.systolic ?? null)
  const diastolic = sorted.map(d => parseBp(d)?.diastolic ?? null)

  bpChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['收缩压(高压)', '舒张压(低压)'], bottom: 0 },
    grid: { left: 55, right: 50, top: 28, bottom: 60 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', name: 'mmHg', nameTextStyle: { fontSize: 11 }, min: 0 },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 8, height: 22 }],
    series: [
      {
        name: '收缩压(高压)', type: 'line', smooth: true, data: systolic,
        lineStyle: { width: 2, color: '#e74c3c' },
        itemStyle: { color: '#e74c3c' },
        markLine: {
          silent: true, symbol: 'none',
          label: { fontSize: 10 },
          data: [
            { yAxis: 140, lineStyle: { color: '#e6a23c', type: 'dashed' }, label: { formatter: '偏高140' } },
            { yAxis: 180, lineStyle: { color: '#f56c6c', type: 'dashed' }, label: { formatter: '危险180' } }
          ]
        }
      },
      {
        name: '舒张压(低压)', type: 'line', smooth: true, data: diastolic,
        lineStyle: { width: 2, color: '#3498db' },
        itemStyle: { color: '#3498db' },
        markLine: {
          silent: true, symbol: 'none',
          label: { fontSize: 10 },
          data: [
            { yAxis: 90, lineStyle: { color: '#e6a23c', type: 'dashed' }, label: { formatter: '偏高90' } },
            { yAxis: 110, lineStyle: { color: '#f56c6c', type: 'dashed' }, label: { formatter: '危险110' } }
          ]
        }
      }
    ]
  })
}

// ---- Blood Sugar Chart ----
const renderSugarChart = async () => {
  await nextTick()
  if (!sugarRef.value) return
  if (!sugarChart) sugarChart = echarts.init(sugarRef.value)
  const sorted = sortByTime(sugarData.value)
  const xData = sorted.map(d => fmtDate(d.reportTime))
  const values = sorted.map(d => parseNum(d))

  sugarChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 55, right: 65, top: 28, bottom: 60 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', name: 'mmol/L', nameTextStyle: { fontSize: 11 }, min: 0 },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 8, height: 22 }],
    series: [{
      name: '血糖值', type: 'line', smooth: true, data: values,
      lineStyle: { width: 2, color: '#e67e22' },
      itemStyle: { color: '#e67e22' },
      areaStyle: { color: 'rgba(230, 126, 34, 0.08)' },
      markLine: {
        silent: true, symbol: 'none',
        label: { fontSize: 10 },
        data: [
          { yAxis: 6.1, lineStyle: { color: '#e6a23c', type: 'dashed' }, label: { formatter: '正常上限6.1' } },
          { yAxis: 11.1, lineStyle: { color: '#f56c6c', type: 'dashed' }, label: { formatter: '危险11.1' } }
        ]
      },
      markArea: {
        silent: true,
        data: [
          [{ yAxis: 0, itemStyle: { color: 'rgba(46, 204, 113, 0.06)' } }, { yAxis: 6.1 }],
          [{ yAxis: 6.1, itemStyle: { color: 'rgba(230, 126, 34, 0.08)' } }, { yAxis: 11.1 }],
          [{ yAxis: 11.1, itemStyle: { color: 'rgba(245, 108, 108, 0.1)' } }, { yAxis: 30 }]
        ]
      }
    }]
  })
}

// ---- Weight Chart ----
const renderWeightChart = async () => {
  await nextTick()
  if (!weightRef.value) return
  if (!weightChart) weightChart = echarts.init(weightRef.value)
  const sorted = sortByTime(weightData.value)
  const xData = sorted.map(d => fmtDate(d.reportTime))
  const values = sorted.map(d => parseNum(d))

  weightChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 20, top: 28, bottom: 60 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', name: 'kg', nameTextStyle: { fontSize: 11 }, min: 0 },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 8, height: 22 }],
    series: [{
      name: '体重', type: 'line', smooth: true, data: values,
      lineStyle: { width: 2, color: '#2ecc71' },
      itemStyle: { color: '#2ecc71' },
      areaStyle: { color: 'rgba(46, 204, 113, 0.1)' }
    }]
  })
}

// ---- Medication Chart ----
const renderMedChart = async () => {
  await nextTick()
  if (!medRef.value) return
  if (!medChart) medChart = echarts.init(medRef.value)
  const sorted = sortByTime(medData.value)
  const xData = sorted.map(d => fmtDate(d.reportTime))

  medChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const v = params.value
        return `${params.name}<br/>${v === 1 ? '✅ 已服药' : '❌ 未服药'}`
      }
    },
    grid: { left: 40, right: 20, top: 25, bottom: 60 },
    xAxis: { type: 'category', data: xData, axisLabel: { rotate: 0, fontSize: 11 } },
    yAxis: { type: 'value', min: -0.5, max: 1.5, show: false },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 8, height: 22 }],
    series: [
      {
        name: '已服药',
        type: 'scatter',
        symbol: 'circle',
        symbolSize: 16,
        itemStyle: { color: '#2ecc71', borderColor: '#27ae60', borderWidth: 1 },
        data: sorted.map((d, i) => {
          const v = String(d.value || '')
          return (v === '已服药' || v === '1') ? [xData[i], 0.8] : null
        }).filter(Boolean)
      },
      {
        name: '未服药',
        type: 'scatter',
        symbol: 'circle',
        symbolSize: 16,
        itemStyle: { color: '#e74c3c', borderColor: '#c0392b', borderWidth: 1, opacity: 0.7 },
        data: sorted.map((d, i) => {
          const v = String(d.value || '')
          return (v !== '已服药' && v !== '1') ? [xData[i], 0.4] : null
        }).filter(Boolean)
      }
    ]
  })
}

const handlePageSizeChange = () => {
  query.pageNo = 1
  load()
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const saveEdit = async () => {
  await ElMessageBox.confirm('确认修改该条健康数据？', '修改确认', {
    type: 'warning',
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    closeOnClickModal: false,
    closeOnPressEscape: false,
    showClose: false
  })
  saving.value = true
  try {
    await updateHealthDataApi(form.id, form)
    ElMessage.success('更新成功')
    visible.value = false
    await loadAll()
  } finally {
    saving.value = false
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该条健康数据吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch {
    return
  }
  try {
    await deleteHealthDataApi(id)
    ElMessage.success('删除成功')
    await loadAll()
  } catch (err) {
    ElMessage.error(err?.message || '删除失败')
  }
}

const handleResize = () => {
  bpChart?.resize()
  sugarChart?.resize()
  weightChart?.resize()
  medChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  bpChart?.dispose(); bpChart = null
  sugarChart?.dispose(); sugarChart = null
  weightChart?.dispose(); weightChart = null
  medChart?.dispose(); medChart = null
})
</script>

<style scoped>
.charts-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
}

.chart-icon {
  font-size: 16px;
}

.chart-unit {
  font-size: 12px;
  color: var(--ink-2);
  font-weight: 400;
}

.chart-desc {
  font-size: 12px;
  color: var(--ink-2);
  font-weight: 400;
  margin-left: 8px;
}

.chart-box {
  width: 100%;
  height: 400px;
  min-height: 400px;
}

.chart-empty {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-2);
  font-size: 13px;
}

.chart-card {
  margin-bottom: 0;
}

.chart-empty-banner {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--ink-2);
  font-size: 14px;
}

.filter-row {
  margin-bottom: 10px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.table-hint {
  font-size: 12px;
  color: var(--ink-2);
}

.summary-row {
  margin-bottom: 12px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
