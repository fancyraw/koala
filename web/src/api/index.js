import { http } from '@/utils/request'

export const api = {
  // 认证
  login: (code) => http.post('/user/login', { code }, { withToken: false }),

  // 首页 / 商品 / 分类
  home: () => http.get('/home'),
  products: (params) => http.get('/products', params),
  productDetail: (id) => http.get('/products/detail', { id }),
  categories: () => http.get('/categories'),

  // 购物车
  cart: () => http.get('/cart'),
  cartAdd: (skuId, quantity) => http.post('/cart/add', { skuId, quantity }),
  cartUpdate: (payload) => http.post('/cart/update', payload),
  cartRemove: (ids) => http.post('/cart/remove', { ids }),

  // 优惠券
  couponAutoGrant: () => http.post('/coupons/auto-grant'),
  couponMine: (status) => http.get('/coupons/mine', status != null ? { status } : {}),

  // 地址
  addresses: () => http.get('/addresses'),
  addressDetail: (id) => http.get('/addresses/detail', { id }),
  addressAdd: (payload) => http.post('/addresses/add', payload),
  addressUpdate: (payload) => http.post('/addresses/update', payload),
  addressDelete: (id) => http.post('/addresses/delete', { id }),
  regions: (parent) => http.get('/regions', parent != null ? { parent } : {}),

  // 订单
  orderPreview: (payload) => http.post('/order/preview', payload),
  orderSubmit: (payload) => http.post('/order/submit', payload),
  orderPay: (no) => http.post('/order/pay', { no }),
  orders: (params) => http.get('/orders', params),
  orderDetail: (no) => http.get('/orders/detail', { no }),
  orderCancel: (no) => http.post('/orders/cancel', { no }),
  orderConfirm: (no) => http.post('/orders/confirm', { no }),
  orderDelete: (no) => http.post('/orders/delete', { no }),

  // 售后
  afterSaleApply: (payload) => http.post('/after-sales/apply', payload),
  afterSaleList: (params) => http.get('/after-sales', params),
  afterSaleDetail: (afterSaleNo) => http.get('/after-sales/detail', { afterSaleNo }),
  afterSaleCancel: (afterSaleNo) => http.post('/after-sales/cancel', { afterSaleNo }),
  afterSaleTracking: (afterSaleNo, returnTrackingNo) =>
    http.post('/after-sales/tracking', { afterSaleNo, returnTrackingNo }),

  // 图片上传（签发直传凭证，前端直传对象存储）
  uploadCredential: () => http.get('/upload/credential'),
}
