# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Koala 是一个微信小程序电商项目，由三个可独立开发/构建的部分组成：

- `server/` — Spring Boot 2.7 + JDK 8 后端，Maven 单模块
- `dashboard/` — Vue3 + Element Plus + Vite 后台管理 SPA
- `web/` — uni-app + Vue3 C 端小程序（编译目标 `mp-weixin`，也可 H5 调试）
- `deploy/` — 本地/生产的 docker-compose 编排
- `docs/` — 产品/UI/技术设计（`整体技术设计.md` 是权威架构文档）

业务基线特意收敛：只有优惠券（满减 + 无门槛，全员自动发放），**没有会员等级、积分、单品折扣**。做设计/评审时不要引入这些概念。

## 常用命令

### 依赖 + 后端

```bash
# 一键起：拉起 MySQL(3306) + Redis(6379)（docker-compose.dev.yml），健康后跑后端
./scripts/dev-start.sh

# 停依赖（保留数据卷 / 清空数据卷）
./scripts/dev-stop.sh
./scripts/dev-stop.sh --clean

# 只跑后端（依赖需已起）
mvn -f server/pom.xml spring-boot:run

# 打包 / 测试
mvn -f server/pom.xml package
mvn -f server/pom.xml test
mvn -f server/pom.xml -Dtest=SomeTest#method test    # 单测
```

后端跑起来后：API 走 `http://localhost:8080/api/v1`，Knife4j 文档 `http://localhost:8080/api/v1/doc.html`。

### 后台管理（dashboard/）

```bash
cd dashboard
npm install
npm run dev       # http://localhost:5174/koala/  （/api/v1 由 vite 代理到 :8080）
npm run build     # 输出 dashboard/dist，base 路径固定 /koala/
```

### C 端小程序（web/）

```bash
cd web
npm install
npm run dev:mp-weixin   # 产物 dist/dev/mp-weixin，用微信开发者工具打开该目录
npm run dev:h5          # 浏览器 H5 调试
npm run build:mp-weixin # 生产构建
```

## 后端架构（关键点）

- 传统三层：`controller` → `service` → `repository`（`repository/impl` 里包 MyBatis-Plus 的 `mapper`），按技术层组织包，依赖单向向下。数据访问统一走 `repository`，不要在 Service 里直接依赖 Mapper。
- 包结构：`common`（统一响应/异常/鉴权/上下文/切面）、`config`（Spring 配置）、`infra`（外部能力 SPI：`pay` / `storage` / `wechat`）、`controller`（C 端 + `controller/admin` 子包）、`service` / `service/impl`、`repository` / `repository/impl` / `mapper`、`entity`、`dto`（按业务域分子包）、`enums`、`event`、`task`。
- 扩展点用接口 + 多实现（支付、存储、登录），运行时按配置激活；新增渠道不应改业务代码。
- 鉴权：JWT（jjwt）。C 端 token TTL 3 天，后台 12 小时；后台超管由 `koala.admin.super-openid` 环境变量指定，`isSuper` 通过 JWT claim 传给前端控制入口（如「管理员管理」）。
- 并发/幂等：Redis + Redisson，用于发券加锁、下单幂等。业务策略优先走数据库 `sys_config` 表（可运行时调整），环境差异走 Spring profile + 环境变量。
- Schema 由 Flyway 管：`server/src/main/resources/db/migration/V*__*.sql`。**只加新的 `V{n}__*.sql`，不要改历史文件**（会导致 checksum 不一致）。种子数据（`sys_config`、地区）也走 Flyway。
- Jackson 全局 `GMT+8`、`yyyy-MM-dd HH:mm:ss`、`non_null`。MyBatis-Plus 已开 `map-underscore-to-camel-case`，实体字段直接驼峰。
- 微信登录 / 支付默认 mock 实现，未配 appid 也能全链路本地跑通。

### 配置

- Profile 由 `SPRING_PROFILES_ACTIVE` 决定，默认 `dev`。
- Dev 默认连本地 `mysql://root:root@localhost:3306/koala`、`redis://localhost:6379`，可用 `DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD/REDIS_HOST/REDIS_PORT` 覆盖。
- 生产：`deploy/.env`（gitignore 忽略）+ `deploy/docker-compose.prod.yml`，`server/Dockerfile` 是多阶段（Maven 打包 → JRE8 运行，非 root，内置 Flyway 自动迁移）。

## 前端约定

- `dashboard/` 部署基路径 **`/koala/`**（vite `base` 和线上 Nginx 都要保持一致）；API 前缀 `/api/v1/admin/*`；主色 `#1677FF`，深色顶栏。ElementPlus 用 `unplugin-auto-import` + `unplugin-vue-components` 按需，别手动全量注册。别名 `@` = `dashboard/src`。SCSS 全局注入 `@/styles/tokens.scss`（design tokens 都在里面，改样式走 token，不要散落硬编码色值）。
- `web/` 视觉走小红书风格，主色 `#ff2442`；uni-app 编译目标默认 `mp-weixin`。

## 部署

- 全容器方案（app + MySQL + Redis）在 `deploy/docker-compose.prod.yml`，只暴露 app 的 `8080`，DB/Redis 只在 compose 内网可达。数据落在命名卷 `koala-mysql` / `koala-redis`。
- 升级流程：`docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build app`。

## 参考文档

- `docs/整体技术设计.md` — 架构、扩展点、数据模型、算价/下单/支付/售后/券机制、幂等并发、安全、部署（权威）
- `docs/C端产品设计.md` / `docs/后台管理产品设计.md` — 业务范围与页面清单
- `docs/C端UI设计.md` / `docs/后台管理UI设计.md`（+ 对应 `*UI原型.html`） — 视觉与交互
