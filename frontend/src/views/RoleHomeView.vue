<template>
  <el-card class="page-shell role-home-v2 fade-in-page">
    <div class="page-header">
      <div>
        <h3 class="page-title">{{ role === 'ADMIN' ? '工作首页' : '首页' }}</h3>
      </div>
      <div class="page-actions">
        <el-button :loading="noticeLoading" @click="loadNotices">刷新公告</el-button>
      </div>
    </div>

    <OverviewCards
      :role="role"
      :loading="role === 'ADMIN' ? loading : roleLoading"
      :cards="cards"
      :steps="roleSteps"
      :hints="patientHints"
      :metrics="patientMetrics"
    />

    <div class="section-divider" aria-hidden="true"></div>

    <NoticeBoard
      :list="noticeList"
      :loading="noticeLoading"
      :show-manage-link="role === 'ADMIN'"
    />
  </el-card>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { showError } from '../utils/message'
import { authStore } from '../stores/auth'
import {
  getAdminFeedbackStatsApi, getDoctorAlertsApi, getDoctorGroupsApi,
  getMonitorOverviewApi, getPatientAlertsApi, getUnreadFeedbackCountApi,
  getUsersApi, listAlertRulesApi, listHealthDataApi,
  listNoticesApi, listVisibleNoticesApi, listOperationLogsPageApi, listRolesApi
} from '../api/modules'
import OverviewCards from './components/OverviewCards.vue'
import NoticeBoard from './components/NoticeBoard.vue'

const role = authStore.role
const loading = ref(false)
const roleLoading = ref(false)
const noticeLoading = ref(false)
const noticeList = ref([])
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

const doctorSummary = reactive({ totalAlerts: 0, highRisk: 0, groupCount: 0, unreadFeedback: 0 })
const patientSummary = reactive({ totalReports: 0, openAlerts: 0, unreadFeedback: 0 })

const cards = computed(() => {
  if (role === 'ADMIN') {
    return [
      { key: 'users', title: '账号管理', main: `账号总数 ${adminSummary.users.total}，启用 ${adminSummary.users.enabled}`, sub: `禁用 ${adminSummary.users.disabled}，用于用户生命周期管理`, path: '/admin/users' },
      { key: 'notices', title: '系统公告', main: `公告总数 ${adminSummary.notices.total}，发布中 ${adminSummary.notices.published}`, sub: `下线 ${adminSummary.notices.offline}，用于平台通知触达`, path: '/admin/notices' },
      { key: 'rules', title: '预警规则', main: `规则总数 ${adminSummary.rules.total}，启用 ${adminSummary.rules.enabled}`, sub: '用于健康风险分级判定与预警触发', path: '/admin/alert-rules' },
      { key: 'roles', title: '角色权限', main: `角色总数 ${adminSummary.roles.total}，已配置权限 ${adminSummary.roles.configured}`, sub: '用于访问控制和接口鉴权边界', path: '/admin/roles' },
      { key: 'monitor', title: '系统监控', main: `用户 ${adminSummary.monitor.totalUsers}，上报 ${adminSummary.monitor.totalHealthData}`, sub: `未处理预警 ${adminSummary.monitor.openAlerts}，用于运行态观测`, path: '/admin/monitor', highlight: true },
      { key: 'logs', title: '操作日志', main: `日志总数 ${adminSummary.logs.total}，失败操作 ${adminSummary.logs.failed}`, sub: '用于审计追踪与异常排查', path: '/admin/logs' },
      { key: 'feedback', title: '反馈消息', main: `反馈总数 ${adminSummary.feedback.totalCount}，待处理 ${adminSummary.feedback.pendingCount}`, sub: `今日新增 ${adminSummary.feedback.todayNewCount}，用于问题闭环处理`, path: '/admin/feedback' }
    ]
  }
  if (role === 'DOCTOR') {
    return [
      { key: 'doctor-alerts', title: '预警工作台', main: `待处理预警 ${doctorSummary.totalAlerts}`, sub: `高风险 ${doctorSummary.highRisk}，优先处理高分预警`, path: '/doctor/alerts', highlight: true },
      { key: 'doctor-groups', title: '群组管理', main: `群组数量 ${doctorSummary.groupCount}`, sub: '按群组组织随访和医生协作', path: '/doctor/groups' },
      { key: 'doctor-feedback', title: '反馈通道', main: `未读反馈 ${doctorSummary.unreadFeedback}`, sub: '跟进患者问题并记录处理结果', path: '/feedback' }
    ]
  }
  return [
    { key: 'patient-report', title: '健康上报', main: `累计上报 ${patientSummary.totalReports}`, sub: '持续记录是趋势分析的基础', path: '/patient/report', highlight: true },
    { key: 'patient-alerts', title: '预警详情', main: `未处理预警 ${patientSummary.openAlerts}`, sub: '查看风险变化并及时处理', path: '/patient/alerts' },
    { key: 'patient-feedback', title: '反馈通道', main: `未读反馈 ${patientSummary.unreadFeedback}`, sub: '查看医生建议并形成闭环', path: '/feedback' }
  ]
})

const roleSteps = computed(() => role === 'DOCTOR'
  ? ['先处理高风险与高分预警，避免风险积压。', '按群组分配随访任务，提升协作效率。', '通过反馈通道同步处理结果，形成闭环。']
  : ['固定时间完成今日健康上报。', '查看预警详情并按建议执行。', '通过反馈通道确认医生回复。']
)

