import { defineStore } from 'pinia'
import { api } from '@/api'

export const useCartStore = defineStore('cart', {
  state: () => ({
    totalCount: 0, // Tab 角标数量
    view: null, // 最近一次拉到的完整 CartView（购物车页用）
  }),
  actions: {
    apply(view) {
      this.view = view
      this.totalCount = view?.totalCount || 0
      this.syncBadge()
    },

    async refresh() {
      const view = await api.cart()
      this.apply(view)
      return view
    },

    // 轻量刷新，只为更新角标
    async refreshBadge() {
      try {
        const view = await api.cart()
        this.apply(view)
      } catch (e) {
        /* 静默 */
      }
    },

    async add(skuId, quantity = 1) {
      const view = await api.cartAdd(skuId, quantity)
      this.apply(view)
      return view
    },

    syncBadge() {
      const idx = 2 // 购物车 Tab 索引
      if (this.totalCount > 0) {
        const text = this.totalCount > 99 ? '99+' : String(this.totalCount)
        uni.setTabBarBadge({ index: idx, text })
      } else {
        uni.removeTabBarBadge({ index: idx })
      }
    },

    clearBadge() {
      this.totalCount = 0
      uni.removeTabBarBadge({ index: 2 })
    },
  },
})
