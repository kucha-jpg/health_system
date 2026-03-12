<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2>健康管理系统登录</h2>
      <el-form :model="form" @submit.prevent>
        <el-form-item><el-input v-model="form.username" placeholder="用户名" /></el-form-item>
        <el-form-item><el-input v-model="form.password" type="password" show-password placeholder="密码" /></el-form-item>
        <el-button type="primary" class="w-full" @click="onLogin">登录</el-button>
        <div style="margin-top: 12px; text-align: center">
          <router-link to="/register">患者注册</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '../api/modules'
import { authStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })

const onLogin = async () => {
  const data = await loginApi(form)
  authStore.setAuth(data)
  ElMessage.success('登录成功')
  router.push('/home')
}

onMounted(() => {
  const queryNotice = typeof route.query.notice === 'string' ? route.query.notice : ''
  const notice = queryNotice || authStore.consumeAuthNotice()
  if (!notice) return
  ElMessage.error({
    message: notice,
    duration: 12000,
    showClose: true
  })
  if (queryNotice) {
    router.replace('/login')
  }
})
</script>
