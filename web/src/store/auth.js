import { defineStore } from 'pinia'
import { api } from '@/api'
import { TOKEN_KEY } from '@/config'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    userId: null,
    nickname: '',
    avatarUrl: '',
  }),
  getters: {
    isLogin: (s) => !!s.token,
  },
  actions: {
    restore() {
      this.token = uni.getStorageSync(TOKEN_KEY) || ''
      const profile = uni.getStorageSync('koala_profile')
      if (profile) {
        this.userId = profile.id
        this.nickname = profile.nickname
        this.avatarUrl = profile.avatarUrl
      }
    },

    // 微信登录：wx.login 取 code → 换后端 token（mock 实现，任意 code 可通）
    async login() {
      const code = await this.wxCode()
      const res = await api.login(code)
      this.token = res.token
      this.userId = res.id
      this.nickname = res.nickname
      this.avatarUrl = res.avatarUrl
      uni.setStorageSync(TOKEN_KEY, res.token)
      uni.setStorageSync('koala_profile', {
        id: res.id,
        nickname: res.nickname,
        avatarUrl: res.avatarUrl,
      })
      return res
    },

    wxCode() {
      return new Promise((resolve, reject) => {
        // #ifdef MP-WEIXIN
        uni.login({
          provider: 'weixin',
          success: (r) => resolve(r.code),
          fail: reject,
        })
        // #endif
        // #ifndef MP-WEIXIN
        resolve('h5_dev_' + Date.now())
        // #endif
      })
    },

    logout() {
      this.token = ''
      this.userId = null
      this.nickname = ''
      this.avatarUrl = ''
      uni.removeStorageSync(TOKEN_KEY)
      uni.removeStorageSync('koala_profile')
    },
  },
})
