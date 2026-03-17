<template>
  <div class="login-page">
    <div class="auth-stage register-stage" :data-auth-caption="copy.caption">
      <el-card class="login-card auth-panel">
        <p class="auth-panel-kicker">{{ copy.kicker }}</p>
        <h2 class="auth-panel-title">{{ copy.title }}</h2>
        <p class="auth-panel-desc">{{ copy.desc }}</p>
        <el-form :model="form" class="auth-form" @submit.prevent>
          <el-form-item><el-input v-model="form.username" :placeholder="copy.usernamePlaceholder" @keyup.enter="onRegister" /></el-form-item>
          <el-form-item><el-input v-model="form.name" :placeholder="copy.namePlaceholder" @keyup.enter="onRegister" /></el-form-item>
          <el-form-item><el-input v-model="form.phone" :placeholder="copy.phonePlaceholder" @keyup.enter="onRegister" /></el-form-item>
          <el-form-item><el-input v-model="form.password" type="password" show-password :placeholder="copy.passwordPlaceholder" @keyup.enter="onRegister" /></el-form-item>
          <el-button type="primary" class="w-full auth-submit" @click="onRegister">{{ copy.submitText }}</el-button>
          <el-button class="w-full auth-back" plain @click="goBack">返回</el-button>
          <div class="auth-footer">
            <span>{{ copy.footerPrefix }}</span>
            <router-link to="/login">{{ copy.footerLinkText }}</router-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '../api/modules'
import { AUTH_UI_COPY } from '../constants/auth-ui'

const router = useRouter()
const form = reactive({ username: '', name: '', phone: '', password: '' })
const phoneReg = /^1[3-9]\d{9}$/
const copy = AUTH_UI_COPY.register

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

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/login')
}
</script>

<style scoped>
.register-stage::before {
  background: linear-gradient(120deg, rgba(38, 103, 209, 0.1), rgba(38, 103, 209, 0.02));
}
</style>
