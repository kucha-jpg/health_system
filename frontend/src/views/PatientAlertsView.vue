<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span>我的预警详情</span>
        <el-select v-model="status" style="width: 160px" @change="load">
          <el-option label="全部" value="" />
          <el-option label="未处理" value="OPEN" />
          <el-option label="已处理" value="CLOSED" />
        </el-select>
      </div>
    </template>
    <el-table :data="alerts" border>
      <el-table-column prop="indicatorType" label="指标" width="100" />
      <el-table-column prop="value" label="数值" width="120" />
      <el-table-column prop="level" label="等级" width="100" />
      <el-table-column prop="reasonText" label="预警原因" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="createTime" label="触发时间" width="180" />
      <el-table-column prop="handleRemark" label="处理备注" />
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { getPatientAlertsApi } from '../api/modules'

const alerts = ref([])
const status = ref('')
let timer = null

const load = async () => {
  alerts.value = await getPatientAlertsApi({ status: status.value })
}

onMounted(() => {
  load()
  timer = window.setInterval(load, 10000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>
