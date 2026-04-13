<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h2 class="auth-title">{{ copy.title }}</h2>
      <p class="auth-subtitle">请输入账号和密码</p>

      <el-form :model="form" class="auth-form" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="form.username"
            :placeholder="copy.usernamePlaceholder"
            size="large"
            @keyup.enter="onLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            size="large"
            :placeholder="copy.passwordPlaceholder"
            @keyup.enter="onLogin"
          />
        </el-form-item>

        <el-button type="primary" class="auth-submit" size="large" :loading="submitting" @click="onLogin">
          {{ copy.submitText }}
        </el-button>

        <div class="auth-footer">
          <span>{{ copy.footerPrefix }}</span>
          <router-link to="/register">{{ copy.footerLinkText }}</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginApi } from '../api/modules'
import { authStore } from '../stores/auth'
import { AUTH_UI_COPY } from '../constants/auth-ui'

const router = useRouter()
const route = useRoute()
const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const copy = AUTH_UI_COPY.login

const onLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  submitting.value = true
  try {
    const data = await loginApi(form)
    authStore.setAuth(data)
    ElMessage.success('登录成功')
    router.push('/home')
  } finally {
    submitting.value = false
  }
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

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 22px 16px;
  display: grid;
  place-items: center;
  position: relative;
  overflow: hidden;
}

.auth-page::before,
.auth-page::after {
  content: '';
  position: absolute;
  pointer-events: none;
}

.auth-page::before {
  width: 440px;
  height: 440px;
  left: -160px;
  top: -130px;
  border-radius: 40% 60% 52% 48%;
  background: radial-gradient(circle, rgba(var(--brand-rgb), 0.3), transparent 68%);
  filter: blur(8px);
}

.auth-page::after {
  width: 340px;
  height: 340px;
  right: -120px;
  bottom: -90px;
  border-radius: 58% 42% 44% 56%;
  background: radial-gradient(circle, rgba(var(--brand-2-rgb), 0.34), transparent 72%);
  filter: blur(10px);
}

.auth-card {
  width: min(500px, 100%);
  border-radius: 24px;
  padding: 26px 24px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.65), rgba(255, 255, 255, 0.36)),
    radial-gradient(circle at 12% 8%, rgba(var(--brand-rgb), 0.12), transparent 42%);
  box-shadow: 0 24px 54px rgba(20, 52, 60, 0.24);
  -webkit-backdrop-filter: blur(18px);
  backdrop-filter: blur(18px);
  position: relative;
  overflow: hidden;
}

.auth-card::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.22), transparent 30%, transparent 60%, rgba(255, 255, 255, 0.2));
}

.auth-title {
  margin: 0;
  font-size: 34px;
  line-height: 1.2;
  color: var(--ink-1);
  font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
  position: relative;
  z-index: 1;
}

.auth-subtitle {
  margin: 6px 0 12px;
  color: var(--ink-2);
  font-size: 15px;
  position: relative;
  z-index: 1;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.auth-form :deep(.el-input__wrapper) {
  min-height: 52px;
  padding: 0 14px;
}

.auth-form :deep(.el-input__inner) {
  font-size: 16px;
}

.auth-submit {
  width: 100%;
  font-weight: 700;
  height: 52px;
  font-size: 16px;
  margin-top: 4px;
  letter-spacing: 0.3px;
}

.auth-form :deep(.auth-submit.el-button) {
  border-radius: 999px !important;
  padding: 0 20px !important;
  border: 0 !important;
  background: linear-gradient(120deg, var(--brand-1), var(--brand-2)) !important;
  color: #ffffff !important;
  box-shadow: 0 10px 24px rgba(var(--brand-rgb), 0.36) !important;
}

.auth-form :deep(.auth-submit.el-button:hover) {
  opacity: 0.92;
}

.auth-footer {
  margin-top: 12px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  color: var(--ink-2);
  position: relative;
  z-index: 1;
}

.auth-footer a {
  color: var(--brand-1);
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 640px) {
  .auth-page {
    padding: 12px;
  }

  .auth-card {
    width: min(100%, 460px);
    padding: 16px 12px;
  }

  .auth-title {
    font-size: 28px;
  }
}
</style>
