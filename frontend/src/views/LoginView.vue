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
  padding: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #edf3ef 0%, #e4ece8 100%);
}

.auth-card {
  width: min(500px, 100%);
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

.auth-submit {
  width: 100%;
  font-weight: 700;
  height: 52px;
  font-size: 16px;
  margin-top: 0;
}

.auth-footer {
  margin-top: 10px;
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
}

@media (max-width: 640px) {
  .auth-page {
    padding: 12px;
  }

  .auth-card {
      width: min(100%, 460px);
    padding: 16px 12px;
  }
}
</style>
