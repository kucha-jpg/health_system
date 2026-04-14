<template>
  <el-card class="page-shell role-home-v2">
    <div class="page-header">
      <div>
        <h3 class="page-title">工作首页</h3>
        <p class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>

    <template v-if="role === 'ADMIN'">
      <div class="overview-grid overview-grid-admin" v-loading="loading">
        <button
          v-for="card in adminCards"
          :key="card.key"
          type="button"
          :class="['overview-card', { 'overview-card--highlight': card.key === 'monitor' }]"
          @click="router.push(card.path)"
        >
          <div class="overview-head">
            <h4>{{ card.title }}</h4>
          </div>
          <p class="overview-main">{{ card.main }}</p>
          <p class="overview-sub">{{ card.sub }}</p>
        </button>
      </div>
    </template>

    <template v-else>
      <div class="overview-grid overview-grid-role" v-loading="roleLoading">
        <button
          v-for="card in roleCards"
          :key="card.key"
          type="button"
          :class="['overview-card', { 'overview-card--highlight': card.highlight }]"
          @click="router.push(card.path)"
        >
          <div class="overview-head">
            <h4>{{ card.title }}</h4>
          </div>
          <p class="overview-main">{{ card.main }}</p>
          <p class="overview-sub">{{ card.sub }}</p>
        </button>
      </div>

      <div v-if="role === 'DOCTOR'" class="role-guide-grid">
        <div class="guide-card">
          <h4>今日三步</h4>
          <ul class="focus-list">
            <li v-for="text in roleSteps" :key="text">{{ text }}</li>
          </ul>
        </div>
        <div class="guide-card">
          <h4>首页说明</h4>
          <p>{{ roleTip }}</p>
        </div>
      </div>

      <div v-else class="patient-rich-grid">
        <div class="guide-card">
          <h4>今日三步</h4>
          <ul class="focus-list">
            <li v-for="text in roleSteps" :key="text">{{ text }}</li>
          </ul>
        </div>
        <div class="guide-card">
          <h4>健康提醒</h4>
          <ul class="focus-list">
            <li v-for="text in patientHints" :key="text">{{ text }}</li>
          </ul>
        </div>
        <div class="guide-card patient-metric-card">
          <div class="patient-metric-item">
            <span>累计上报</span>
            <strong>{{ patientSummary.totalReports }}</strong>
          </div>
          <div class="patient-metric-item">
            <span>未处理预警</span>
            <strong>{{ patientSummary.openAlerts }}</strong>
          </div>
          <div class="patient-metric-item">
            <span>未读反馈</span>
            <strong>{{ patientSummary.unreadFeedback }}</strong>
          </div>
        </div>
      </div>
    </template>
  </el-card>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { authStore } from '../stores/auth'
import {
  getAdminFeedbackStatsApi,
  getDoctorAlertsApi,
  getDoctorGroupsApi,
  getMonitorOverviewApi,
  getPatientAlertsApi,
  getUnreadFeedbackCountApi,
  getUsersApi,
  listAlertRulesApi,
  listHealthDataApi,
  listNoticesApi,
  listOperationLogsPageApi,
  listRolesApi
} from '../api/modules'

const router = useRouter()
const role = authStore.role
const loading = ref(false)
const roleLoading = ref(false)
let timer = null

const adminSummary = reactive({
  users: { total: 0, enabled: 0, disabled: 0 },
  notices: { total: 0, published: 0, offline: 0 },
  rules: { total: 0, enabled: 0 },
  roles: { total: 0, configured: 0 },
  monitor: { totalUsers: 0, totalHealthData: 0, openAlerts: 0 },
  logs: { total: 0, failed: 0 },
  feedback: { totalCount: 0, pendingCount: 0, todayNewCount: 0 }
})

const doctorSummary = reactive({
  totalAlerts: 0,
  highRisk: 0,
  groupCount: 0,
  unreadFeedback: 0
})

const patientSummary = reactive({
  totalReports: 0,
  openAlerts: 0,
  unreadFeedback: 0
})

