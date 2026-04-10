import http from '../http'

export const listNoticesApi = (params) => http.get('/admin/config/notices', { params })
export const listVisibleNoticesApi = () => http.get('/notices')
export const createNoticeApi = (payload) => http.post('/admin/config/notices', payload)
export const updateNoticeApi = (payload) => http.put('/admin/config/notices', payload)
export const deleteNoticeApi = (id) => http.delete(`/admin/config/notices/${id}`)

export const listAlertRulesApi = () => http.get('/admin/config/alert-rules')
export const createAlertRuleApi = (payload) => http.post('/admin/config/alert-rules', payload)
export const updateAlertRuleApi = (payload) => http.put('/admin/config/alert-rules', payload)

export const listIndicatorTypesApi = (params) => http.get('/admin/config/indicator-types', { params })
export const createIndicatorTypeApi = (payload) => http.post('/admin/config/indicator-types', payload)
export const updateIndicatorTypeApi = (payload) => http.put('/admin/config/indicator-types', payload)
export const deleteIndicatorTypeApi = (id) => http.delete(`/admin/config/indicator-types/${id}`)
