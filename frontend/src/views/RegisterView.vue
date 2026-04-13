<template>
  <div class="auth-page">
    <el-card class="auth-card" shadow="never">
      <h2 class="auth-title">{{ copy.title }}</h2>
      <p class="auth-subtitle">填写基础信息即可完成注册</p>

      <el-form :model="form" class="auth-form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" :placeholder="copy.usernamePlaceholder" size="large" @keyup.enter="onRegister" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.name" :placeholder="copy.namePlaceholder" size="large" @keyup.enter="onRegister" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.phone" :placeholder="copy.phonePlaceholder" size="large" @keyup.enter="onRegister" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password :placeholder="copy.passwordPlaceholder" size="large" @keyup.enter="onRegister" />
        </el-form-item>

        <div class="auth-actions">
          <el-button type="primary" class="auth-action auth-submit" size="large" :loading="submitting" @click="onRegister">{{ copy.submitText }}</el-button>
        </div>

        <div class="auth-footer">
          <span>{{ copy.footerPrefix }}</span>
          <router-link to="/login">{{ copy.footerLinkText }}</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi } from '../api/modules'
import { AUTH_UI_COPY } from '../constants/auth-ui'

const router = useRouter()
const form = reactive({ username: '', name: '', phone: '', password: '' })
const submitting = ref(false)
const phoneReg = /^1[3-9]\d{9}$/
const copy = AUTH_UI_COPY.register

const onRegister = async () => {
  if (!form.username || !form.name || !form.phone || !form.password) {
    ElMessage.error('请完整填写注册信息')
    return
  }
  if (!phoneReg.test(form.phone)) {
    ElMessage.error('手机号格式不正确')
    return
  }
  if (form.password.length < 6 || form.password.length > 20) {
    ElMessage.error('密码长度需在6-20位')
    return
  }
  submitting.value = true
  try {
    await registerApi(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    submitting.value = false
  }
}

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
  width: 480px;
  height: 480px;
  left: -180px;
  top: -130px;
  border-radius: 40% 60% 52% 48%;
  background: radial-gradient(circle, rgba(var(--brand-rgb), 0.28), transparent 70%);
  filter: blur(10px);
}

.auth-page::after {
  width: 360px;
  height: 360px;
  right: -120px;
  bottom: -90px;
  border-radius: 58% 42% 44% 56%;
  background: radial-gradient(circle, rgba(var(--brand-2-rgb), 0.34), transparent 72%);
  filter: blur(10px);
}

.auth-card {
  width: min(520px, 100%);
  border-radius: 24px;
  padding: 26px 24px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.66), rgba(255, 255, 255, 0.35)),
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

.auth-actions {
  display: flex;
  justify-content: center;
}

.auth-action {
  width: min(260px, 100%);
  height: 52px;
  font-size: 16px;
  margin-left: 0;
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

@media (max-width: 980px) {
  .auth-page {
    padding: 12px;
  }

  .auth-card {
    width: min(100%, 480px);
    padding: 16px 12px;
  }

  .auth-actions {
    width: 100%;
  }

  .auth-title {
    font-size: 28px;
  }
}
</style>
