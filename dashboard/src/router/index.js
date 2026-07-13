import { createRouter, createWebHistory } from 'vue-router'
import { TOKEN_KEY } from '@/config'

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('@/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '数据看板' } },
      { path: 'products', name: 'products', component: () => import('@/views/Products.vue'), meta: { title: '商品管理' } },
      { path: 'orders', name: 'orders', component: () => import('@/views/Orders.vue'), meta: { title: '订单管理' } },
      { path: 'users', name: 'users', component: () => import('@/views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'coupons', name: 'coupons', component: () => import('@/views/Coupons.vue'), meta: { title: '优惠券管理' } },
      { path: 'banners', name: 'banners', component: () => import('@/views/Banners.vue'), meta: { title: '内容管理' } },
      { path: 'settings', name: 'settings', component: () => import('@/views/Settings.vue'), meta: { title: '系统设置' } },
      { path: 'admins', name: 'admins', component: () => import('@/views/Admins.vue'), meta: { title: '管理员管理', super: true } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory('/koala/'),
  routes,
})

router.beforeEach((to) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (to.meta.public) return true
  if (!token) return { path: '/login' }
  return true
})

export default router
