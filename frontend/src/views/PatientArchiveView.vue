<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">个人健康档案</h3>
        <p class="page-subtitle">维护病史与用药信息</p>
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

    <div class="kpi-grid">
      <div class="kpi-card">
        <div class="kpi-label">已填写字段</div>
        <div class="kpi-value">{{ filledCount }}/5</div>
      </div>
      <div class="kpi-card">
        <div class="kpi-label">建议更新时间</div>
        <div class="kpi-value">每周</div>
      </div>
    </div>

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
import { ElMessage } from 'element-plus'
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
  const data = await getArchiveApi()
  if (data) Object.assign(form, data)
}

const save = async () => {
  await saveArchiveApi(form)
  ElMessage.success('保存成功')
}

onMounted(load)
</script>
