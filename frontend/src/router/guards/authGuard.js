import { authStore } from '../../stores/auth'
import { validateSessionApi } from '../../api/modules'

export const registerAuthGuard = (router) => {
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
}
