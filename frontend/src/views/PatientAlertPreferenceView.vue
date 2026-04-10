<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">个性化预警阈值</h3>
        <p class="page-subtitle">按个人情况调整阈值</p>
      </div>
      <div class="page-actions">
        <el-button :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveCurrent">保存当前配置</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">阈值优先级：个人 &gt; 系统 &gt; 默认</div>
        <div class="info-strip-desc">可按指标启用个性化规则，系统将据此重新计算风险等级。</div>
      </div>
      <el-tag effect="light">当前编辑：{{ activeIndicator }}</el-tag>
    </div>

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">已启用个性化指标</div>
        <div class="kpi-value">{{ enabledCount }}</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">可配置指标</div>
        <div class="kpi-value">{{ indicatorOptions.length }}</div>
      </div>
    </div>

    <el-segmented v-model="activeIndicator" :options="indicatorOptions" class="indicator-switch" @change="fillFormByIndicator" />

    <el-form :model="form" label-width="120px" class="pref-form">
      <el-form-item label="指标类型">
        <el-input v-model="form.indicatorType" disabled />
      </el-form-item>
      <el-form-item :label="highRuleLabel">
        <el-input v-model="form.highRule" :placeholder="highRulePlaceholder" />
      </el-form-item>
      <el-form-item :label="mediumRuleLabel">
        <el-input v-model="form.mediumRule" :placeholder="mediumRulePlaceholder" />
      </el-form-item>
      <el-form-item label="启用个性化阈值">
        <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>

    <div class="form-actions">
      <el-button @click="applyRecommended">恢复推荐阈值</el-button>
      <el-button type="primary" plain @click="goAlerts">查看预警结果</el-button>
    </div>

    <el-divider />

    <el-table :data="tableData" border v-loading="loading" empty-text="暂无配置">
      <el-table-column prop="indicatorType" label="指标" width="120" />
      <el-table-column prop="highRule" label="高风险阈值" width="160" />
      <el-table-column prop="mediumRule" label="中风险阈值" width="160" />
      <el-table-column label="启用状态" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'">
            {{ scope.row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link type="primary" @click="switchIndicator(scope.row.indicatorType)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPatientAlertPreferencesApi, updatePatientAlertPreferenceApi } from '../api/modules'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const activeIndicator = ref('血压')
const indicatorOptions = ['血压', '血糖', '体重']
const defaults = {
  血压: { highRule: '180/120', mediumRule: '140/90', enabled: 0 },
  血糖: { highRule: '16.7', mediumRule: '11.1', enabled: 0 },
  体重: { highRule: '200', mediumRule: '', enabled: 0 }
}

const prefMap = ref({
  血压: { indicatorType: '血压', ...defaults['血压'] },
  血糖: { indicatorType: '血糖', ...defaults['血糖'] },
  体重: { indicatorType: '体重', ...defaults['体重'] }
})

const form = reactive({
  indicatorType: '血压',
  highRule: '180/120',
  mediumRule: '140/90',
  enabled: 0
})

const tableData = computed(() => indicatorOptions.map((it) => prefMap.value[it]))
const highRuleLabel = computed(() => (activeIndicator.value === '血压' ? '高风险阈值(收/舒)' : '高风险阈值'))
const mediumRuleLabel = computed(() => (activeIndicator.value === '血压' ? '中风险阈值(收/舒)' : '中风险阈值'))
const highRulePlaceholder = computed(() => (activeIndicator.value === '血压' ? '例如 180/120' : '请输入正数'))
const mediumRulePlaceholder = computed(() => (activeIndicator.value === '血压' ? '例如 140/90' : '请输入正数，可留空'))
const enabledCount = computed(() => tableData.value.filter((item) => item.enabled === 1).length)

const fillFormByIndicator = () => {
  const selected = prefMap.value[activeIndicator.value]
  if (!selected) return
  form.indicatorType = selected.indicatorType
  form.highRule = selected.highRule || ''
  form.mediumRule = selected.mediumRule || ''
  form.enabled = selected.enabled ?? 0
}

const switchIndicator = (indicatorType) => {
  activeIndicator.value = indicatorType
  fillFormByIndicator()
}

const applyRecommended = () => {
  const d = defaults[activeIndicator.value]
  form.highRule = d.highRule
  form.mediumRule = d.mediumRule
  form.enabled = 0
}

const validate = () => {
  const highRule = String(form.highRule || '').trim()
  const mediumRule = String(form.mediumRule || '').trim()

  if (!highRule) {
    return '高风险阈值不能为空'
  }

  if (form.indicatorType === '血压') {
    if (!/^[1-9]\d{1,2}\/[1-9]\d{1,2}$/.test(highRule)) {
      return '血压高风险阈值格式应为 xx/xx'
    }
    if (mediumRule && !/^[1-9]\d{1,2}\/[1-9]\d{1,2}$/.test(mediumRule)) {
      return '血压中风险阈值格式应为 xx/xx'
    }
    return ''
  }

  const high = Number(highRule)
  if (!Number.isFinite(high) || high <= 0) {
    return '高风险阈值必须为正数'
  }
  if (mediumRule) {
    const medium = Number(mediumRule)
    if (!Number.isFinite(medium) || medium <= 0) {
      return '中风险阈值必须为正数'
    }
  }
  return ''
}

const load = async () => {
  loading.value = true
  try {
    const list = await listPatientAlertPreferencesApi()
    const nextMap = {
      血压: { indicatorType: '血压', ...defaults['血压'] },
      血糖: { indicatorType: '血糖', ...defaults['血糖'] },
      体重: { indicatorType: '体重', ...defaults['体重'] }
    }
    ;(list || []).forEach((item) => {
      if (nextMap[item.indicatorType]) {
        nextMap[item.indicatorType] = {
          indicatorType: item.indicatorType,
          highRule: item.highRule || nextMap[item.indicatorType].highRule,
          mediumRule: item.mediumRule || nextMap[item.indicatorType].mediumRule,
          enabled: item.enabled ?? 0
        }
      }
    })
    prefMap.value = nextMap
    fillFormByIndicator()
  } catch (err) {
    ElMessage.error(err?.message || '加载个性化阈值失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const saveCurrent = async () => {
  const msg = validate()
  if (msg) {
    ElMessage.warning(msg)
    return
  }

  const payload = {
    indicatorType: form.indicatorType,
    highRule: String(form.highRule || '').trim(),
    mediumRule: String(form.mediumRule || '').trim(),
    enabled: form.enabled
  }

  saving.value = true
  try {
    await updatePatientAlertPreferenceApi(payload)
    prefMap.value[form.indicatorType] = { ...payload }
    ElMessage.success('个性化阈值保存成功')
  } catch (err) {
    ElMessage.error(err?.message || '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

const goAlerts = () => {
  router.push('/patient/alerts')
}

onMounted(() => {
  fillFormByIndicator()
  load()
})
</script>

<style scoped>
.indicator-switch {
  margin: 8px 0 14px;
}

.pref-form {
  max-width: 620px;
}

.form-actions {
  margin-top: 8px;
  display: inline-flex;
  gap: 8px;
}
</style>
