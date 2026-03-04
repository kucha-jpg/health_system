<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>患者注册</h2>
      <el-form :model="form" @submit.prevent>
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.name" placeholder="姓名" /></el-form-item>
        <el-form-item><el-input v-model="form.phone" placeholder="手机号" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码(6-20位)" /></el-form-item>
        <el-button type="primary" class="w-full" @click="onRegister">注册</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '../api/modules'

const router = useRouter()
const form = reactive({ username: '', name: '', phone: '', password: '' })
const phoneReg = /^1[3-9]\d{9}$/

const onRegister = async () => {
  if (!phoneReg.test(form.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.error('密码长度需在6-20位')
    return
  }
  await registerApi(form)
  ElMessage.success('注册成功，请登录')
  router.push('/login')
}
</script>
