export const AFTERSALE_STATUS = {
  0: { text: '待审核', color: '#ffb020' },
  1: { text: '通过待寄回', color: '#ff2442' },
  2: { text: '买家已寄回', color: '#1a1a1a' },
  3: { text: '商家已收货', color: '#1a1a1a' },
  4: { text: '已退款', color: '#999999' },
  5: { text: '已拒绝', color: '#999999' },
}

export function afterSaleStatusText(s) {
  return AFTERSALE_STATUS[s]?.text || '未知'
}
export function afterSaleStatusColor(s) {
  return AFTERSALE_STATUS[s]?.color || '#999999'
}

// 退货退款(type=2)在「通过待寄回」「买家已寄回」阶段可填/改寄回单号
export function canFillTracking(view) {
  return view && view.type === 2 && (view.status === 1 || view.status === 2)
}
