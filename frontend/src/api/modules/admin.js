import http from '../http'

export const getUsersApi = (params) => http.get('/admin/user', { params })
export const addUserApi = (payload) => http.post('/admin/user', payload)
export const updateUserApi = (payload) => http.put('/admin/user', payload)
export const updateUserStatusApi = (id, status) => http.patch(`/admin/user/${id}/status?status=${status}`)
export const getMonitorOverviewApi = () => http.get('/admin/monitor/overview')

export const listOperationLogsPageApi = (params) => http.get('/admin/logs/page', { params })
export const listRolesApi = () => http.get('/admin/roles')
export const updateRolePermissionApi = (payload) => http.put('/admin/roles', payload)
