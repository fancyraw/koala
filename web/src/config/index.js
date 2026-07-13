// 运行时配置。H5 走 vite 代理 /api → 8080；小程序端直连后端域名。
// 小程序上线需在微信后台配置 request 合法域名。
const ENV = {
  // #ifdef H5
  baseUrl: '/api/v1',
  // #endif
  // #ifndef H5
  baseUrl: 'http://localhost:8080/api/v1',
  // #endif
}

export const BASE_URL = ENV.baseUrl
export const TOKEN_KEY = 'koala_token'
export const REQUEST_TIMEOUT = 15000
