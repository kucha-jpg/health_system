<template>
  <el-card class="section-card" style="margin-bottom: 12px" shadow="never">
    <template #header>患者档案</template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="姓名">{{ archive?.name || patientName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="年龄">{{ archive?.age ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="既往病史">{{ archive?.medicalHistory || '-' }}</el-descriptions-item>
      <el-descriptions-item label="用药史">{{ archive?.medicationHistory || '-' }}</el-descriptions-item>
      <el-descriptions-item label="过敏史">{{ archive?.allergyHistory || '-' }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
defineProps({
  archive: { type: Object, default: null },
  patientName: { type: String, default: '-' }
})
</script>
