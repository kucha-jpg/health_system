<template>
  <el-card>
    <template #header>角色首页</template>
    <p v-if="role === 'ADMIN'">欢迎管理员，请通过左侧菜单管理患者/医生账号。</p>
    <p v-else-if="role === 'DOCTOR'">欢迎医生，请前往医生工作台处理健康预警闭环。</p>
    <p v-else>欢迎患者，请完善档案并上报健康数据。</p>
  </el-card>
</template>

<script setup>
import { authStore } from '../stores/auth'
const role = authStore.role
</script>
