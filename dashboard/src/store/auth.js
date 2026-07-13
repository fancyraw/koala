import { defineStore } from 'pinia'
import { api } from '@/api'
import { TOKEN_KEY, ADMIN_KEY } from '@/config'

// 解析 JWT payload 读取 isSuper（后端把 isSuper 放在 JWT claim，未随 LoginResponse 返回）
function decodeSuper(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    return !!payload.isSuper
  } catch (e) {
    return false
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: JSON.parse(localStorage.getItem(ADMIN_KEY) || 'null'),
    isSuper: false,
  }),
  getters: {
    isLogin: (s) => !!s.token,
    nickname: (s) => s.profile?.nickname || '管理员',
    avatarUrl: (s) => s.profile?.avatarUrl || '',
  },
  actions: {
    restore() {
      if (this.token) this.isSuper = decodeSuper(this.token)
    },
    setLogin(login) {
      this.token = login.token
      this.profile = {
        id: login.id,
        nickname: login.nickname,
        avatarUrl: login.avatarUrl,
      }
      this.isSuper = decodeSuper(login.token)
      localStorage.setItem(TOKEN_KEY, login.token)
      localStorage.setItem(ADMIN_KEY, JSON.stringify(this.profile))
    },
    async logout() {
      try {
        await api.logout()
      } catch (e) {
        /* 忽略登出接口失败，仍清本地 */
      }
      this.token = ''
      this.profile = null
      this.isSuper = false
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(ADMIN_KEY)
    },
  },
})
