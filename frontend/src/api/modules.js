import http from './http'

export const loginApi = (payload) => http.post('/auth/login', payload)
export const registerApi = (payload) => http.post('/auth/register', payload)

export const getUsersApi = () => http.get('/admin/user')
export const addUserApi = (payload) => http.post('/admin/user', payload)
export const updateUserApi = (payload) => http.put('/admin/user', payload)
export const updateUserStatusApi = (id, status) => http.patch(`/admin/user/${id}/status?status=${status}`)

export const getArchiveApi = () => http.get('/patient/archive')
export const saveArchiveApi = (payload) => http.post('/patient/archive', payload)

export const reportHealthDataApi = (payload) => http.post('/patient/data', payload)
export const listHealthDataApi = (params) => http.get('/patient/data', { params })
export const updateHealthDataApi = (id, payload) => http.put(`/patient/data/${id}`, payload)
export const deleteHealthDataApi = (id) => http.delete(`/patient/data/${id}`)
