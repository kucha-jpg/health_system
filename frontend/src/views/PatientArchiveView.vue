<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">个人健康档案</h3>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="save">保存档案</el-button>
      </div>
    </div>

    <div class="info-strip">
      <div>
        <div class="info-strip-title">档案用于风险评分和个性化预警</div>
        <div class="info-strip-desc">建议每次就诊后更新一次，保持评估结果准确。</div>
      </div>
      <el-tag type="success" effect="light">完整度 {{ completeness }}%</el-tag>
    </div>

    <el-row :gutter="10" class="summary-row">
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card">已填写字段：{{ filledCount }}/5</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card">建议更新时间：每周</el-card></el-col>
      <el-col :xs="24" :sm="8"><el-card shadow="never" class="summary-stat-card summary-stat-card--accent">最近更新时间：{{ form.updateTime || '-' }}</el-card></el-col>
    </el-row>

    <el-card class="section-card" shadow="never">
      <template #header>基础档案信息</template>
      <el-form :model="form" label-width="120px">
        <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="年龄"><el-input v-model.number="form.age" /></el-form-item>
        <el-form-item label="病史"><el-input v-model="form.medicalHistory" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="用药史"><el-input v-model="form.medicationHistory" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="过敏史"><el-input v-model="form.allergyHistory" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </el-card>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showError, showConfirm } from '../utils/message'
import { getArchiveApi, saveArchiveApi } from '../api/modules'

const form = reactive({ name: '', age: null, medicalHistory: '', medicationHistory: '', allergyHistory: '' })

const filledCount = computed(() => {
  let count = 0
  if (String(form.name || '').trim()) count += 1
  if (Number.isFinite(Number(form.age)) && Number(form.age) > 0) count += 1
  if (String(form.medicalHistory || '').trim()) count += 1
  if (String(form.medicationHistory || '').trim()) count += 1
  if (String(form.allergyHistory || '').trim()) count += 1
  return count
})

const completeness = computed(() => Math.round((filledCount.value / 5) * 100))

const load = async () => {
  try {
    const data = await getArchiveApi()
    if (data) Object.assign(form, data)
  } catch (err) {
    showError(err?.message || '加载健康档案失败，请稍后重试')
  }
}

const save = async () => {
  try {
    await showConfirm('确认保存档案？', '保存确认')
  } catch {
    return
  }
  try {
    await saveArchiveApi(form)
    await load()
  } catch (err) {
    showError(err?.message || '保存失败')
  }
}

onMounted(load)
</script>

<style scoped>
.summary-row {
  margin-bottom: 12px;
}

.summary-stat-card {
  font-weight: 600;
  color: #2f4952;
}

.summary-stat-card--accent {
  color: #255c4a;
}
</style>
