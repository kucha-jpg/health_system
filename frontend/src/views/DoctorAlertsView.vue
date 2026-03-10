<template>
  <el-card>
    <template #header>医生预警工作台</template>
    <el-table :data="alerts" v-loading="loading" border>
      <el-table-column prop="userId" label="患者ID" width="90" />
      <el-table-column prop="indicatorType" label="指标" width="90" />
      <el-table-column prop="value" label="数值" width="120" />
      <el-table-column prop="level" label="等级" width="90" />
      <el-table-column label="原因">
        <template #default="scope">
          {{ scope.row.reasonText || scope.row.reason }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handle(scope.row)">闭环处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getDoctorAlertsApi, handleDoctorAlertApi } from '../api/modules'

const loading = ref(false)
const alerts = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDoctorAlertsApi()
    alerts.value = res || []
  } finally {
    loading.value = false
  }
}

const handle = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入处理意见', '预警闭环', { confirmButtonText: '确认', cancelButtonText: '取消' })
  await handleDoctorAlertApi(row.id, { handleRemark: value })
  ElMessage.success('处理成功')
  await loadData()
}

onMounted(loadData)
</script>
