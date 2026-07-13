# koala-web

Koala C 端小程序（uni-app + Vue3，编译到微信小程序，兼容 H5 本地预览）。

## 技术栈

- uni-app（Vue3 + Vite）
- Pinia 状态管理（登录态、购物车角标）
- SCSS，设计 token 集中在 [src/uni.scss](src/uni.scss)（小红书风格：轻盈通透、图片即主角）

## 目录结构

| 目录 | 说明 |
| --- | --- |
| `src/pages/` | 页面（首页/分类/购物车/我的 + 商品、订单、地址、优惠券、售后、登录） |
| `src/components/` | 通用组件（`k-` 前缀，easycom 自动注册）：k-price / k-product-card / k-empty / k-navbar |
| `src/store/` | Pinia store（auth、cart） |
| `src/api/` | 接口封装，统一走 [src/utils/request.js](src/utils/request.js) |
| `src/utils/` | request（Result 解包 + 鉴权跳转）、format、toast、order-status |
| `src/config/` | baseUrl、token key 等运行时配置 |
| `src/static/tabbar/` | Tab 图标（由 `scripts/gen-tabbar-icons.py` 生成） |

## 本地开发

前置：Node ≥ 18，后端已按根目录 README 起在 `http://localhost:8080`。

```bash
cd web
npm install

# H5 预览（浏览器，走 vite 代理 /api → localhost:8080，无跨域）
npm run dev:h5

# 微信小程序（产物在 dist/dev/mp-weixin，用微信开发者工具导入运行）
npm run dev:mp-weixin
```

## 构建

```bash
npm run build:h5          # dist/build/h5
npm run build:mp-weixin   # dist/build/mp-weixin，微信开发者工具导入
```

## 接口约定

- 统一响应 `Result<T>`：`{ code, message, data, traceId }`，`code === 0` 为成功；
  `1001/1002/1003` 鉴权失败 → 自动清 token 并跳登录。
- 鉴权：`Authorization: Bearer <jwt>`，token 存于本地缓存 `koala_token`。
- 微信登录为后端 mock 实现：`wx.login` 取 code → `POST /user/login` 换 token；
  H5 端用 `h5_dev_<ts>` 占位 code，可全链路本地跑通。

## 说明

- 小程序上线前需在 [src/manifest.json](src/manifest.json) 填 `appid`，
  并在微信公众平台配置 request 合法域名为后端地址。
- Tab 图标为脚本生成的线性占位图，正式设计稿替换 `src/static/tabbar/` 下同名 PNG 即可。
- 已实现主链路：首页 → 商品列表/分类 → 商品详情（SKU 选规格） → 购物车 → 确认订单 → 支付结果 → 订单详情/列表；
  以及登录、我的、优惠券、地址管理/编辑、售后申请。
