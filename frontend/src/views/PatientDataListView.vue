<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">历史上报数据</h3>
        <p class="page-subtitle">支持按指标和时间范围快速筛选</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" plain @click="goReport">快捷上报</el-button>
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button @click="resetFilter">重置筛选</el-button>
      </div>
    </div>

    <div class="soft-tip">
      当前筛选：{{ query.indicator_type || '全部指标' }} / {{ query.timeRange || '全部时间' }}
      <el-button link type="primary" @click="resetFilter">清空条件</el-button>
    </div>

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
    </div>

    <el-row :gutter="10" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never">当前列表数量：{{ list.length }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">总记录数：{{ total }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">筛选指标：{{ query.indicator_type || '全部' }}</el-card></el-col>
    </el-row>

    <el-card style="margin-bottom: 12px">
      <template #header>指标趋势图</template>
      <div v-if="list.length === 0" class="chart-empty">暂无可绘制数据，请先上报或调整筛选条件</div>
      <div v-else ref="trendRef" class="trend-chart"></div>
    </el-card>

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

  <el-dialog v-model="visible" title="编辑健康数据">
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
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { deleteHealthDataApi, listHealthDataApi, updateHealthDataApi } from '../api/modules'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const saving = ref(false)
const visible = ref(false)
const query = reactive({ indicator_type: '', timeRange: '', pageNo: 1, pageSize: 20 })
const form = reactive({ id: null, indicatorType: '', value: '', reportTime: '', remark: '' })
const trendRef = ref(null)
let trendChart = null
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const res = await listHealthDataApi(query)
    list.value = res?.list || []
    total.value = res?.total || 0
    await renderTrend()
  } catch (err) {
    ElMessage.error(err?.message || '加载健康数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
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

const parseNumericValue = (item) => {
  if (!item) return null
  const type = item.indicatorType
  if (type === '血压') {
    const arr = String(item.value || '').split('/')
    if (arr.length !== 2) return null
    const systolic = Number(arr[0])
    return Number.isFinite(systolic) ? systolic : null
  }
  const n = Number(item.value)
  return Number.isFinite(n) ? n : null
}

const resolveThresholds = () => {
  const selectedType = query.indicator_type
  const source = list.value
  const inferredType = source.length > 0 ? source[0].indicatorType : ''
  const type = selectedType || inferredType
  if (type === '血压') {
    return [
      { yAxis: 140, lineStyle: { color: '#e6a23c' }, label: { formatter: '偏高阈值 140' } },
      { yAxis: 180, lineStyle: { color: '#f56c6c' }, label: { formatter: '危急阈值 180' } }
    ]
  }
  if (type === '血糖') {
    return [
      { yAxis: 11.1, lineStyle: { color: '#e6a23c' }, label: { formatter: '偏高阈值 11.1' } },
      { yAxis: 16.7, lineStyle: { color: '#f56c6c' }, label: { formatter: '危急阈值 16.7' } }
    ]
  }
  if (type === '体重') {
    return [{ yAxis: 200, lineStyle: { color: '#e6a23c' }, label: { formatter: '关注阈值 200' } }]
  }
  return []
}

const renderTrend = async () => {
  await nextTick()
  if (list.value.length === 0) {
    if (trendChart) {
      trendChart.clear()
    }
    return
  }
  if (!trendRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendRef.value)
  }

  const sorted = [...list.value].sort((a, b) => String(a.reportTime).localeCompare(String(b.reportTime)))
  const xAxis = sorted.map((item) => item.reportTime)
  const yAxis = sorted.map((item) => parseNumericValue(item))
  const thresholds = resolveThresholds()

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      smooth: true,
      data: yAxis,
      connectNulls: false,
      markLine: thresholds.length > 0 ? { symbol: 'none', data: thresholds } : undefined
    }]
  })
}

const handlePageSizeChange = () => {
  query.pageNo = 1
  load()
}

const goReport = () => {
  router.push('/patient/report')
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const saveEdit = async () => {
  saving.value = true
  try {
    await updateHealthDataApi(form.id, form)
    ElMessage.success('更新成功')
    visible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const remove = async (id) => {
  await ElMessageBox.confirm('确认删除该条健康数据吗？', '删除确认', { type: 'warning' })
  await deleteHealthDataApi(id)
  ElMessage.success('删除成功')
  await load()
}

const handleResize = () => {
  if (trendChart) {
    trendChart.resize()
  }
}

onMounted(() => {
  load()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.actions {
  display: flex;
  gap: 8px;
}

.chart-empty {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #698188;
  font-size: 13px;
}

.filter-row {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.summary-row {
  margin-bottom: 12px;
}

.trend-chart {
  width: 100%;
  height: 320px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
