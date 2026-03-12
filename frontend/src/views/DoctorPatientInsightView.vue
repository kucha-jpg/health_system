<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>患者档案与趋势洞察</span>
        <div class="filters">
          <el-select v-model="query.indicatorType" clearable placeholder="指标类型" style="width: 130px" @change="load">
            <el-option label="血压" value="血压" />
            <el-option label="血糖" value="血糖" />
            <el-option label="体重" value="体重" />
            <el-option label="服药" value="服药" />
          </el-select>
          <el-select v-model="query.timeRange" placeholder="时间范围" style="width: 130px" @change="load">
            <el-option label="最近一天" value="day" />
            <el-option label="最近一周" value="week" />
            <el-option label="最近一月" value="month" />
          </el-select>
          <el-button @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="8"><el-card>患者：{{ insight.patient?.name || '-' }}</el-card></el-col>
      <el-col :span="8"><el-card>手机号：{{ insight.patient?.phone || '-' }}</el-card></el-col>
      <el-col :span="8"><el-card>未处理预警：{{ insight.openAlertCount || 0 }}</el-card></el-col>
    </el-row>

    <el-card style="margin-bottom: 12px">
      <template #header>患者档案</template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ insight.archive?.name || insight.patient?.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ insight.archive?.age ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="既往病史">{{ insight.archive?.medicalHistory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用药史">{{ insight.archive?.medicationHistory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="过敏史">{{ insight.archive?.allergyHistory || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-bottom: 12px">
      <template #header>连续健康数据趋势</template>
      <div ref="trendRef" class="trend-chart"></div>
    </el-card>

    <el-card style="margin-bottom: 12px">
      <template #header>健康数据明细</template>
      <el-table :data="insight.trendData || []" border>
        <el-table-column prop="indicatorType" label="指标" width="100" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>

    <el-card>
      <template #header>最近预警记录</template>
      <el-table :data="insight.recentAlerts || []" border>
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
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { getDoctorPatientInsightApi } from '../api/modules'

const route = useRoute()
const patientUserId = route.params.patientUserId
const insight = ref({})
const query = reactive({ indicatorType: '', timeRange: 'month' })
const trendRef = ref(null)
let trendChart = null

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
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: xAxis },
    yAxis: { type: 'value', name: query.indicatorType === '血压' ? '收缩压' : '数值' },
    series: [{ type: 'line', smooth: true, data: yAxis }]
  })
}

const load = async () => {
  insight.value = await getDoctorPatientInsightApi(patientUserId, query)
  await renderChart()
}

const handleResize = () => {
  if (trendChart) trendChart.resize()
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

.filters {
  display: flex;
  gap: 8px;
  align-items: center;
}

.trend-chart {
  width: 100%;
  height: 320px;
}
</style>
