<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>健康周报/月报</span>
        <el-radio-group v-model="range" @change="load">
          <el-radio-button label="week">周报</el-radio-button>
          <el-radio-button label="month">月报</el-radio-button>
        </el-radio-group>
      </div>
    </template>

    <el-row :gutter="12" style="margin-bottom: 12px">
      <el-col :span="8"><el-card>上报总数：{{ summary.reportCount || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>预警总数：{{ summary.alertCount || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>统计范围：{{ summary.range || '-' }}</el-card></el-col>
    </el-row>

    <el-card style="margin-bottom: 12px">
      <template #header>指标分布</template>
      <div v-if="summary.byType">
        <el-tag v-for="(v, k) in summary.byType" :key="k" style="margin-right: 8px">{{ k }}: {{ v }}</el-tag>
      </div>
    </el-card>

    <el-card>
      <template #header>最近上报</template>
      <el-table :data="summary.latestData || []" border>
        <el-table-column prop="indicatorType" label="指标" width="100" />
        <el-table-column prop="value" label="数值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" width="180" />
        <el-table-column prop="remark" label="备注" />
      </el-table>
    </el-card>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getPatientReportSummaryApi } from '../api/modules'

const summary = ref({})
const range = ref('week')

const load = async () => {
  summary.value = await getPatientReportSummaryApi({ range: range.value })
}

onMounted(load)
</script>
