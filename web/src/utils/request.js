import { BASE_URL, TOKEN_KEY, REQUEST_TIMEOUT } from '@/config'
import { toast } from './toast'

// 触发登录跳转的鉴权错误码
const AUTH_ERRORS = new Set([1001, 1002, 1003])
const MAINTENANCE = 5001

let redirectingToLogin = false

function gotoLogin() {
  if (redirectingToLogin) return
  redirectingToLogin = true
  uni.removeStorageSync(TOKEN_KEY)
  uni.navigateTo({
    url: '/pages/login/login',
    complete: () => {
      setTimeout(() => (redirectingToLogin = false), 800)
    },
  })
}

/**
 * 统一请求。自动附带 Bearer token，解包 Result<T>：
 * - code === 0 → resolve(data)
 * - 鉴权错误 → 清 token + 跳登录，reject
 * - 其余 → toast(message) 并 reject（可用 { silent: true } 关闭）
 */
export function request(options) {
  const { url, method = 'GET', data, header = {}, silent = false, withToken = true } = options

  return new Promise((resolve, reject) => {
    const finalHeader = { 'Content-Type': 'application/json', ...header }
    if (withToken) {
      const token = uni.getStorageSync(TOKEN_KEY)
      if (token) finalHeader.Authorization = `Bearer ${token}`
    }

    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: finalHeader,
      timeout: REQUEST_TIMEOUT,
      success: (res) => {
        const { statusCode, data: body } = res
        if (statusCode < 200 || statusCode >= 300) {
          if (!silent) toast(`网络异常(${statusCode})`)
          return reject(new Error(`HTTP ${statusCode}`))
        }
        // 非 Result 包裹（极少数）直接返回
        if (!body || typeof body.code === 'undefined') {
          return resolve(body)
        }
        if (body.code === 0) {
          return resolve(body.data)
        }
        if (AUTH_ERRORS.has(body.code)) {
          gotoLogin()
          return reject(new BizError(body.code, body.message))
        }
        if (body.code === MAINTENANCE) {
          if (!silent) toast(body.message || '系统维护中')
          return reject(new BizError(body.code, body.message))
        }
        if (!silent) toast(body.message || '请求失败')
        return reject(new BizError(body.code, body.message))
      },
      fail: (err) => {
        if (!silent) toast('网络连接失败')
        reject(err)
      },
    })
  })
}

export class BizError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
    this.name = 'BizError'
  }
}

export const http = {
  get: (url, data, opts) => request({ url, method: 'GET', data, ...opts }),
  post: (url, data, opts) => request({ url, method: 'POST', data, ...opts }),
}
