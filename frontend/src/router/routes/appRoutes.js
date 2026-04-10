const MainLayout = () => import('../../views/MainLayout.vue')
const UserView = () => import('../../views/UserView.vue')
const RoleHomeView = () => import('../../views/RoleHomeView.vue')
const PatientArchiveView = () => import('../../views/PatientArchiveView.vue')
const PatientDataReportView = () => import('../../views/PatientDataReportView.vue')
const PatientDataListView = () => import('../../views/PatientDataListView.vue')
const DoctorAlertsView = () => import('../../views/DoctorAlertsView.vue')
const AdminMonitorView = () => import('../../views/AdminMonitorView.vue')
const DoctorGroupView = () => import('../../views/DoctorGroupView.vue')
const PatientAlertsView = () => import('../../views/PatientAlertsView.vue')
const PatientAlertPreferenceView = () => import('../../views/PatientAlertPreferenceView.vue')
const PatientReportSummaryView = () => import('../../views/PatientReportSummaryView.vue')
const AdminNoticeView = () => import('../../views/AdminNoticeView.vue')
const AdminAlertRuleView = () => import('../../views/AdminAlertRuleView.vue')
const AdminLogView = () => import('../../views/AdminLogView.vue')
const AdminRoleView = () => import('../../views/AdminRoleView.vue')
const DoctorPatientInsightView = () => import('../../views/DoctorPatientInsightView.vue')
const FeedbackView = () => import('../../views/FeedbackView.vue')
const AdminFeedbackView = () => import('../../views/AdminFeedbackView.vue')

export const appRoutes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'home', component: RoleHomeView, meta: { roles: ['PATIENT', 'DOCTOR', 'ADMIN'], title: '工作首页' } },
      { path: 'admin/users', component: UserView, meta: { roles: ['ADMIN'], title: '账号管理' } },
      { path: 'admin/monitor', component: AdminMonitorView, meta: { roles: ['ADMIN'], title: '系统监控' } },
      { path: 'admin/notices', component: AdminNoticeView, meta: { roles: ['ADMIN'], title: '系统公告' } },
      { path: 'admin/alert-rules', component: AdminAlertRuleView, meta: { roles: ['ADMIN'], title: '预警规则' } },
      { path: 'admin/roles', component: AdminRoleView, meta: { roles: ['ADMIN'], title: '角色权限' } },
      { path: 'admin/logs', component: AdminLogView, meta: { roles: ['ADMIN'], title: '操作日志' } },
      { path: 'admin/feedback', component: AdminFeedbackView, meta: { roles: ['ADMIN'], title: '反馈消息' } },
      { path: 'doctor/alerts', component: DoctorAlertsView, meta: { roles: ['DOCTOR'], title: '医生预警工作台' } },
      { path: 'doctor/groups', component: DoctorGroupView, meta: { roles: ['DOCTOR'], title: '群组管理' } },
      { path: 'doctor/patients/:patientUserId', component: DoctorPatientInsightView, meta: { roles: ['DOCTOR'], title: '患者洞察' } },
      { path: 'feedback', component: FeedbackView, meta: { roles: ['PATIENT', 'DOCTOR'], title: '反馈通道' } },
      { path: 'patient/archive', component: PatientArchiveView, meta: { roles: ['PATIENT'], title: '个人档案' } },
      { path: 'patient/report', component: PatientDataReportView, meta: { roles: ['PATIENT'], title: '健康上报' } },
      { path: 'patient/data', component: PatientDataListView, meta: { roles: ['PATIENT'], title: '历史数据' } },
      { path: 'patient/alerts', component: PatientAlertsView, meta: { roles: ['PATIENT'], title: '预警详情' } },
      { path: 'patient/alert-preferences', component: PatientAlertPreferenceView, meta: { roles: ['PATIENT'], title: '个性化阈值' } },
      { path: 'patient/summary', component: PatientReportSummaryView, meta: { roles: ['PATIENT'], title: '周报月报' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]
