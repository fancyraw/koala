// 金额来自后端 BigDecimal，可能是 number 或 string，统一处理。
export function toYuan(v) {
  if (v === null || v === undefined || v === '') return '0.00'
  const n = Number(v)
  if (Number.isNaN(n)) return '0.00'
  return n.toFixed(2)
}

// 拆分整数/小数，便于价格排版（¥ 与小数用小字号）
export function splitPrice(v) {
  const s = toYuan(v)
  const [int, dec] = s.split('.')
  return { int, dec }
}

export function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(typeof ts === 'number' ? ts : String(ts).replace(/-/g, '/'))
  if (Number.isNaN(d.getTime())) return String(ts)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
