<template>
  <div class="login-page">
    <div class="auth-stage" :data-auth-caption="copy.caption">
      <el-card class="login-card auth-panel">
        <p class="auth-panel-kicker">{{ copy.kicker }}</p>
        <h2 class="auth-panel-title">{{ copy.title }}</h2>
        <p class="auth-panel-desc">{{ copy.desc }}</p>
        <el-form :model="form" class="auth-form" @submit.prevent>
          <el-form-item>
            <el-input v-model="form.username" :placeholder="copy.usernamePlaceholder" @keyup.enter="onLogin" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" show-password :placeholder="copy.passwordPlaceholder" @keyup.enter="onLogin" />
          </el-form-item>
          <el-button type="primary" class="w-full auth-submit" @click="onLogin">{{ copy.submitText }}</el-button>
          <div class="auth-footer">
            <span>{{ copy.footerPrefix }}</span>
            <router-link to="/register">{{ copy.footerLinkText }}</router-link>
          </div>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '../api/modules'
import { authStore } from '../stores/auth'
import { AUTH_UI_COPY } from '../constants/auth-ui'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const copy = AUTH_UI_COPY.login

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