const patientHints = computed(() => [
  patientSummary.openAlerts > 0 ? '优先处理预警详情，必要时联系医生。' : '当前无未处理预警，继续保持稳定上报。',
  patientSummary.unreadFeedback > 0 ? '你有新的反馈回复，建议及时查看。' : '反馈通道暂无未读消息。',
  '建议固定时间上报，趋势更准确。'
])

const patientMetrics = computed(() => ({
  totalReports: patientSummary.totalReports,
  openAlerts: patientSummary.openAlerts,
  unreadFeedback: patientSummary.unreadFeedback
}))

const loadAdminSummary = async () => {
  if (role !== 'ADMIN') return
  loading.value = true
  try {
    const [users, notices, rules, roles, monitor, logPage, logFailed, feedback] = await Promise.all([
      getUsersApi({}), listNoticesApi({ includeOffline: true }), listAlertRulesApi(), listRolesApi(),
      getMonitorOverviewApi(), listOperationLogsPageApi({ pageNo: 1, pageSize: 1 }),
      listOperationLogsPageApi({ pageNo: 1, pageSize: 1, success: 0 }), getAdminFeedbackStatsApi()
    ])
    const userList = Array.isArray(users) ? users : (Array.isArray(users?.records) ? users.records : [])
    const noticeListAll = Array.isArray(notices) ? notices : []
    const ruleList = Array.isArray(rules) ? rules : []
    const roleList = Array.isArray(roles) ? roles : []

    adminSummary.users.total = userList.length
    adminSummary.users.enabled = userList.filter(u => u.status === 1).length
    adminSummary.users.disabled = userList.filter(u => u.status !== 1).length
    adminSummary.notices.total = noticeListAll.length
    adminSummary.notices.published = noticeListAll.filter(n => n.status === 1).length
    adminSummary.notices.offline = noticeListAll.filter(n => n.status !== 1).length
    adminSummary.rules.total = ruleList.length
    adminSummary.rules.enabled = ruleList.filter(r => r.enabled === 1).length
    adminSummary.roles.total = roleList.length
    adminSummary.roles.configured = roleList.filter(r => String(r.permission || '').trim().length > 0).length
    adminSummary.monitor.totalUsers = Number(monitor?.totalUsers) || 0
    adminSummary.monitor.totalHealthData = Number(monitor?.totalHealthData) || 0
    adminSummary.monitor.openAlerts = Number(monitor?.openAlerts) || 0
    adminSummary.logs.total = Number(logPage?.total) || 0
    adminSummary.logs.failed = Number(logFailed?.total) || 0
    adminSummary.feedback.totalCount = Number(feedback?.totalCount) || 0
    adminSummary.feedback.pendingCount = Number(feedback?.pendingCount) || 0
    adminSummary.feedback.todayNewCount = Number(feedback?.todayNewCount) || 0
  } catch (err) {
    showError(err?.message || '加载管理概览失败，请稍后重试')
  } finally { loading.value = false }
}

const loadRoleSummary = async () => {
  if (role === 'ADMIN') return
  roleLoading.value = true
  try {
    if (role === 'DOCTOR') {
      const [alerts, groups, unreadFeedback] = await Promise.all([
        getDoctorAlertsApi({ pageNo: 1, pageSize: 200, sortBy: 'risk_desc', minRiskScore: 0 }),
        getDoctorGroupsApi(), getUnreadFeedbackCountApi()
      ])
      const alertList = Array.isArray(alerts?.list) ? alerts.list : []
      doctorSummary.totalAlerts = Number(alerts?.total) || alertList.length
      doctorSummary.highRisk = alertList.filter(a => a.riskLevel === 'HIGH').length
      doctorSummary.groupCount = Array.isArray(groups) ? groups.length : 0
      doctorSummary.unreadFeedback = Number(unreadFeedback) || 0
    } else {
      const [reportPage, openAlertPage, unreadFeedback] = await Promise.all([
        listHealthDataApi({ pageNo: 1, pageSize: 1 }),
        getPatientAlertsApi({ status: 'OPEN', pageNo: 1, pageSize: 1 }),
        getUnreadFeedbackCountApi()
      ])
      patientSummary.totalReports = Number(reportPage?.total) || 0
      patientSummary.openAlerts = Number(openAlertPage?.total) || 0
      patientSummary.unreadFeedback = Number(unreadFeedback) || 0
    }
  } catch (err) {
    showError(err?.message || '加载首页数据失败，请稍后重试')
  } finally { roleLoading.value = false }
}

const loadNotices = async () => {
  noticeLoading.value = true
  try {
    const list = await listVisibleNoticesApi()
    noticeList.value = Array.isArray(list) ? list.slice(0, 6) : []
  } finally { noticeLoading.value = false }
}

onMounted(() => {
  if (role === 'ADMIN') { loadAdminSummary(); loadNotices() }
  else { loadRoleSummary(); loadNotices() }
  timer = window.setInterval(() => {
    if (role === 'ADMIN') { loadAdminSummary(); loadNotices() }
    else { loadRoleSummary(); loadNotices() }
  }, 15000)
})

onUnmounted(() => {
  if (timer) { window.clearInterval(timer); timer = null }
})
</script>

<style scoped>
.role-home-v2 {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.68), rgba(255, 255, 255, 0.48)),
              radial-gradient(circle at 10% 14%, rgba(31, 143, 114, 0.12), transparent 32%);
  gap: 10px;
}
.section-divider {
  height: 1px; margin: 14px 2px 10px;
  background: linear-gradient(90deg, rgba(31, 143, 114, 0), rgba(31, 143, 114, 0.45), rgba(31, 143, 114, 0));
}
</style>