const subtitle = computed(() => {
  if (role === 'ADMIN') return '管理总览：聚合 7 个核心模块关键信息，快速判断系统运行状态。'
  if (role === 'DOCTOR') return '聚焦风险患者、团队协作与反馈闭环。'
  return '聚焦每日上报、风险感知与长期健康习惯。'
})

const adminCards = computed(() => ([
  {
    key: 'users',
    title: '账号管理',
    main: `账号总数 ${adminSummary.users.total}，启用 ${adminSummary.users.enabled}`,
    sub: `禁用 ${adminSummary.users.disabled}，用于用户生命周期管理`,
    path: '/admin/users'
  },
  {
    key: 'notices',
    title: '系统公告',
    main: `公告总数 ${adminSummary.notices.total}，发布中 ${adminSummary.notices.published}`,
    sub: `下线 ${adminSummary.notices.offline}，用于平台通知触达`,
    path: '/admin/notices'
  },
  {
    key: 'rules',
    title: '预警规则',
    main: `规则总数 ${adminSummary.rules.total}，启用 ${adminSummary.rules.enabled}`,
    sub: '用于健康风险分级判定与预警触发',
    path: '/admin/alert-rules'
  },
  {
    key: 'roles',
    title: '角色权限',
    main: `角色总数 ${adminSummary.roles.total}，已配置权限 ${adminSummary.roles.configured}`,
    sub: '用于访问控制和接口鉴权边界',
    path: '/admin/roles'
  },
  {
    key: 'monitor',
    title: '系统监控',
    main: `用户 ${adminSummary.monitor.totalUsers}，上报 ${adminSummary.monitor.totalHealthData}`,
    sub: `未处理预警 ${adminSummary.monitor.openAlerts}，用于运行态观测`,
    path: '/admin/monitor'
  },
  {
    key: 'logs',
    title: '操作日志',
    main: `日志总数 ${adminSummary.logs.total}，失败操作 ${adminSummary.logs.failed}`,
    sub: '用于审计追踪与异常排查',
    path: '/admin/logs'
  },
  {
    key: 'feedback',
    title: '反馈消息',
    main: `反馈总数 ${adminSummary.feedback.totalCount}，待处理 ${adminSummary.feedback.pendingCount}`,
    sub: `今日新增 ${adminSummary.feedback.todayNewCount}，用于问题闭环处理`,
    path: '/admin/feedback'
  }
]))

const roleCards = computed(() => {
  if (role === 'DOCTOR') {
    return [
      {
        key: 'doctor-alerts',
        title: '预警工作台',
        main: `待处理预警 ${doctorSummary.totalAlerts}`,
        sub: `高风险 ${doctorSummary.highRisk}，优先处理高分预警`,
        path: '/doctor/alerts',
        highlight: true
      },
      {
        key: 'doctor-groups',
        title: '群组管理',
        main: `群组数量 ${doctorSummary.groupCount}`,
        sub: '按群组组织随访和医生协作',
        path: '/doctor/groups',
        highlight: false
      },
      {
        key: 'doctor-feedback',
        title: '反馈通道',
        main: `未读反馈 ${doctorSummary.unreadFeedback}`,
        sub: '跟进患者问题并记录处理结果',
        path: '/feedback',
        highlight: false
      }
    ]
  }

  return [
    {
      key: 'patient-report',
      title: '健康上报',
      main: `累计上报 ${patientSummary.totalReports}`,
      sub: '持续记录是趋势分析的基础',
      path: '/patient/report',
      highlight: true
    },
    {
      key: 'patient-alerts',
      title: '预警详情',
      main: `未处理预警 ${patientSummary.openAlerts}`,
      sub: '查看风险变化并及时处理',
      path: '/patient/alerts',
      highlight: false
    },
    {
      key: 'patient-feedback',
      title: '反馈通道',
      main: `未读反馈 ${patientSummary.unreadFeedback}`,
      sub: '查看医生建议并形成闭环',
      path: '/feedback',
      highlight: false
    }
  ]
})

