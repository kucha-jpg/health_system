<template>
  <el-card class="page-shell">
    <div class="page-header">
      <div>
        <h3 class="page-title">个人健康档案</h3>
        <p class="page-subtitle">维护病史、用药史与过敏史，供医生持续随访参考</p>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="save">保存档案</el-button>
      </div>
    </div>

    <div class="soft-tip">档案信息将用于健康分析与预警评估，建议如实填写并及时更新。</div>

    <el-form :model="form" label-width="120px">
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="年龄"><el-input v-model.number="form.age" /></el-form-item>
      <el-form-item label="病史"><el-input v-model="form.medicalHistory" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="用药史"><el-input v-model="form.medicationHistory" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="过敏史"><el-input v-model="form.allergyHistory" type="textarea" :rows="3" /></el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { getArchiveApi, saveArchiveApi } from '../api/modules'

const form = reactive({ name: '', age: null, medicalHistory: '', medicationHistory: '', allergyHistory: '' })

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
