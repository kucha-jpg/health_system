import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authStore } from '../stores/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

const showError = (message) => {
  ElMessage.error({
    message,
    duration: 5000,
    showClose: true
  })
}

http.interceptors.request.use((config) => {
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

http.interceptors.response.use(
  (res) => {
    const { code, msg, data } = res.data
    if (code !== 200) {
      if (!res.config?.__skipErrorToast) {
        showError(msg || '请求失败')
      }
      return Promise.reject(new Error(msg))
    }
    return data
  },
  (error) => {
    const msg = error.response?.data?.msg || '网络异常'
    if (error.response?.status === 401) {
      if (error.config?.__skipAuthRedirect) {
        if (!error.config?.__skipErrorToast) {
          showError(msg)
        }
        return Promise.reject(error)
      }
      authStore.setAuthNotice(msg)
      authStore.clear()
      if (window.location.pathname !== '/login') {
        window.location.href = `/login?notice=${encodeURIComponent(msg)}`
      } else {
        showError(msg)
      }
      return Promise.reject(error)
    }
    if (!error.config?.__skipErrorToast) {
      showError(msg)
    }
    return Promise.reject(error)
  }
)

window.addEventListener('auth:kicked', (event) => {
  const reason = event?.detail?.reason || '在其他地方登录'
  authStore.setAuthNotice(reason)
  authStore.clear()
  if (window.location.pathname !== '/login') {
    window.location.href = `/login?notice=${encodeURIComponent(reason)}`
  } else {
    showError(reason)
  }
})

export default http