const roleSteps = computed(() => {
  if (role === 'DOCTOR') {
    return [
      '先处理高风险与高分预警，避免风险积压。',
      '按群组分配随访任务，提升协作效率。',
      '通过反馈通道同步处理结果，形成闭环。'
    ]
  }

  return [
    '固定时间完成今日健康上报。',
    '查看预警详情并按建议执行。',
    '通过反馈通道确认医生回复。'
  ]
})

const roleTip = computed(() => {
  if (role === 'DOCTOR') return '医生首页聚焦“风险处理 -> 协作执行 -> 反馈闭环”，让每日任务更聚焦。'
  return '患者首页聚焦“持续上报 -> 风险感知 -> 建议执行”，帮助形成稳定健康节奏。'
})

const patientHints = computed(() => [
  patientSummary.openAlerts > 0 ? '优先处理预警详情，必要时联系医生。' : '当前无未处理预警，继续保持稳定上报。',
  patientSummary.unreadFeedback > 0 ? '你有新的反馈回复，建议及时查看。' : '反馈通道暂无未读消息。',
  '建议固定时间上报，趋势更准确。'
])

const loadAdminSummary = async () => {
  if (role !== 'ADMIN') return
  loading.value = true
  try {
    const [users, notices, rules, roles, monitor, logPage, logFailed, feedback] = await Promise.all([
      getUsersApi({}),
      listNoticesApi({ includeOffline: true }),
      listAlertRulesApi(),
      listRolesApi(),
      getMonitorOverviewApi(),
      listOperationLogsPageApi({ pageNo: 1, pageSize: 1 }),
      listOperationLogsPageApi({ pageNo: 1, pageSize: 1, success: 0 }),
      getAdminFeedbackStatsApi()
    ])

    const userList = Array.isArray(users) ? users : []
    const noticeList = Array.isArray(notices) ? notices : []
    const ruleList = Array.isArray(rules) ? rules : []
    const roleList = Array.isArray(roles) ? roles : []

    adminSummary.users.total = userList.length
    adminSummary.users.enabled = userList.filter((item) => item.status === 1).length
    adminSummary.users.disabled = userList.filter((item) => item.status !== 1).length

    adminSummary.notices.total = noticeList.length
    adminSummary.notices.published = noticeList.filter((item) => item.status === 1).length
    adminSummary.notices.offline = noticeList.filter((item) => item.status !== 1).length

    adminSummary.rules.total = ruleList.length
    adminSummary.rules.enabled = ruleList.filter((item) => item.enabled === 1).length

    adminSummary.roles.total = roleList.length
    adminSummary.roles.configured = roleList.filter((item) => String(item.permission || '').trim().length > 0).length

    adminSummary.monitor.totalUsers = Number(monitor?.totalUsers) || 0
    adminSummary.monitor.totalHealthData = Number(monitor?.totalHealthData) || 0
    adminSummary.monitor.openAlerts = Number(monitor?.openAlerts) || 0

    adminSummary.logs.total = Number(logPage?.total) || 0
    adminSummary.logs.failed = Number(logFailed?.total) || 0

    adminSummary.feedback.totalCount = Number(feedback?.totalCount) || 0
    adminSummary.feedback.pendingCount = Number(feedback?.pendingCount) || 0
    adminSummary.feedback.todayNewCount = Number(feedback?.todayNewCount) || 0
  } finally {
    loading.value = false
  }
}

