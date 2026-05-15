import http from '../http'

export const loginApi = (payload, config) => http.post('/auth/login', payload, config)
export const registerApi = (payload, config) => http.post('/auth/register', payload, config)
export const validateSessionApi = () => http.get('/auth/ping', { __skipAuthRedirect: true, __skipErrorToast: true })
