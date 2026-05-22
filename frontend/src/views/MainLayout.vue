<template>
  <div class="layout">
    <aside class="sidebar" :class="{ 'sidebar--hidden': sidebarCollapsed }">
      <div class="logo">
        <div class="logo-mark">H+</div>
        <div>
          <div class="logo-title">医疗健康系统</div>
          <div class="logo-sub">院外管理协作平台</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item index="/home">{{ role === 'ADMIN' ? '工作首页' : '首页' }}</el-menu-item>

        <el-menu-item-group v-if="role === 'ADMIN'" title="管理中心">
          <el-menu-item index="/admin/users">账号管理</el-menu-item>
          <el-menu-item index="/admin/notices">系统公告</el-menu-item>
          <el-menu-item index="/admin/alert-rules">预警与指标配置</el-menu-item>
          <el-menu-item index="/admin/groups">群组治理</el-menu-item>
          <el-menu-item index="/admin/roles">角色权限</el-menu-item>
          <el-menu-item index="/admin/monitor">系统监控</el-menu-item>
          <el-menu-item index="/admin/logs">操作日志</el-menu-item>
          <el-menu-item index="/admin/feedback">
            <span class="menu-badge-item">
              <span>反馈消息</span>
              <el-badge v-if="pendingFeedbackCount > 0" :value="pendingFeedbackCount" :max="99" />
            </span>
          </el-menu-item>
        </el-menu-item-group>

        <template v-if="role === 'DOCTOR'">
          <el-menu-item index="/doctor/alerts">医生工作台</el-menu-item>
          <el-menu-item index="/doctor/groups">群组管理</el-menu-item>
          <el-menu-item index="/feedback">
            <span class="menu-badge-item">
              <span>反馈通道</span>
              <el-badge v-if="unreadFeedbackCount > 0" :value="unreadFeedbackCount" :max="99" />
            </span>
          </el-menu-item>
        </template>

        <el-menu-item-group v-if="role === 'PATIENT'" title="患者中心">
          <el-menu-item index="/patient/archive">个人档案</el-menu-item>
          <el-menu-item index="/patient/report">健康上报</el-menu-item>
          <el-menu-item index="/patient/data">历史数据</el-menu-item>
          <el-menu-item index="/patient/alerts">预警详情</el-menu-item>
          <el-menu-item index="/patient/alert-preferences">个性化阈值</el-menu-item>
          <el-menu-item index="/patient/summary">周报月报</el-menu-item>
          <el-menu-item index="/feedback">
            <span class="menu-badge-item">
              <span>反馈通道</span>
              <el-badge v-if="unreadFeedbackCount > 0" :value="unreadFeedbackCount" :max="99" />
            </span>
          </el-menu-item>
        </el-menu-item-group>
      </el-menu>
    </aside>
    <main class="main-content">
      <div class="topbar">
        <div class="topbar-left">
          <button class="sidebar-toggle" @click="toggleSidebar" aria-label="切换侧边栏">
            <span></span><span></span><span></span>
          </button>
          <el-button v-if="!isHome" size="small" plain @click="goBack">返回</el-button>
          <span v-else class="topbar-back-spacer" aria-hidden="true"></span>
          <span class="topbar-user">
            <span class="user-dot"></span>
            <span>{{ name }}（{{ role }}）</span>
          </span>
        </div>
        <div class="topbar-right">
          <el-select v-model="currentTheme" size="small" class="theme-switch" @change="applyTheme">
            <el-option v-for="item in themeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <template v-if="role === 'DOCTOR'">
            <div class="patient-search">
              <input
                v-model="patientSearchId"
                class="patient-search-input"
                placeholder="患者ID"
                @keyup.enter="searchPatient"
              />
              <el-button size="small" type="primary" @click="searchPatient">查患者</el-button>
            </div>
            <el-button v-if="$route.path !== '/doctor/alerts'" size="small" type="warning" plain @click="router.push('/doctor/alerts')">查看预警</el-button>
          </template>
          <el-button size="small" @click="logout">退出</el-button>
        </div>
      </div>
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showWarning, showConfirm } from '../utils/message'
import { authStore } from '../stores/auth'
import { getPendingFeedbackCountApi, getUnreadFeedbackCountApi, validateSessionApi } from '../api/modules'
import { themeOptions } from '../constants/layout'

