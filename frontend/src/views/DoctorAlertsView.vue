<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <div>
          <div class="title">医生预警工作台</div>
          <div class="sub">聚焦高风险患者，支持快捷筛选与分页查看</div>
        </div>
        <div class="toolbar-actions">
          <el-button :loading="loading" @click="loadData">刷新</el-button>
          <el-button type="danger" plain @click="applyHighRiskPreset">高风险快捷视图</el-button>
        </div>
      </div>
    </template>

    <el-row :gutter="10" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never">全部预警：{{ total }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">高风险：{{ riskSummary.HIGH }}</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never">中风险：{{ riskSummary.MEDIUM }}</el-card></el-col>
    </el-row>

    <div class="filters">
      <el-select v-model="query.riskLevel" clearable placeholder="风险级别" style="width: 140px" @change="onFilterChanged">
        <el-option label="全部" value="" />
        <el-option label="高风险" value="HIGH" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="低风险" value="LOW" />
      </el-select>
      <el-input-number v-model="query.minRiskScore" :min="0" :max="100" :step="5" @change="onFilterChanged" />
      <el-select v-model="query.sortBy" style="width: 170px" @change="onFilterChanged">
        <el-option label="按风险分优先" value="risk_desc" />
        <el-option label="按最新时间优先" value="time_desc" />
      </el-select>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-table :data="alerts" v-loading="loading" border empty-text="暂无匹配预警，试试调整筛选条件">
      <el-table-column prop="userId" label="患者ID" width="90" />
      <el-table-column prop="indicatorType" label="指标" width="90" />
      <el-table-column prop="value" label="数值" width="120" />
      <el-table-column prop="level" label="等级" width="90" />
      <el-table-column label="风险级别" width="110">
        <template #default="scope">
          <el-tag :type="riskTagType(scope.row.riskLevel)">{{ scope.row.riskLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="riskScore" label="风险分" width="90" />
      <el-table-column label="原因">
        <template #default="scope">
          {{ scope.row.reasonText || scope.row.reason }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handle(scope.row)">闭环处理</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <span class="sub">最近刷新：{{ lastUpdated || '-' }}</span>
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        layout="total, prev, pager, next, sizes"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="loadData"
        @size-change="handlePageSizeChange"
      />
    </div>
  </el-card>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { getDoctorAlertsApi, handleDoctorAlertApi } from '../api/modules'

const loading = ref(false)
const alerts = ref([])
const query = ref({
  riskLevel: '',
  minRiskScore: 0,
  sortBy: 'risk_desc',
  pageNo: 1,
  pageSize: 20
})
let timer = null
const total = ref(0)
const lastUpdated = ref('')

const riskSummary = computed(() => {
  const stat = { HIGH: 0, MEDIUM: 0 }
  alerts.value.forEach((item) => {
    if (item.riskLevel === 'HIGH') stat.HIGH += 1
    if (item.riskLevel === 'MEDIUM') stat.MEDIUM += 1
  })
  return stat
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDoctorAlertsApi(query.value)
    alerts.value = res?.list || []
    total.value = res?.total || 0
    lastUpdated.value = new Date().toLocaleString()
  } catch (err) {
    ElMessage.error(err?.message || '加载预警数据失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const onFilterChanged = () => {
  query.value.pageNo = 1
  loadData()
}

const resetFilters = () => {
  query.value = {
    riskLevel: '',
    minRiskScore: 0,
    sortBy: 'risk_desc',
    pageNo: 1,
    pageSize: 20
  }
  loadData()
}

const applyHighRiskPreset = () => {
  query.value = {
    riskLevel: 'HIGH',
    minRiskScore: 80,
    sortBy: 'risk_desc',
    pageNo: 1,
    pageSize: 20
  }
  loadData()
}

const handlePageSizeChange = () => {
  query.value.pageNo = 1
  loadData()
}

const riskTagType = (level) => {
  if (level === 'HIGH') return 'danger'
  if (level === 'MEDIUM') return 'warning'
  return 'info'
}

const handle = async (row) => {
  const { value } = await ElMessageBox.prompt('请输入处理意见', '预警闭环', { confirmButtonText: '确认', cancelButtonText: '取消' })
  await handleDoctorAlertApi(row.id, { handleRemark: value })
  ElMessage.success('处理成功')
  await loadData()
}

onMounted(() => {
  loadData()
  timer = window.setInterval(loadData, 10000)
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

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.title {
  font-size: 16px;
  font-weight: 600;
}

.sub {
  color: #909399;
  font-size: 12px;
}

.summary-row {
  margin-bottom: 10px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 0 12px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
</style>
