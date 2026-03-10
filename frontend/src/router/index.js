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
import AdminLogView from '../views/AdminLogView.vue'
import AdminRoleView from '../views/AdminRoleView.vue'

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
      { path: 'admin/roles', component: AdminRoleView, meta: { roles: ['ADMIN'] } },
      { path: 'admin/logs', component: AdminLogView, meta: { roles: ['ADMIN'] } },
      { path: 'doctor/alerts', component: DoctorAlertsView, meta: { roles: ['DOCTOR'] } },
      { path: 'doctor/groups', component: DoctorGroupView, meta: { roles: ['DOCTOR'] } },
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

router.beforeEach((to, from, next) => {
  if (['/login', '/register'].includes(to.path)) return next()
  if (!authStore.token) return next('/login')
  const role = authStore.role
  if (to.meta.roles && !to.meta.roles.includes(role)) return next('/home')
  next()
})

export default router
