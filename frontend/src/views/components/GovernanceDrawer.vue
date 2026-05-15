<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="520px"
    :close-on-click-modal="false"
  >
    <template #header>
      <div class="drawer-header">
        <h3 class="drawer-title">{{ group?.groupName || '-' }}</h3>
        <el-tag :type="statusTagType(group?.governanceStatus)" size="small" effect="dark">
          {{ statusText(group?.governanceStatus) }}
        </el-tag>
      </div>
    </template>

    <template v-if="group">
      <div class="info-grid">
        <div class="info-item">
          <span class="info-label">负责医生</span>
          <span class="info-value">{{ group.doctorName }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">患者数量</span>
          <span class="info-value">{{ group.patientCount }} 人</span>
        </div>
        <div class="info-item">
          <span class="info-label">目标科室</span>
          <span class="info-value">{{ group.targetDept || '未指定' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">最近操作</span>
          <span class="info-value">{{ group.lastActionTime || '暂无' }}</span>
        </div>
      </div>

      <div v-if="group.reviewRemark" class="remark-card">
        <div class="remark-label">审核备注</div>
        <div class="remark-text">{{ group.reviewRemark }}</div>
      </div>

      <div class="action-area">
        <h4 class="section-title">审核操作</h4>
        <div class="action-hint">
          <template v-if="group.governanceStatus === 'PENDING_REVIEW'">
            医生已提交此群组，审核通过后方可向群组添加患者。
          </template>
          <template v-else-if="group.governanceStatus === 'CROSS_DEPT'">
            已指派给 <b>{{ group.targetDept || '其他科室' }}</b>，审核通过后恢复运行。
          </template>
          <template v-else-if="group.governanceStatus === 'ACTIVE'">
            运行中，医生可正常管理患者。不再需要时可归档。
          </template>
          <template v-else-if="group.governanceStatus === 'REJECTED'">
            已驳回，医生修改后可重新提交审核。
          </template>
        </div>

        <div class="btn-group">
          <div v-if="group.governanceStatus === 'PENDING_REVIEW' || group.governanceStatus === 'CROSS_DEPT'" class="btn-card btn-card--approve">
            <div class="btn-card-body">
              <div class="btn-card-title">审核通过</div>
              <div class="btn-card-desc">群组设为运行中，医生可开始管理患者</div>
              <el-input v-model="approveRemark" type="textarea" :rows="2" placeholder="审核意见（选填）" maxlength="500" show-word-limit />
            </div>
            <el-button type="success" :loading="loading" @click="doApprove">确认</el-button>
          </div>

          <div v-if="group.governanceStatus === 'PENDING_REVIEW'" class="btn-card btn-card--reject">
            <div class="btn-card-body">
              <div class="btn-card-title">驳回</div>
              <div class="btn-card-desc">退回医生修改，需填写驳回原因</div>
              <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="驳回原因（必填，医生将看到此内容）" maxlength="500" show-word-limit />
            </div>
            <el-button type="danger" :loading="loading" :disabled="!rejectReason.trim()" @click="doReject">驳回</el-button>
          </div>

          <div v-if="group.governanceStatus !== 'ARCHIVED'" class="btn-card btn-card--cross">
            <div class="btn-card-body">
              <div class="btn-card-title">跨科室协调</div>
              <div class="btn-card-desc">指派给其他科室处理</div>
              <el-input v-model="crossDeptInput" placeholder="目标科室名称" maxlength="64" />
            </div>
            <el-button type="warning" :disabled="!crossDeptInput.trim()" @click="doCrossDept">确认</el-button>
          </div>

          <div v-if="group.governanceStatus !== 'ARCHIVED'" class="btn-card btn-card--archive">
            <div class="btn-card-body">
              <div class="btn-card-title">归档</div>
              <div class="btn-card-desc">标记为已结束，不可恢复</div>
            </div>
            <el-button type="danger" plain @click="doArchive">归档</el-button>
          </div>
        </div>
      </div>
    </template>

    <el-empty v-else description="未选择群组" />
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  group: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'approve', 'archive', 'reject', 'cross-dept'])

const visible = computed({
  get: () => props.modelValue,
  set: (v) => {
    emit('update:modelValue', v)
    if (!v) document.body.style.overflow = ''
  }
})

const approveRemark = ref('')
const rejectReason = ref('')
const crossDeptInput = ref('')

watch(() => props.group, () => {
  approveRemark.value = ''
  rejectReason.value = ''
  crossDeptInput.value = ''
})

const doApprove = () => emit('approve', { group: props.group, remark: approveRemark.value.trim() || undefined })
const doReject = () => {
  if (!rejectReason.value.trim()) { ElMessage.warning('请填写驳回原因'); return }
  emit('reject', { group: props.group, reason: rejectReason.value.trim() })
}
const doCrossDept = () => {
  if (!crossDeptInput.value.trim()) { ElMessage.warning('请输入目标科室'); return }
  emit('cross-dept', { group: props.group, targetDept: crossDeptInput.value.trim() })
  crossDeptInput.value = ''
}
const doArchive = () => {
  ElMessageBox.confirm('归档后群组变为只读，确认？', '归档确认', {
    type: 'warning', confirmButtonText: '确认归档', cancelButtonText: '取消',
    closeOnClickModal: false, closeOnPressEscape: false, showClose: false
  }).then(() => emit('archive', props.group)).catch(() => {})
}

const statusText = (s) => {
  const map = { PENDING_REVIEW: '待审核', ACTIVE: '运行中', REJECTED: '已驳回', CROSS_DEPT: '跨科室中', ARCHIVED: '已归档' }
  return map[s] || '未知'
}
const statusTagType = (s) => {
  if (s === 'PENDING_REVIEW') return 'warning'
  if (s === 'REJECTED' || s === 'CROSS_DEPT') return 'danger'
  if (s === 'ARCHIVED') return 'info'
  return 'success'
}
</script>

<style scoped>
.drawer-header { margin-bottom: 4px; }
.drawer-title { margin: 0 0 6px; font-size: 18px; color: #1f424c; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin: 16px 0; }
.info-item { display: flex; flex-direction: column; gap: 4px; }
.info-label { font-size: 12px; color: #909399; }
.info-value { font-size: 14px; color: #303133; font-weight: 500; }
.remark-card { margin: 8px 0 16px; padding: 12px 16px; border-radius: 10px; background: rgba(245, 108, 108, 0.06); border: 1px solid rgba(245, 108, 108, 0.18); }
.remark-label { font-size: 12px; color: #f56c6c; margin-bottom: 4px; font-weight: 600; }
.remark-text { font-size: 14px; color: #303133; line-height: 1.6; }
.action-area { margin-top: 8px; }
.section-title { margin: 0 0 8px; font-size: 15px; color: #1f424c; }
.action-hint { margin-bottom: 16px; font-size: 13px; color: #69828a; line-height: 1.6; padding: 10px 14px; border-radius: 8px; background: rgba(64, 158, 255, 0.06); }
.btn-group { display: flex; flex-direction: column; gap: 12px; }
.btn-card { border-radius: 12px; padding: 16px; border: 1px solid rgba(0, 0, 0, 0.06); display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; }
.btn-card--approve  { background: rgba(103, 194, 58, 0.04);  border-color: rgba(103, 194, 58, 0.2); }
.btn-card--reject  { background: rgba(245, 108, 108, 0.04); border-color: rgba(245, 108, 108, 0.2); }
.btn-card--cross   { background: rgba(230, 162, 60, 0.04);  border-color: rgba(230, 162, 60, 0.2); }
.btn-card--archive { background: rgba(144, 147, 153, 0.04); border-color: rgba(144, 147, 153, 0.2); }
.btn-card-body { flex: 1; min-width: 0; }
.btn-card-title { font-size: 14px; font-weight: 700; color: #303133; margin-bottom: 4px; }
.btn-card-desc { font-size: 12px; color: #909399; margin-bottom: 8px; }
</style>
