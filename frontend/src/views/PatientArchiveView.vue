<template>
  <el-card>
    <template #header>个人健康档案</template>
    <el-form :model="form" label-width="120px">
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="年龄"><el-input v-model.number="form.age" /></el-form-item>
      <el-form-item label="病史"><el-input v-model="form.medicalHistory" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="用药史"><el-input v-model="form.medicationHistory" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="过敏史"><el-input v-model="form.allergyHistory" type="textarea" :rows="3" /></el-form-item>
      <el-form-item>
        <el-button type="primary" @click="save">保存档案</el-button>
      </el-form-item>
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
