<template>
  <div>
    <el-row :gutter="12">
      <el-col :span="8"><el-card>系统用户总数：{{ overview.totalUsers || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>健康上报总数：{{ overview.totalHealthData || 0 }}</el-card></el-col>
      <el-col :span="8"><el-card>未处理预警：{{ overview.openAlerts || 0 }}</el-card></el-col>
    </el-row>

    <el-card style="margin-top: 16px">
      <template #header>最近上报数据</template>
      <el-table :data="overview.latestHealthData || []" border>
        <el-table-column prop="userId" label="患者ID" width="100" />
        <el-table-column prop="indicatorType" label="指标" width="120" />
        <el-table-column prop="value" label="值" width="120" />
        <el-table-column prop="reportTime" label="上报时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMonitorOverviewApi } from '../api/modules'

const overview = ref({})

onMounted(async () => {
  const res = await getMonitorOverviewApi()
  overview.value = res || {}
})
</script>
