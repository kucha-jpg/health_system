<template>
  <div class="page-shell">
    <el-card class="page-shell fade-in-page">
      <div class="page-header">
        <div>
          <h3 class="page-title">预警规则管理</h3>
          <p class="page-subtitle">维护预警阈值与指标开关</p>
        </div>
        <div class="page-actions">
          <el-button @click="load">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增规则</el-button>
        </div>
      </div>

      <div class="info-strip">
        <div>
          <div class="info-strip-title">规则维护建议先确认指标启用状态，再配置高/中风险阈值</div>
          <div class="info-strip-desc">规则 {{ rules.length }} 条，启用指标 {{ enabledIndicatorTypes.length }} 个。</div>
        </div>
        <el-tag effect="light">规则总数 {{ rules.length }}</el-tag>
      </div>

      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-label">规则总数</div>
          <div class="kpi-value">{{ rules.length }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">启用规则</div>
          <div class="kpi-value">{{ enabledRuleCount }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">停用规则</div>
          <div class="kpi-value">{{ disabledRuleCount }}</div>
        </div>
      </div>

      <el-card class="section-card" shadow="never">
      <template #header>预警规则列表</template>
      <el-table :data="rules" border size="large" class="rule-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="indicatorType" label="指标类型" width="160" />
      <el-table-column prop="highRule" label="高风险阈值" min-width="260" />
      <el-table-column prop="mediumRule" label="中风险阈值" min-width="260" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">{{ scope.row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无预警规则</div>
          <div class="empty-desc">可先新增规则并启用对应指标类型。</div>
        </div>
      </template>
      </el-table>
      </el-card>
    </el-card>

    <el-card class="fade-in-page page-shell">
      <div class="page-header">
        <div>
          <h3 class="page-title">健康指标类型管理</h3>
          <p class="page-subtitle">维护可上报指标</p>
        </div>
        <div class="page-actions">
          <el-button type="primary" @click="openIndicatorDialog()">新增指标</el-button>
        </div>
      </div>

      <div class="info-strip">
        <div>
          <div class="info-strip-title">指标类型是上报入口基础，停用后相关规则将无法生效</div>
          <div class="info-strip-desc">指标总数 {{ indicatorTypes.length }}，启用 {{ enabledIndicatorTypes.length }}。</div>
        </div>
        <el-tag effect="light">指标总数 {{ indicatorTypes.length }}</el-tag>
      </div>

      <div class="kpi-grid">
        <div class="kpi-card">
          <div class="kpi-label">指标总数</div>
          <div class="kpi-value">{{ indicatorTypes.length }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">启用指标</div>
          <div class="kpi-value">{{ enabledIndicatorTypes.length }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">停用指标</div>
          <div class="kpi-value">{{ disabledIndicatorCount }}</div>
        </div>
      </div>

      <el-card class="section-card" shadow="never">
      <template #header>指标类型列表</template>
      <el-table :data="indicatorTypes" border size="large" class="rule-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="indicatorType" label="指标类型" width="180" />
      <el-table-column prop="displayName" label="展示名称" min-width="180" />
      <el-table-column label="来源" width="110">
        <template #default="scope">
          <el-tag v-if="isCoreIndicator(scope.row.indicatorType)" type="success" effect="plain">内置核心</el-tag>
          <el-tag v-else type="info" effect="plain">自定义</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">{{ scope.row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="scope">
          <el-button link type="primary" @click="openIndicatorDialog(scope.row)">编辑</el-button>
          <el-button
            v-if="scope.row.enabled === 1"
            link
            type="warning"
            :disabled="isCoreIndicator(scope.row.indicatorType)"
            @click="archiveIndicator(scope.row)"
          >
            归档指标
          </el-button>
          <el-button
            v-else
            link
            type="success"
            :disabled="isCoreIndicator(scope.row.indicatorType)"
            @click="restoreIndicator(scope.row)"
          >
            恢复启用
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="empty-state">
          <div class="empty-illustration"></div>
          <div class="empty-title">暂无指标类型</div>
          <div class="empty-desc">建议先创建基础指标，再维护对应预警规则。</div>
        </div>
      </template>
      </el-table>
      </el-card>
    </el-card>
  </div>

  <el-dialog v-model="visible" width="860px" class="rule-edit-dialog app-dialog-style" destroy-on-close :show-close="false" align-center>
    <template #header>
      <div class="dialog-head">
        <div class="dialog-title-row">
          <span class="dialog-title">预警规则配置</span>
        </div>
        <div class="dialog-subtitle">设置高风险与中风险阈值，并决定该规则是否启用</div>
      </div>
    </template>
    <el-form :model="form" label-width="110px" class="dialog-form">
      <el-form-item label="指标类型">
        <el-select v-model="form.indicatorType" class="w-full" :disabled="!!form.id">
          <el-option
            v-for="item in enabledIndicatorTypes"
            :key="item.id"
            :label="item.displayName"
            :value="item.indicatorType"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="高风险阈值">
        <el-input v-model="form.highRule" placeholder="血压示例: 180/120；血糖示例: 16.7" />
      </el-form-item>
      <el-form-item label="中风险阈值">
        <el-input v-model="form.mediumRule" placeholder="血压示例: 140/90；血糖示例: 11.1" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer-actions">
        <el-button class="dialog-btn-secondary" @click="visible = false">取消</el-button>
        <el-button class="dialog-btn-primary" type="primary" @click="save">保存</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="indicatorVisible" width="780px" class="rule-edit-dialog app-dialog-style" destroy-on-close :show-close="false" align-center>
    <template #header>
      <div class="dialog-head indicator-dialog-head">
        <div class="dialog-title-row">
          <span class="dialog-title">健康指标类型</span>
        </div>
        <div class="dialog-subtitle">维护指标名称和展示名，状态请在列表中归档或恢复</div>
      </div>
    </template>
    <el-form :model="indicatorForm" label-width="110px" class="dialog-form">
      <el-form-item label="指标类型" class="indicator-emphasis-item">
        <el-input
          v-model="indicatorForm.indicatorType"
          :disabled="!!indicatorForm.id && isCoreIndicator(indicatorForm.originalIndicatorType)"
          placeholder="例如：血压"
        />
      </el-form-item>
      <el-form-item label="展示名称" class="indicator-emphasis-item">
        <el-input v-model="indicatorForm.displayName" placeholder="例如：血压" />
      </el-form-item>
      <el-form-item v-if="!indicatorForm.id" label="启用状态">
        <el-switch
          v-model="indicatorForm.enabled"
          :active-value="1"
          :inactive-value="0"
        />
      </el-form-item>
      <el-alert
        v-else
        type="info"
        :closable="false"
        show-icon
        title="编辑仅修改指标信息，状态请使用“归档指标/恢复启用”操作"
      />
    </el-form>
    <template #footer>
      <div class="dialog-footer-actions">
        <el-button class="dialog-btn-primary" type="primary" @click="saveIndicator">保存</el-button>
        <el-button class="dialog-btn-secondary" @click="indicatorVisible = false">取消</el-button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="archiveVisible" width="780px" class="rule-edit-dialog app-dialog-style" destroy-on-close :show-close="false" align-center>
    <template #header>
      <div class="dialog-head indicator-dialog-head">
        <div class="dialog-title-row">
          <span class="dialog-title">健康指标类型</span>
        </div>
        <div class="dialog-subtitle">维护指标名称和展示名，状态请在列表中归档或恢复</div>
      </div>
    </template>
    <div class="archive-confirm-text">确认归档指标 {{ archiveTarget?.indicatorType || '-' }} 吗？归档后将不再用于新增上报。</div>
    <template #footer>
      <div class="dialog-footer-actions">
        <el-button class="dialog-btn-primary" type="primary" @click="confirmArchive">确认归档</el-button>
        <el-button class="dialog-btn-secondary" @click="archiveVisible = false">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAlertRuleApi,
  createIndicatorTypeApi,
  listAlertRulesApi,
  listIndicatorTypesApi,
  updateAlertRuleApi,
  updateIndicatorTypeApi
} from '../api/modules'

const rules = ref([])
const indicatorTypes = ref([])
const visible = ref(false)
const indicatorVisible = ref(false)
const archiveVisible = ref(false)
const archiveTarget = ref(null)
const form = reactive({ id: null, indicatorType: '', highRule: '', mediumRule: '', enabled: 1 })
const indicatorForm = reactive({ id: null, indicatorType: '', displayName: '', enabled: 1, originalIndicatorType: '' })
const enabledIndicatorTypes = ref([])
const enabledRuleCount = ref(0)
const disabledRuleCount = ref(0)
const disabledIndicatorCount = ref(0)

const CORE_INDICATORS = new Set(['血压', '血糖', '体重', '服药', 'BLOOD_PRESSURE', 'BLOOD_SUGAR', 'WEIGHT', 'MEDICATION'])

const isCoreIndicator = (indicatorType) => CORE_INDICATORS.has(String(indicatorType || '').trim())

const load = async () => {
  rules.value = await listAlertRulesApi()
  indicatorTypes.value = await listIndicatorTypesApi({ includeDisabled: true })
  enabledIndicatorTypes.value = indicatorTypes.value.filter(item => item.enabled === 1)
  enabledRuleCount.value = rules.value.filter((item) => item.enabled === 1).length
  disabledRuleCount.value = rules.value.length - enabledRuleCount.value
  disabledIndicatorCount.value = indicatorTypes.value.length - enabledIndicatorTypes.value.length
}

const openDialog = (row) => {
  const defaultType = enabledIndicatorTypes.value[0]?.indicatorType || ''
  Object.assign(form, { id: null, indicatorType: defaultType, highRule: '', mediumRule: '', enabled: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  if (form.id) {
    await updateAlertRuleApi(form)
  } else {
    await createAlertRuleApi(form)
  }
  ElMessage.success('保存成功')
  visible.value = false
  await load()
}

const openIndicatorDialog = (row) => {
  const base = { id: null, indicatorType: '', displayName: '', enabled: 1, originalIndicatorType: '' }
  Object.assign(indicatorForm, base, row || {})
  indicatorForm.originalIndicatorType = row?.indicatorType || ''
  indicatorVisible.value = true
}

const saveIndicator = async () => {
  const payload = {
    id: indicatorForm.id,
    indicatorType: indicatorForm.indicatorType,
    displayName: indicatorForm.displayName,
    enabled: indicatorForm.enabled
  }
  if (indicatorForm.id) {
    await updateIndicatorTypeApi(payload)
  } else {
    await createIndicatorTypeApi(payload)
  }
  ElMessage.success('保存成功')
  indicatorVisible.value = false
  await load()
}

const archiveIndicator = async (row) => {
  if (row.enabled !== 1) {
    ElMessage.info('该指标已归档')
    return
  }
  if (isCoreIndicator(row.indicatorType)) {
    ElMessage.warning('内置核心指标不支持归档')
    return
  }
  archiveTarget.value = row
  archiveVisible.value = true
}

const confirmArchive = async () => {
  if (!archiveTarget.value) {
    archiveVisible.value = false
    return
  }
  await updateIndicatorTypeApi({
    id: archiveTarget.value.id,
    indicatorType: archiveTarget.value.indicatorType,
    displayName: archiveTarget.value.displayName,
    enabled: 0
  })
  archiveVisible.value = false
  archiveTarget.value = null
  ElMessage.success('归档成功')
  await load()
}

const restoreIndicator = async (row) => {
  if (row.enabled === 1) {
    ElMessage.info('该指标已启用')
    return
  }
  if (isCoreIndicator(row.indicatorType)) {
    ElMessage.warning('内置核心指标始终保持启用')
    return
  }
  await ElMessageBox.confirm(
    `确认恢复指标 ${row.indicatorType} 吗？恢复后可用于新增上报。`,
    '恢复确认',
    {
      type: 'info',
      confirmButtonText: '确认恢复',
      cancelButtonText: '取消',
      showClose: false,
      closeOnClickModal: false,
      closeOnPressEscape: false
    }
  )
  await updateIndicatorTypeApi({
    id: row.id,
    indicatorType: row.indicatorType,
    displayName: row.displayName,
    enabled: 1
  })
  ElMessage.success('已恢复启用')
  await load()
}

onMounted(load)
</script>

<style scoped>
:global(.rule-edit-dialog) {
  border-radius: 0;
  background: #ffffff;
  overflow: hidden;
  border: 1px solid #dbe8ec;
  box-shadow: 0 24px 52px rgba(16, 70, 82, 0.18);
}

:global(.rule-edit-dialog .el-dialog__header) {
  padding: 0;
  border-bottom: 0;
  background: #ffffff;
}

.dialog-head {
  padding: 24px 28px 18px;
  border-bottom: 1px solid #e7f0f2;
  background: linear-gradient(180deg, #f5fbfb 0%, #ffffff 100%);
}

.indicator-dialog-head {
  border-bottom: 0;
}

.indicator-dialog-head .dialog-title {
  font-size: 26px;
}

.indicator-dialog-head .dialog-subtitle {
  font-size: 16px;
  line-height: 1.9;
}

.dialog-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-title {
  font-size: 22px;
  font-weight: 700;
  color: #15383f;
}

.dialog-subtitle {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.8;
  color: #4f6870;
}

:global(.rule-edit-dialog .el-dialog__body) {
  padding: 26px 28px 12px;
}

.dialog-form {
  padding-top: 4px;
}

:global(.rule-edit-dialog .indicator-emphasis-item .el-form-item__label) {
  font-size: 16px;
  font-weight: 700;
  display: flex;
  align-items: center;
  min-height: 44px;
  line-height: 1.2;
}

:global(.rule-edit-dialog .indicator-emphasis-item .el-input__inner) {
  font-size: 16px;
}

:global(.rule-edit-dialog .el-form-item) {
  margin-bottom: 22px;
}

:global(.rule-edit-dialog .el-form-item__label) {
  font-size: 14px;
  font-weight: 600;
  color: #1f3b42;
}

:global(.rule-edit-dialog .el-input__wrapper),
:global(.rule-edit-dialog .el-select__wrapper) {
  min-height: 44px;
  padding-top: 6px;
  padding-bottom: 6px;
}

:global(.rule-edit-dialog .el-textarea__inner) {
  min-height: 94px !important;
  line-height: 1.7;
}

:global(.rule-edit-dialog .el-dialog__footer) {
  padding: 18px 28px 22px;
  border-top: 1px solid #e9f1f3;
}

.dialog-footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.dialog-btn-secondary {
  min-width: 108px;
  height: 42px;
  border-radius: 0;
  border-color: #cfdee2;
  color: #24454d;
}

.dialog-btn-primary {
  min-width: 130px;
  height: 42px;
  border-radius: 0;
  background: linear-gradient(135deg, #1a8a78 0%, #0f6c5f 100%);
  border-color: #0f6c5f;
}

.archive-confirm-text {
  font-size: 16px;
  color: #4f6870;
  line-height: 1.9;
  padding: 2px 0 8px;
}

:deep(.rule-table .el-table__cell) {
  padding-top: 14px;
  padding-bottom: 14px;
}
</style>
