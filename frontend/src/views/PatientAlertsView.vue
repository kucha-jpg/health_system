<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <div>
          <div class="title">我的预警详情</div>
          <div class="sub">查看风险变化与处理进度</div>
        </div>
        <div class="actions">
          <el-select v-model="status" style="width: 160px" @change="onStatusChanged">
            <el-option label="全部" value="" />
            <el-option label="未处理" value="OPEN" />
            <el-option label="已处理" value="CLOSED" />
          </el-select>
          <el-button :loading="loading" @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <el-table :data="alerts" border v-loading="loading" empty-text="暂无预警记录">
      <el-table-column prop="indicatorType" label="指标" width="100" />
      <el-table-column prop="value" label="数值" width="120" />
      <el-table-column prop="level" label="等级" width="100" />
      <el-table-column label="风险级别" width="110">
        <template #default="scope">
          <el-tag :type="riskType(scope.row.riskLevel)">{{ scope.row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="riskScore" label="风险分" width="90" />
      <el-table-column prop="reasonText" label="预警原因" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'OPEN' ? 'danger' : 'success'">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="触发时间" width="180" />
      <el-table-column prop="handleRemark" label="处理备注" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="pager">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="handlePageSizeChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPatientAlertsApi } from '../api/modules'

const alerts = ref([])
const status = ref('')
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
let timer = null

const load = async () => {
  loading.value = true
  try {
    const res = await getPatientAlertsApi({ status: status.value, pageNo: pageNo.value, pageSize: pageSize.value })
    alerts.value = res?.list || []
    total.value = res?.total || 0
  } catch (err) {
    ElMessage.error(err?.message || '加载预警失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const onStatusChanged = () => {
  pageNo.value = 1
  load()
}

const handlePageSizeChange = () => {
  pageNo.value = 1
  load()
}

const riskType = (riskLevel) => {
  if (riskLevel === 'HIGH') return 'danger'
  if (riskLevel === 'MEDIUM') return 'warning'
  return 'info'
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
  align-items: center;
  gap: 8px;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
