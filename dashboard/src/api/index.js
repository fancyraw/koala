import { http } from '@/utils/request'

export const api = {
  // 登录（扫码）
  loginQrcode: () => http.get('/admin/login/qrcode'),
  loginCheck: (state) => http.post('/admin/login/check', null, { params: { state } }),
  logout: () => http.post('/admin/login/logout'),

  // 数据看板
  dashboard: (range) => http.get('/admin/dashboard', { range }),

  // 商品
  products: (params) => http.get('/admin/products', params),
  productDetail: (id) => http.get('/admin/products/detail', { id }),
  productSave: (payload) => http.post('/admin/products/save', payload),
  productDelete: (id) => http.post('/admin/products/delete', { id }),
  productStatus: (id, valid) => http.post('/admin/products/status', { id, valid }),

  // 分类
  categories: () => http.get('/admin/categories'),
  categorySave: (payload) => http.post('/admin/categories/save', payload),
  categoryDelete: (id) => http.post('/admin/categories/delete', { id }),
  categorySort: (items) => http.post('/admin/categories/sort', { items }),

  // 标签
  tags: () => http.get('/admin/tags'),
  tagSave: (payload) => http.post('/admin/tags/save', payload),
  tagDelete: (id) => http.post('/admin/tags/delete', { id }),
  tagSort: (items) => http.post('/admin/tags/sort', { items }),

  // 订单
  orders: (params) => http.get('/admin/orders', params),
  orderDetail: (no) => http.get('/admin/orders/detail', { no }),
  orderShip: (payload) => http.post('/admin/orders/ship', payload),
  orderRefund: (payload) => http.post('/admin/orders/refund', payload),

  // 售后
  afterSales: (params) => http.get('/admin/after-sales', params),
  afterSaleDetail: (afterSaleNo) => http.get('/admin/after-sales/detail', { afterSaleNo }),
  afterSaleAudit: (payload) => http.post('/admin/after-sales/audit', payload),
  afterSaleConfirmReceive: (afterSaleNo) =>
    http.post('/admin/after-sales/confirm-receive', { afterSaleNo }),

  // 用户
  users: (params) => http.get('/admin/users', params),
  userDetail: (id, params) => http.get('/admin/users/detail', { id, ...params }),
  userStatus: (id, valid) => http.post('/admin/users/status', { id, valid }),

  // 优惠券
  coupons: (params) => http.get('/admin/coupons', params),
  couponDetail: (id) => http.get('/admin/coupons/detail', { id }),
  couponSave: (payload) => http.post('/admin/coupons/save', payload),
  couponStop: (id) => http.post('/admin/coupons/stop', { id }),
  couponDelete: (id) => http.post('/admin/coupons/delete', { id }),
  couponGrants: (params) => http.get('/admin/coupons/grants', params),

  // Banner
  banners: () => http.get('/admin/banners'),
  bannerSave: (payload) => http.post('/admin/banners/save', payload),
  bannerDelete: (id) => http.post('/admin/banners/delete', { id }),
  bannerSort: (items) => http.post('/admin/banners/sort', { items }),

  // 系统配置
  config: (group) => http.get('/admin/config', { group }),
  configSave: (payload) => http.post('/admin/config/save', payload),

  // 管理员管理（仅超管）
  admins: () => http.get('/admin/admins'),
  adminInvite: () => http.post('/admin/admins/invite'),
  adminStatus: (id, valid) => http.post('/admin/admins/status', { id, valid }),

  // 上传凭证
  uploadCredential: () => http.get('/admin/upload/credential'),
}