const router = useRouter()
const route = useRoute()
const role = authStore.role
const name = authStore.name || '用户'
const sidebarCollapsed = ref(false)
const toggleSidebar = () => { sidebarCollapsed.value = !sidebarCollapsed.value }
const patientSearchId = ref('')
const searchPatient = () => {
  const id = String(patientSearchId.value || '').trim()
  if (!id) {
    showWarning('请输入患者ID')
    return
  }
  patientSearchId.value = ''
  router.push(`/doctor/patients/${id}`)
}
let sessionTimer = null
let bodyLockTimer = null
let feedbackTimer = null
const pendingFeedbackCount = ref(0)
const unreadFeedbackCount = ref(0)
const currentTheme = ref('mint')
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

const logout = async () => {
  try {
    await showConfirm('确认退出当前登录吗？', '退出确认')
    authStore.clear()
    router.push('/login')
  } catch (e) {
    // 用户取消退出，不做跳转
  }
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

const applyTheme = (theme) => {
  const nextTheme = theme || 'mint'
  currentTheme.value = nextTheme
  document.body.setAttribute('data-theme', nextTheme)
  window.localStorage.setItem('hs_theme', nextTheme)
}

function cleanupBodyLock() {
  if (document.body.style.paddingRight && !document.querySelector('.el-overlay')) {
    document.body.style.overflow = ''
    document.body.style.paddingRight = ''
  }
}

onMounted(() => {
  document.body.setAttribute('data-role', role || 'PATIENT')
  const savedTheme = window.localStorage.getItem('hs_theme')
  applyTheme(savedTheme || 'mint')
  checkSession()
  loadPendingFeedbackCount()
  loadUnreadFeedbackCount()
  sessionTimer = window.setInterval(checkSession, 10000)
  bodyLockTimer = window.setInterval(cleanupBodyLock, 500)
  feedbackTimer = window.setInterval(async () => {
    await loadPendingFeedbackCount()
    await loadUnreadFeedbackCount()
  }, 10000)
  document.addEventListener('visibilitychange', handleVisibilityChange)
  window.addEventListener('feedback:read', handleFeedbackRead)
  router.afterEach(() => { nextTick(cleanupBodyLock) })
})

onUnmounted(() => {
  if (sessionTimer) {
    window.clearInterval(sessionTimer)
    sessionTimer = null
  }
  if (bodyLockTimer) {
    window.clearInterval(bodyLockTimer)
    bodyLockTimer = null
  }
  if (feedbackTimer) {
    window.clearInterval(feedbackTimer)
    feedbackTimer = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  window.removeEventListener('feedback:read', handleFeedbackRead)
  document.body.removeAttribute('data-role')
})
</script>

<style scoped>
.sidebar-toggle {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 32px;
  height: 32px;
  padding: 6px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: background 0.2s;
}

.sidebar-toggle:hover {
  background: rgba(255, 255, 255, 0.72);
}

.sidebar-toggle span {
  display: block;
  width: 100%;
  height: 2px;
  border-radius: 2px;
  background: var(--ink-2);
}

.logo-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.2px;
  font-family: 'Noto Serif SC', 'Noto Sans SC', serif;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 700;
  color: #ffffff;
  background: linear-gradient(135deg, var(--brand-1), var(--brand-2));
  box-shadow: 0 8px 16px rgba(var(--brand-rgb), 0.32);
}

.logo-sub {
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-2);
  opacity: 0.9;
}

.menu-badge-item {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.topbar-right {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.patient-search {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.patient-search-input {
  width: 100px;
  height: 30px;
  padding: 0 8px;
  border: 1px solid rgba(var(--brand-rgb), 0.3);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.6);
  font-size: 13px;
  color: var(--ink-1);
  outline: none;
  transition: border-color 0.2s;
}

.patient-search-input:focus {
  border-color: rgba(var(--brand-rgb), 0.6);
}

.patient-search-input::placeholder {
  color: var(--ink-2);
  opacity: 0.6;
}

.theme-switch {
  width: 104px;
}

.topbar-user {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.56);
  border: 1px solid rgba(255, 255, 255, 0.64);
}

.user-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--brand-1);
  box-shadow: 0 0 0 3px rgba(var(--brand-rgb), 0.22);
}

@media (max-width: 900px) {
  .sidebar-toggle {
    display: flex;
  }

  .sidebar--hidden {
    display: none;
  }

  .theme-switch {
    width: 100%;
  }
}
</style>
