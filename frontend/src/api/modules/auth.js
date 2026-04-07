import http from '../http'

export const loginApi = (payload) => http.post('/auth/login', payload)
export const registerApi = (payload) => http.post('/auth/register', payload)
export const validateSessionApi = () => http.get('/auth/ping', { __skipAuthRedirect: true, __skipErrorToast: true })
