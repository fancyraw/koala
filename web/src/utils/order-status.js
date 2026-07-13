export const ORDER_STATUS = {
  0: { text: '待付款', color: '#ff2442' },
  1: { text: '待发货', color: '#1a1a1a' },
  2: { text: '待收货', color: '#1a1a1a' },
  3: { text: '已完成', color: '#999999' },
  4: { text: '已取消', color: '#999999' },
  5: { text: '售后中', color: '#ffb020' },
  6: { text: '已退款', color: '#999999' },
}

export function statusText(s) {
  return ORDER_STATUS[s]?.text || '未知'
}
export function statusColor(s) {
  return ORDER_STATUS[s]?.color || '#999999'
}
