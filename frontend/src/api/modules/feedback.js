import http from '../http'
import { authStore } from '../../stores/auth'

export const createFeedbackApi = (payload) => http.post('/feedback', payload)
export const listMyFeedbackPageApi = (params) => http.get('/feedback/mine/page', { params })
export const getUnreadFeedbackCountApi = () => http.get('/feedback/unread-count')
export const markMyFeedbackReadApi = () => http.post('/feedback/mark-read')
export const listAdminFeedbackPageApi = (params) => http.get('/admin/feedback/page', { params })
export const getAdminFeedbackStatsApi = () => http.get('/admin/feedback/stats')
export const getPendingFeedbackCountApi = () => http.get('/admin/feedback/pending-count')
export const updateFeedbackStatusApi = (id, status) => http.patch(`/admin/feedback/${id}/status?status=${status}`)
export const batchUpdateFeedbackStatusApi = (payload) => http.patch('/admin/feedback/batch-status', payload)
export const batchUpdateFeedbackStatusByFilterApi = (params) => http.patch('/admin/feedback/batch-status-by-filter', null, { params })
export const replyFeedbackApi = (payload) => http.put('/admin/feedback/reply', payload)

export const exportFeedbackApi = async (params) => {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      searchParams.append(key, String(value))
    }
  })
  const url = `/api/admin/feedback/export${searchParams.toString() ? `?${searchParams.toString()}` : ''}`
  const res = await fetch(url, {
    method: 'GET',
    headers: { Authorization: `Bearer ${authStore.token}` }
  })
  if (!res.ok) {
    throw new Error('导出失败')
  }
  return await res.blob()
}
