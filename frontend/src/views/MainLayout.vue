<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">医疗健康系统</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/home">首页</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/users">账号管理</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/notices">系统公告</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/alert-rules">预警规则</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/roles">角色权限</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/monitor">系统监控</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/logs">操作日志</el-menu-item>
        <el-menu-item v-if="role === 'ADMIN'" index="/admin/feedback">
          <span style="display:flex; align-items:center; gap:8px;">
            <span>反馈消息</span>
            <el-badge v-if="pendingFeedbackCount > 0" :value="pendingFeedbackCount" :max="99" />
          </span>
        </el-menu-item>
        <el-menu-item v-if="role === 'DOCTOR'" index="/doctor/alerts">医生工作台</el-menu-item>
        <el-menu-item v-if="role === 'DOCTOR'" index="/doctor/groups">群组管理</el-menu-item>
        <el-menu-item v-if="role === 'DOCTOR'" index="/feedback">
          <span style="display:flex; align-items:center; gap:8px;">
            <span>反馈通道</span>
            <el-badge v-if="unreadFeedbackCount > 0" :value="unreadFeedbackCount" :max="99" />
          </span>
        </el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/archive">个人档案</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/report">健康上报</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/data">历史数据</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/alerts">预警详情</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/patient/summary">周报月报</el-menu-item>
        <el-menu-item v-if="role === 'PATIENT'" index="/feedback">
          <span style="display:flex; align-items:center; gap:8px;">
            <span>反馈通道</span>
            <el-badge v-if="unreadFeedbackCount > 0" :value="unreadFeedbackCount" :max="99" />
          </span>
        </el-menu-item>
      </el-menu>
    </aside>
    <main class="main-content">
      <div class="topbar">
        <div class="topbar-left">
          <el-button v-if="!isHome" size="small" plain @click="goBack">返回</el-button>
          <span v-else class="topbar-back-spacer" aria-hidden="true"></span>
          <span class="topbar-user">
            <span>👤</span>
            <span>{{ name }}（{{ role }}）</span>
          </span>
        </div>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authStore } from '../stores/auth'
import { getPendingFeedbackCountApi, getUnreadFeedbackCountApi, validateSessionApi } from '../api/modules'

const router = useRouter()
const route = useRoute()
const role = authStore.role
const name = authStore.name || '用户'
let sessionTimer = null
let feedbackTimer = null
const pendingFeedbackCount = ref(0)
const unreadFeedbackCount = ref(0)
const isHome = computed(() => route.path === '/home')

const checkSession = async () => {
  if (!authStore.token) return
  try {
    await validateSessionApi()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '未登录或登录已过期'
    authStore.setAuthNotice(msg)
    authStore.clear()
    if (router.currentRoute.value.path !== '/login') {
      router.push({ path: '/login', query: { notice: msg } })
    }
  }
}

const handleVisibilityChange = () => {
  if (document.visibilityState === 'visible') {
    checkSession()
  }
}

const logout = () => {
  authStore.clear()
  router.push('/login')
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/home')
}

const loadPendingFeedbackCount = async () => {
  if (role !== 'ADMIN') return
  try {
    pendingFeedbackCount.value = Number(await getPendingFeedbackCountApi()) || 0
  } catch (e) {
    pendingFeedbackCount.value = 0
  }
}

const loadUnreadFeedbackCount = async () => {
  if (role === 'ADMIN') return
  try {
    unreadFeedbackCount.value = Number(await getUnreadFeedbackCountApi()) || 0
  } catch (e) {
    unreadFeedbackCount.value = 0
  }
}

const handleFeedbackRead = () => {
  loadUnreadFeedbackCount()
}

onMounted(() => {
  checkSession()
  loadPendingFeedbackCount()
  loadUnreadFeedbackCount()
  sessionTimer = window.setInterval(async () => {
    await checkSession()
  }, 5000)
  feedbackTimer = window.setInterval(async () => {
    await loadPendingFeedbackCount()
    await loadUnreadFeedbackCount()
  }, 10000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('feedback:read', handleFeedbackRead)
})

onUnmounted(() => {
  if (sessionTimer) {
    window.clearInterval(sessionTimer)
    sessionTimer = null
  }
  if (feedbackTimer) {
    window.clearInterval(feedbackTimer)
    feedbackTimer = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('feedback:read', handleFeedbackRead)
})
</script>
