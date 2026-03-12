const TOKEN_KEY = 'token'
const ROLE_KEY = 'roleType'
const NAME_KEY = 'name'
const USERNAME_KEY = 'username'
const TAB_ID_KEY = 'tabId'
const AUTH_NOTICE_KEY = 'authNotice'

const tabId = sessionStorage.getItem(TAB_ID_KEY) || `${Date.now()}_${Math.random().toString(36).slice(2)}`
sessionStorage.setItem(TAB_ID_KEY, tabId)

const authChannel = ('BroadcastChannel' in window) ? new BroadcastChannel('health-system-auth') : null

export const authStore = {
  get token() {
    return sessionStorage.getItem(TOKEN_KEY) || ''
  },
  get role() {
    return sessionStorage.getItem(ROLE_KEY) || ''
  },
  get name() {
    return sessionStorage.getItem(NAME_KEY) || ''
  },
  get username() {
    return sessionStorage.getItem(USERNAME_KEY) || ''
  },
  setAuth(payload) {
    sessionStorage.setItem(TOKEN_KEY, payload.token)
    sessionStorage.setItem(ROLE_KEY, payload.userInfo.roleType)
    sessionStorage.setItem(NAME_KEY, payload.userInfo.name)
    sessionStorage.setItem(USERNAME_KEY, payload.userInfo.username)

    if (authChannel) {
      authChannel.postMessage({
        type: 'LOGIN',
        username: payload.userInfo.username,
        tabId
      })
    }
  },
  clear() {
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(ROLE_KEY)
    sessionStorage.removeItem(NAME_KEY)
    sessionStorage.removeItem(USERNAME_KEY)
  },
  setAuthNotice(message) {
    if (!message) return
    sessionStorage.setItem(AUTH_NOTICE_KEY, message)
  },
  consumeAuthNotice() {
    const message = sessionStorage.getItem(AUTH_NOTICE_KEY) || ''
    if (message) {
      sessionStorage.removeItem(AUTH_NOTICE_KEY)
    }
    return message
  }
}

if (authChannel) {
  authChannel.onmessage = (event) => {
    const data = event.data || {}
    if (data.type !== 'LOGIN' || data.tabId === tabId) return
    if (!authStore.username || authStore.username !== data.username) return

    authStore.clear()
    window.dispatchEvent(new CustomEvent('auth:kicked', { detail: { reason: '在其他地方登录' } }))
  }
}
