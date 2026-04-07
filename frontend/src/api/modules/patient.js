import http from '../http'

export const getArchiveApi = () => http.get('/patient/archive')
export const saveArchiveApi = (payload) => http.post('/patient/archive', payload)

export const reportHealthDataApi = (payload) => http.post('/patient/data', payload)
export const listHealthDataApi = (params) => http.get('/patient/data', { params })
export const updateHealthDataApi = (id, payload) => http.put(`/patient/data/${id}`, payload)
export const deleteHealthDataApi = (id) => http.delete(`/patient/data/${id}`)

export const getPatientAlertsApi = (params) => http.get('/patient/alerts', { params })
export const getPatientReportSummaryApi = (params) => http.get('/patient/reports/summary', { params })
export const listPatientAlertPreferencesApi = () => http.get('/patient/alert-preferences')
export const updatePatientAlertPreferenceApi = (payload) => http.put('/patient/alert-preferences', payload)
