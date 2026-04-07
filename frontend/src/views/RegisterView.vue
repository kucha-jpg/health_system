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
          <el-button type="primary" class="auth-action" size="large" :loading="submitting" @click="onRegister">{{ copy.submitText }}</el-button>
          <el-button class="auth-action" size="large" plain @click="goBack">返回登录</el-button>
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

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/login')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #edf3ef 0%, #e4ece8 100%);
}

.auth-card {
  width: min(520px, 100%);
  border-radius: 14px;
  padding: 20px 20px;
  border: 1px solid #e6eeea;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(22, 53, 45, 0.1);
}

.auth-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.2;
  color: #1f3f36;
}

.auth-subtitle {
  margin: 6px 0 12px;
  color: #5e756c;
  font-size: 15px;
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
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.auth-action {
  width: 100%;
  height: 52px;
  font-size: 16px;
  margin-left: 0;
}

.auth-footer {
  margin-top: 10px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
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
    grid-template-columns: 1fr;
  }
}
</style>
