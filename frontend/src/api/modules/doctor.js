import http from '../http'

export const getDoctorAlertsApi = (params) => http.get('/doctor/alerts', { params })
export const handleDoctorAlertApi = (id, payload) => http.post(`/doctor/alerts/${id}/handle`, payload)
export const getDoctorGroupsApi = () => http.get('/doctor/groups')
export const createDoctorGroupApi = (payload) => http.post('/doctor/groups', payload)
export const addDoctorGroupPatientApi = (id, payload) => http.post(`/doctor/groups/${id}/patients`, payload)
export const listDoctorGroupPatientsApi = (id) => http.get(`/doctor/groups/${id}/patients`)
export const addDoctorGroupDoctorApi = (id, payload) => http.post(`/doctor/groups/${id}/doctors`, payload)
export const listDoctorGroupDoctorsApi = (id) => http.get(`/doctor/groups/${id}/doctors`)
export const getDoctorPatientInsightApi = (patientUserId, params) => http.get(`/doctor/patients/${patientUserId}/insight`, { params })
