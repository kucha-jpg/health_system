<template>
  <div class="page-shell">
    <el-card class="page-shell fade-in-page">
      <div class="page-header">
        <div>
          <h3 class="page-title">预警规则管理</h3>
          <p class="page-subtitle">维护高/中风险阈值，支持启停与指标联动配置</p>
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

    <el-card class="fade-in-page">
      <div class="page-header">
        <div>
          <h3 class="page-title">健康指标类型管理</h3>
          <p class="page-subtitle">统一维护可上报指标与展示名称，支持启停控制</p>
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
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button link type="primary" @click="openIndicatorDialog(scope.row)">编辑</el-button>
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

  <el-dialog v-model="visible" title="预警规则">
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

  <el-dialog v-model="indicatorVisible" title="健康指标类型">
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
import { ElMessage } from 'element-plus'
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
  Object.assign(indicatorForm, { id: null, indicatorType: '', displayName: '', enabled: 1 }, row || {})
  indicatorVisible.value = true
}

const saveIndicator = async () => {
  if (indicatorForm.id) {
    await updateIndicatorTypeApi(indicatorForm)
  } else {
    await createIndicatorTypeApi(indicatorForm)
  }
  ElMessage.success('保存成功')
  indicatorVisible.value = false
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
