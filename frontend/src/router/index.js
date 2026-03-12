import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import MainLayout from '../views/MainLayout.vue'
import UserView from '../views/UserView.vue'
import RoleHomeView from '../views/RoleHomeView.vue'
import PatientArchiveView from '../views/PatientArchiveView.vue'
import PatientDataReportView from '../views/PatientDataReportView.vue'
import PatientDataListView from '../views/PatientDataListView.vue'
import DoctorAlertsView from '../views/DoctorAlertsView.vue'
import AdminMonitorView from '../views/AdminMonitorView.vue'
import DoctorGroupView from '../views/DoctorGroupView.vue'
import PatientAlertsView from '../views/PatientAlertsView.vue'
import PatientReportSummaryView from '../views/PatientReportSummaryView.vue'
import AdminNoticeView from '../views/AdminNoticeView.vue'
import AdminAlertRuleView from '../views/AdminAlertRuleView.vue'
import AdminLogView from '../views/AdminLogView.vue'
import AdminRoleView from '../views/AdminRoleView.vue'
import DoctorPatientInsightView from '../views/DoctorPatientInsightView.vue'
import FeedbackView from '../views/FeedbackView.vue'
import AdminFeedbackView from '../views/AdminFeedbackView.vue'
import { validateSessionApi } from '../api/modules'

const routes = [
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'home', component: RoleHomeView, meta: { roles: ['PATIENT', 'DOCTOR', 'ADMIN'] } },
      { path: 'admin/users', component: UserView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/monitor', component: AdminMonitorView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/notices', component: AdminNoticeView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/alert-rules', component: AdminAlertRuleView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/roles', component: AdminRoleView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/logs', component: AdminLogView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/feedback', component: AdminFeedbackView, meta: { roles: ['ADMIN'] } },
      { path: 'doctor/alerts', component: DoctorAlertsView, meta: { roles: ['DOCTOR'] } },
      { path: 'doctor/groups', component: DoctorGroupView, meta: { roles: ['DOCTOR'] } },
      { path: 'doctor/patients/:patientUserId', component: DoctorPatientInsightView, meta: { roles: ['DOCTOR'] } },
      { path: 'feedback', component: FeedbackView, meta: { roles: ['PATIENT', 'DOCTOR'] } },
      { path: 'patient/archive', component: PatientArchiveView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/report', component: PatientDataReportView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/data', component: PatientDataListView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/alerts', component: PatientAlertsView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/summary', component: PatientReportSummaryView, meta: { roles: ['PATIENT'] } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(async (to, from, next) => {
  if (['/login', '/register'].includes(to.path)) return next()
  if (!authStore.token) return next('/login')

  try {
    await validateSessionApi()
  } catch (e) {
    const msg = e?.response?.data?.msg || e?.message || '未登录或登录已过期'
    authStore.setAuthNotice(msg)
    authStore.clear()
    return next({ path: '/login', query: { notice: msg } })
  }

  const role = authStore.role
  if (to.meta.roles && !to.meta.roles.includes(role)) return next('/home')
  next()
})

export default router
