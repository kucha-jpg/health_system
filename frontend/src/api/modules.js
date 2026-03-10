import http from './http'

export const loginApi = (payload) => http.post('/auth/login', payload)
export const registerApi = (payload) => http.post('/auth/register', payload)

export const getUsersApi = (params) => http.get('/admin/user', { params })
export const addUserApi = (payload) => http.post('/admin/user', payload)
export const updateUserApi = (payload) => http.put('/admin/user', payload)
export const updateUserStatusApi = (id, status) => http.patch(`/admin/user/${id}/status?status=${status}`)
export const getMonitorOverviewApi = () => http.get('/admin/monitor/overview')

export const getArchiveApi = () => http.get('/patient/archive')
export const saveArchiveApi = (payload) => http.post('/patient/archive', payload)

export const reportHealthDataApi = (payload) => http.post('/patient/data', payload)
export const listHealthDataApi = (params) => http.get('/patient/data', { params })
export const updateHealthDataApi = (id, payload) => http.put(`/patient/data/${id}`, payload)
export const deleteHealthDataApi = (id) => http.delete(`/patient/data/${id}`)

export const getDoctorAlertsApi = () => http.get('/doctor/alerts')
export const handleDoctorAlertApi = (id, payload) => http.post(`/doctor/alerts/${id}/handle`, payload)
export const getDoctorGroupsApi = () => http.get('/doctor/groups')
export const createDoctorGroupApi = (payload) => http.post('/doctor/groups', payload)
export const addDoctorGroupPatientApi = (id, payload) => http.post(`/doctor/groups/${id}/patients`, payload)
export const listDoctorGroupPatientsApi = (id) => http.get(`/doctor/groups/${id}/patients`)

export const getPatientAlertsApi = (params) => http.get('/patient/alerts', { params })
export const getPatientReportSummaryApi = (params) => http.get('/patient/reports/summary', { params })

export const listNoticesApi = (params) => http.get('/admin/config/notices', { params })
export const createNoticeApi = (payload) => http.post('/admin/config/notices', payload)
export const updateNoticeApi = (payload) => http.put('/admin/config/notices', payload)
export const deleteNoticeApi = (id) => http.delete(`/admin/config/notices/${id}`)
export const listOperationLogsApi = (params) => http.get('/admin/logs', { params })
export const listRolesApi = () => http.get('/admin/roles')
export const updateRolePermissionApi = (payload) => http.put('/admin/roles', payload)
