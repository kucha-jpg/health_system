import axios from 'axios'
import { ElMessage } from 'element-plus'
import { authStore } from '../stores/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 10000
})

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
      ElMessage.error(msg || '请求失败')
      return Promise.reject(new Error(msg))
    }
    return data
  },
  (error) => {
    ElMessage.error(error.response?.data?.msg || '网络异常')
    return Promise.reject(error)
  }
)

export default http
