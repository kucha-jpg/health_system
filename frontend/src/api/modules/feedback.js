import http from '../http'

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
