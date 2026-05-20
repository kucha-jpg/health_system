import http from '../http'
import { authStore } from '../../stores/auth'

export const getUsersApi = (params) => http.get('/admin/user', { params })
export const addUserApi = (payload) => http.post('/admin/user', payload)
export const updateUserApi = (payload) => http.put('/admin/user', payload)
export const updateUserStatusApi = (id, status) => http.patch(`/admin/user/${id}/status?status=${status}`)
export const deleteUserApi = (id) => http.delete(`/admin/user/${id}`)
export const getMonitorOverviewApi = () => http.get('/admin/monitor/overview')

export const listOperationLogsPageApi = (params) => http.get('/admin/logs/page', { params })
export const listRolesApi = () => http.get('/admin/roles')
export const updateRolePermissionApi = (payload) => http.put('/admin/roles', payload)

export const listAdminGroupsApi = (params) => http.get('/admin/groups', { params })
export const getAdminGroupStatsApi = () => http.get('/admin/groups/stats')
export const approveGroupApi = (id) => http.patch(`/admin/groups/${id}/approve`)
export const archiveGroupApi = (id) => http.patch(`/admin/groups/${id}/archive`)
export const crossDeptGroupApi = (id, payload) => http.patch(`/admin/groups/${id}/cross-dept`, payload)
export const batchApproveGroupsApi = (payload) => http.post('/admin/groups/batch-approve', payload)
export const batchArchiveGroupsApi = (payload) => http.post('/admin/groups/batch-archive', payload)
export const batchCrossDeptGroupsApi = (payload) => http.post('/admin/groups/batch-cross-dept', payload)
export const deleteGroupApi = (id) => http.delete(`/admin/groups/${id}`)

export const exportOperationLogsApi = async (params) => {
  const searchParams = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== '') {
      searchParams.append(key, String(value))
    }
  })
  const url = `/api/admin/logs/export?${searchParams.toString()}`
  const res = await fetch(url, {
    method: 'GET',
    headers: { Authorization: `Bearer ${authStore.token}` }
  })
  if (!res.ok) {
    throw new Error('导出失败')
  }
  const blob = await res.blob()
  return {
    blob,
    requestedLimit: Number(res.headers.get('X-Requested-Limit') || 0),
    effectiveLimit: Number(res.headers.get('X-Effective-Limit') || 0)
  }
}
