import axios from 'axios'
import { ElMessage } from 'element-plus'
import { BASE_URL, TOKEN_KEY, REQUEST_TIMEOUT } from '@/config'
import router from '@/router'

// 鉴权错误码：清 token + 跳登录（与后端约定，见 C 端 request.js）
const AUTH_ERRORS = new Set([1001, 1002, 1003])

let redirecting = false
function gotoLogin() {
  if (redirecting) return
  redirecting = true
  localStorage.removeItem(TOKEN_KEY)
  router.replace('/login').finally(() => {
    setTimeout(() => (redirecting = false), 800)
  })
}

const instance = axios.create({
  baseURL: BASE_URL,
  timeout: REQUEST_TIMEOUT,
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

instance.interceptors.response.use(
  (res) => {
    const body = res.data
    // 非 Result 包裹直接返回
    if (!body || typeof body.code === 'undefined') return body
    if (body.code === 0) return body.data
    if (AUTH_ERRORS.has(body.code)) {
      gotoLogin()
      return Promise.reject(new BizError(body.code, body.message))
    }
    if (!res.config.silent) ElMessage.error(body.message || '请求失败')
    return Promise.reject(new BizError(body.code, body.message))
  },
  (err) => {
    const status = err.response?.status
    if (status === 401 || status === 403) {
      gotoLogin()
    } else if (!err.config?.silent) {
      ElMessage.error(status ? `网络异常(${status})` : '网络连接失败')
    }
    return Promise.reject(err)
  }
)

export class BizError extends Error {
  constructor(code, message) {
    super(message)
    this.code = code
    this.name = 'BizError'
  }
}

export const http = {
  get: (url, params, opts) => instance.get(url, { params, ...opts }),
  post: (url, data, opts) => instance.post(url, data, opts),
}
