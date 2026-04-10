<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">医生预警工作台</h3>
        <p class="page-subtitle">高风险患者优先处理</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="danger" plain @click="applyHighRiskPreset">高风险快捷视图</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">优先处理高风险与待闭环预警</div>
        <div class="info-strip-desc">筛选：{{ activeFilterText }}</div>
      </div>
      <el-tag effect="light">最近刷新：{{ lastUpdated || '-' }}</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">全部预警</div>
        <div class="kpi-value">{{ total }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">高风险</div>
        <div class="kpi-value">{{ riskSummary.HIGH }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">中风险</div>
        <div class="kpi-value">{{ riskSummary.MEDIUM }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">待闭环</div>
        <div class="kpi-value">{{ openCount }}</div>
      </div>
    </div>

    <div class="filter-toolbar filters">
      <el-select v-model="query.riskLevel" class="w-140" clearable placeholder="风险级别" @change="onFilterChanged">
        <el-option label="全部" value="" />
        <el-option label="高风险" value="HIGH" />
        <el-option label="中风险" value="MEDIUM" />
        <el-option label="低风险" value="LOW" />
      </el-select>
      <el-input-number v-model="query.minRiskScore" :min="0" :max="100" :step="5" @change="onFilterChanged" />
      <el-select v-model="query.sortBy" class="w-170" @change="onFilterChanged">
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
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="scope">
          <div class="action-group">
            <el-button size="small" class="btn-handle" type="primary" @click="handle(scope.row)">闭环处理</el-button>
            <el-button size="small" class="btn-quick" type="success" plain @click="quickHandle(scope.row)">一键闭环</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-row">
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
import { showFirstVisitGuide } from '../composables/useFirstVisitGuide'

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

const openCount = computed(() => alerts.value.filter((item) => item.status === 'OPEN').length)

const activeFilterText = computed(() => {
  const level = query.value.riskLevel || '全部'
  const score = query.value.minRiskScore
  const sortText = query.value.sortBy === 'risk_desc' ? '风险优先' : '时间优先'
  return `${level} / 最低分 ${score} / ${sortText}`
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

const quickHandle = async (row) => {
  await ElMessageBox.confirm('将使用默认处理意见“已电话随访，建议持续监测”，确认继续？', '一键闭环', {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await handleDoctorAlertApi(row.id, { handleRemark: '已电话随访，建议持续监测' })
  ElMessage.success('已快速完成闭环处理')
  await loadData()
}

onMounted(() => {
  loadData()
  showFirstVisitGuide({
    storageKey: 'guide_doctor_alerts_v1',
    title: '预警工作台引导',
    message: '可先使用“高风险快捷视图”进行分诊，再按风险分和时间排序处理；闭环意见将进入审计链路用于追踪。'
  }).catch(() => {})
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

.action-group {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-handle,
.btn-quick {
  min-width: 76px;
}

.btn-quick {
  border-color: rgba(37, 138, 102, 0.38) !important;
  color: #1e7458 !important;
  background: rgba(255, 255, 255, 0.86) !important;
}

.btn-quick:hover {
  border-color: rgba(37, 138, 102, 0.62) !important;
  color: #145842 !important;
  background: rgba(255, 255, 255, 0.96) !important;
}

.filters {
  margin: 8px 0 12px;
}
</style>
