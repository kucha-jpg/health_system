const LoginView = () => import('../../views/LoginView.vue')
const RegisterView = () => import('../../views/RegisterView.vue')

export const authRoutes = [
  { path: '/login', component: LoginView },
  { path: '/register', component: RegisterView }
]
