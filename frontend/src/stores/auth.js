export const authStore = {
  get token() {
    return localStorage.getItem('token') || ''
  },
  get role() {
    return localStorage.getItem('roleType') || ''
  },
  get name() {
    return localStorage.getItem('name') || ''
  },
  setAuth(payload) {
    localStorage.setItem('token', payload.token)
    localStorage.setItem('roleType', payload.userInfo.roleType)
    localStorage.setItem('name', payload.userInfo.name)
  },
  clear() {
    localStorage.clear()
  }
}