const loadRoleSummary = async () => {
  if (role === 'ADMIN') return
  roleLoading.value = true
  try {
    if (role === 'DOCTOR') {
      const [alerts, groups, unreadFeedback] = await Promise.all([
        getDoctorAlertsApi({ pageNo: 1, pageSize: 200, sortBy: 'risk_desc', minRiskScore: 0 }),
        getDoctorGroupsApi(),
        getUnreadFeedbackCountApi()
      ])
      const alertList = Array.isArray(alerts?.list) ? alerts.list : []
      doctorSummary.totalAlerts = Number(alerts?.total) || alertList.length
      doctorSummary.highRisk = alertList.filter((item) => item.riskLevel === 'HIGH').length
      doctorSummary.groupCount = Array.isArray(groups) ? groups.length : 0
      doctorSummary.unreadFeedback = Number(unreadFeedback) || 0
      return
    }

    const [reportPage, openAlertPage, unreadFeedback] = await Promise.all([
      listHealthDataApi({ pageNo: 1, pageSize: 1 }),
      getPatientAlertsApi({ status: 'OPEN', pageNo: 1, pageSize: 1 }),
      getUnreadFeedbackCountApi()
    ])

    patientSummary.totalReports = Number(reportPage?.total) || 0
    patientSummary.openAlerts = Number(openAlertPage?.total) || 0
    patientSummary.unreadFeedback = Number(unreadFeedback) || 0
  } finally {
    roleLoading.value = false
  }
}

onMounted(() => {
  if (role === 'ADMIN') {
    loadAdminSummary()
    timer = window.setInterval(loadAdminSummary, 15000)
  } else {
    loadRoleSummary()
    timer = window.setInterval(loadRoleSummary, 15000)
  }
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
})
</script>

<style scoped>
.role-home-v2 {
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.48)),
    radial-gradient(circle at 10% 14%, rgba(31, 143, 114, 0.12), transparent 32%);
  gap: 10px;
}

.overview-grid {
  display: grid;
  gap: 8px;
}

.overview-grid-admin {
  grid-template-columns: repeat(12, minmax(0, 1fr));
}

.overview-grid-admin .overview-card {
  grid-column: span 3;
}

.overview-grid-admin .overview-card:last-child {
  grid-column: span 6;
}

.overview-grid-role {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.overview-card {
  text-align: left;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
}

.overview-card:hover {
  background: rgba(255, 255, 255, 0.64);
}

.overview-card--highlight {
  border: 2px solid rgba(var(--brand-rgb), 0.8);
  background:
    linear-gradient(150deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.66)),
    radial-gradient(circle at 8% 10%, rgba(var(--brand-rgb), 0.28), transparent 42%);
  box-shadow: 0 0 0 2px rgba(var(--brand-rgb), 0.2);
}

.overview-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: baseline;
}

.overview-head h4 {
  margin: 0;
  font-size: 15px;
  color: #1f424c;
}

.overview-main {
  margin: 8px 0 0;
  font-size: 13px;
  font-weight: 700;
  color: #244a53;
}

.overview-sub {
  margin: 6px 0 0;
  font-size: 12px;
  color: #567078;
  line-height: 1.6;
}

.role-guide-grid {
  display: grid;
  grid-template-columns: minmax(0, 7fr) minmax(0, 5fr);
  gap: 8px;
}

.guide-card {
  border-radius: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.5);
}

.guide-card h4 {
  margin: 0;
  font-size: 14px;
  color: #204f57;
}

.guide-card p {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #48656c;
}

.focus-list {
  margin: 6px 0 0;
  padding-left: 16px;
  display: grid;
  gap: 4px;
  font-size: 13px;
  line-height: 1.6;
  color: #48656c;
}

.patient-rich-grid {
  display: grid;
  grid-template-columns: minmax(0, 6fr) minmax(0, 6fr);
  gap: 8px;
}

.patient-metric-card {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.patient-metric-item {
  border-radius: 10px;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.72);
  background: rgba(255, 255, 255, 0.52);
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #42666f;
  font-size: 12px;
}

.patient-metric-item strong {
  font-size: 20px;
  color: #224a55;
}

@media (max-width: 900px) {
  .overview-grid-admin .overview-card,
  .overview-grid-admin .overview-card:last-child {
    grid-column: span 6;
  }

  .overview-grid-role,
  .role-guide-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .patient-rich-grid {
    grid-template-columns: 1fr;
  }

  .patient-metric-card {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .overview-grid-admin,
  .overview-grid-role,
  .role-guide-grid,
  .patient-rich-grid,
  .patient-metric-card {
    grid-template-columns: 1fr;
  }

  .overview-grid-admin .overview-card,
  .overview-grid-admin .overview-card:last-child {
    grid-column: auto;
  }
}
</style>
