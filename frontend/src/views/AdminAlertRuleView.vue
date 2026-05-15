<template>
  <div class="alert-page">
    <el-card class="fade-in-page">
      <div class="page-header">
        <div>
          <h3 class="page-title">预警规则管理</h3>
        </div>
        <div class="page-actions">
          <el-button @click="load">刷新</el-button>
          <el-button type="primary" @click="openDialog()">新增规则</el-button>
        </div>
      </div>

      <div class="soft-tip">规则总数 {{ rules.length }}，启用指标 {{ enabledIndicatorTypes.length }} 个。</div>

      <el-table :data="rules" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="indicatorType" label="指标类型" width="120" />
      <el-table-column prop="highRule" label="高风险阈值" width="180" />
      <el-table-column prop="mediumRule" label="中风险阈值" width="180" />
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

    <el-card class="fade-in-page compact-card">
      <div class="page-header">
        <div>
          <h3 class="page-title">健康指标类型管理</h3>
        </div>
        <div class="page-actions">
          <el-button type="primary" @click="openIndicatorDialog()">新增指标</el-button>
        </div>
      </div>

      <el-table :data="indicatorTypes" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="indicatorType" label="指标类型" width="160" />
      <el-table-column prop="displayName" label="展示名称" width="160" />
      <el-table-column label="状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">{{ scope.row.enabled === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link type="primary" @click="openIndicatorDialog(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="deleteIndicator(scope.row)">删除</el-button>
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
  </div>

  <el-dialog
    v-model="visible"
    title="预警规则"
    center
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <el-form :model="form" label-width="110px">
      <el-form-item label="指标类型">
        <el-select v-model="form.indicatorType" style="width:100%" :disabled="!!form.id">
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
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="save">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="indicatorVisible"
    title="健康指标类型"
    center
    align-center
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
  >
    <el-form :model="indicatorForm" label-width="110px">
      <el-form-item label="指标类型">
        <el-input v-model="indicatorForm.indicatorType" :disabled="!!indicatorForm.id" placeholder="例如：血压" />
      </el-form-item>
      <el-form-item label="展示名称">
        <el-input v-model="indicatorForm.displayName" placeholder="例如：血压" />
      </el-form-item>
      <el-form-item label="启用状态">
        <el-switch v-model="indicatorForm.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="indicatorVisible = false">取消</el-button>
      <el-button type="primary" @click="saveIndicator">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAlertRuleApi,
  createIndicatorTypeApi,
  deleteIndicatorTypeApi,
  listAlertRulesApi,
  listIndicatorTypesApi,
  updateAlertRuleApi,
  updateIndicatorTypeApi
} from '../api/modules'

const rules = ref([])
const indicatorTypes = ref([])
const visible = ref(false)
const indicatorVisible = ref(false)
const form = reactive({ id: null, indicatorType: '', highRule: '', mediumRule: '', enabled: 1 })
const indicatorForm = reactive({ id: null, indicatorType: '', displayName: '', enabled: 1 })
const enabledIndicatorTypes = ref([])

const load = async () => {
  rules.value = await listAlertRulesApi()
  indicatorTypes.value = await listIndicatorTypesApi({ includeDisabled: true })
  enabledIndicatorTypes.value = indicatorTypes.value.filter(item => item.enabled === 1)
}

const openDialog = (row) => {
  const defaultType = enabledIndicatorTypes.value[0]?.indicatorType || ''
  Object.assign(form, { id: null, indicatorType: defaultType, highRule: '', mediumRule: '', enabled: 1 }, row || {})
  visible.value = true
}

const save = async () => {
  try {
    await ElMessageBox.confirm(form.id ? '确认修改该预警规则？' : '确认新增该预警规则？', '保存确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch {
    return
  }
  try {
    if (form.id) {
      await updateAlertRuleApi(form)
    } else {
      await createAlertRuleApi(form)
    }
    ElMessage.success('保存成功')
    visible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '保存规则失败，请稍后重试')
  }
}

const openIndicatorDialog = (row) => {
  Object.assign(indicatorForm, { id: null, indicatorType: '', displayName: '', enabled: 1 }, row || {})
  indicatorVisible.value = true
}

const saveIndicator = async () => {
  try {
    await ElMessageBox.confirm(indicatorForm.id ? '确认修改该指标类型？' : '确认新增该指标类型？', '保存确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      closeOnClickModal: false,
      closeOnPressEscape: false,
      showClose: false
    })
  } catch {
    return
  }
  try {
    if (indicatorForm.id) {
      await updateIndicatorTypeApi(indicatorForm)
    } else {
      await createIndicatorTypeApi(indicatorForm)
    }
    ElMessage.success('保存成功')
    indicatorVisible.value = false
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '保存指标类型失败，请稍后重试')
  }
}

const deleteIndicator = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除指标类型「${row.displayName || row.indicatorType}」吗？删除后关联的预警规则可能失效。`,
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        closeOnClickModal: false,
        closeOnPressEscape: false,
        showClose: false
      }
    )
  } catch {
    return
  }
  try {
    await deleteIndicatorTypeApi(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    ElMessage.error(err?.message || '删除指标类型失败，请稍后重试')
  }
}

onMounted(load)
</script>

<style scoped>
.alert-page {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.compact-card {
  margin-top: 0;
}

.alert-page :deep(.el-card__body) {
  padding-top: 14px;
  padding-bottom: 14px;
}

.alert-page :deep(.el-table .cell) {
  padding-top: 6px;
  padding-bottom: 6px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
