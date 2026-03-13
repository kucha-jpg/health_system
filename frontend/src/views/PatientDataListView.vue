<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>历史上报数据</span>
        <div>
          <el-select v-model="query.indicator_type" placeholder="指标类型" clearable style="width: 140px; margin-right: 8px">
            <el-option label="血压" value="血压" />
            <el-option label="血糖" value="血糖" />
            <el-option label="体重" value="体重" />
            <el-option label="服药" value="服药" />
          </el-select>
          <el-select v-model="query.timeRange" placeholder="时间范围" clearable style="width: 140px; margin-right: 8px">
            <el-option label="最近一天" value="day" />
            <el-option label="最近一周" value="week" />
            <el-option label="最近一月" value="month" />
          </el-select>
          <el-button @click="load">筛选</el-button>
        </div>
      </div>
    </template>

    <el-card style="margin-bottom: 12px">
      <template #header>指标趋势图</template>
      <div ref="trendRef" class="trend-chart"></div>
    </el-card>

    <el-table :data="list" border>
      <el-table-column prop="indicatorType" label="指标" width="100" />
      <el-table-column prop="value" label="数值" width="140" />
      <el-table-column prop="reportTime" label="上报时间" width="180" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="primary" @click="openEdit(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="remove(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="visible" title="编辑健康数据">
    <el-form :model="form" label-width="100px">
      <el-form-item label="指标"><el-input v-model="form.indicatorType" disabled /></el-form-item>
      <el-form-item label="数值"><el-input v-model="form.value" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible=false">取消</el-button>
      <el-button type="primary" @click="saveEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { deleteHealthDataApi, listHealthDataApi, updateHealthDataApi } from '../api/modules'

const list = ref([])
const visible = ref(false)
const query = reactive({ indicator_type: '', timeRange: '' })
const form = reactive({ id: null, indicatorType: '', value: '', reportTime: '', remark: '' })
const trendRef = ref(null)
let trendChart = null

const load = async () => {
  list.value = await listHealthDataApi(query)
  await renderTrend()
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

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const saveEdit = async () => {
  await updateHealthDataApi(form.id, form)
  ElMessage.success('更新成功')
  visible.value = false
  await load()
}

const remove = async (id) => {
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
.trend-chart {
  width: 100%;
  height: 320px;
}
</style>
