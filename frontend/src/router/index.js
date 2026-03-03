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

const routes = [
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'home', component: RoleHomeView, meta: { roles: ['PATIENT', 'DOCTOR', 'ADMIN'] } },
      { path: 'admin/users', component: UserView, meta: { roles: ['ADMIN'] } },
      { path: 'patient/archive', component: PatientArchiveView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/report', component: PatientDataReportView, meta: { roles: ['PATIENT'] } },
      { path: 'patient/data', component: PatientDataListView, meta: { roles: ['PATIENT'] } }
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
