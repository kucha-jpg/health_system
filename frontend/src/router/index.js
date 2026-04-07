import { createRouter, createWebHistory } from 'vue-router'
import { authRoutes } from './routes/authRoutes'
import { appRoutes } from './routes/appRoutes'
import { registerAuthGuard } from './guards/authGuard'

const routes = [...authRoutes, ...appRoutes]

const router = createRouter({ history: createWebHistory(), routes })
registerAuthGuard(router)

export default router
