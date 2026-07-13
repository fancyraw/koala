# koala

微信小程序电商系统。C 端小程序 + 后台管理 + Java 后端。

业务上只做**优惠券**（满减券、无门槛券，自动发放给所有用户），不含会员等级、积分、折扣。

## 目录结构

| 目录 | 说明 |
| --- | --- |
| `server/` | Java 后端（Spring Boot 2.7 + JDK8） |
| `dashboard/` | 后台管理前端 |
| `web/` | C 端小程序 |
| `docs/` | 产品设计、UI 原型、技术设计文档 |
| `deploy/` | 部署编排（本地开发用的 docker-compose） |
| `scripts/` | 本地开发脚本 |

文档入口（`docs/`）：整体技术设计、C 端产品/UI 设计、后台管理产品/UI 设计，以及可直接打开的 UI 原型 HTML。

## 技术栈

### 后端（`server/`）

- Spring Boot 2.7.18 / JDK 8
- MyBatis-Plus 3.5.5，数据访问统一走 `repository` 层
- MySQL 8 + Flyway（`db/migration` 自动建表并灌入配置/地区种子数据）
- Redis + Redisson（发券加锁、下单幂等）
- JWT（jjwt）鉴权，Hutool 工具库
- Knife4j (OpenAPI3) 接口文档
- 微信登录 / 支付为 mock 实现，未配置 appid 时可全链路本地跑通

### C 端小程序（`web/`）

- uni-app + Vue3（`@dcloudio/uni-app` 3.0）+ Pinia + Vite 5
- 编译目标：微信小程序（`mp-weixin`），开发期也可跑 H5
- 视觉走小红书风格，主色 `#ff2442`

### 后台管理（`dashboard/`）

- Vue3 + Element Plus 2.7 + Vite 5 单页应用（SPA）
- Pinia 状态管理、vue-router、axios、ECharts 5
- 部署基路径 `/koala/`，API 走 `/api/v1/admin/*`
- 中后台风格，主色 `#1677FF`，深色顶栏
- 管理员登录为微信扫码（mock），`isSuper` 能力位从 JWT claim 解析，「管理员管理」仅超管可见
- 覆盖模块：数据看板 / 商品 / 订单 / 用户 / 优惠券 / 内容(Banner) / 系统设置 / 管理员管理

## 本地启动

前置：已安装 Docker、JDK 8、Maven。

```bash
# 一键起：拉起 MySQL + Redis，等就绪后跑后端（dev profile，Flyway 自动迁移）
./scripts/dev-start.sh

# 停依赖（保留数据卷）
./scripts/dev-stop.sh
# 停并清空数据卷
./scripts/dev-stop.sh --clean
```

也可只起依赖、后端自己跑：

```bash
docker compose -f deploy/docker-compose.dev.yml up -d
mvn -f server/pom.xml spring-boot:run
```

启动后：

- API 前缀：`http://localhost:8080/api/v1`
- 接口文档（Knife4j）：`http://localhost:8080/api/v1/doc.html`

### 后台管理（`dashboard/`）

```bash
cd dashboard
npm install
npm run dev      # 开发服务器 http://localhost:5174/koala/
npm run build    # 产物输出到 dashboard/dist（base 路径 /koala/）
```

开发期 `/api/v1` 请求由 Vite 代理转发到 `http://localhost:8080`，需先起后端。

### C 端小程序（`web/`）

```bash
cd web
npm install
npm run dev:mp-weixin    # 编译到 dist/dev/mp-weixin，用微信开发者工具打开该目录
npm run dev:h5           # 或在浏览器里以 H5 调试
npm run build:mp-weixin  # 生产构建
```

## 配置

Spring profile 由 `SPRING_PROFILES_ACTIVE` 控制，默认 `dev`。dev 环境默认连本地 MySQL（`localhost:3306/koala`，root/root）与 Redis（`localhost:6379`），可用环境变量覆盖（`DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` / `REDIS_HOST` / `REDIS_PORT`）。

## 生产部署（阿里云 ECS，全容器）

app、MySQL、Redis 全部跑在容器里，用 `deploy/docker-compose.prod.yml` 编排。app 镜像由 [server/Dockerfile](server/Dockerfile) 多阶段构建（Maven 打包 + JRE8 运行，非 root、内置 Flyway 自动迁移）。

```bash
# 1. 准备环境变量（首次）
cp deploy/.env.example deploy/.env
# 编辑 deploy/.env，填写数据库密码、Redis 密码、JWT 密钥等（.env 已被 .gitignore 忽略，勿提交）

# 2. 构建并启动全套
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build

# 3. 查看日志 / 状态
docker compose -f deploy/docker-compose.prod.yml logs -f app
docker compose -f deploy/docker-compose.prod.yml ps

# 升级（拉新代码后重建 app）
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build app
```

说明：

- 只有 app 对外暴露 `8080`；MySQL/Redis 仅在 compose 内网可达，不映射到宿主机，更安全。
- 数据持久化在命名卷 `koala-mysql` / `koala-redis`，请对宿主机磁盘做好备份（预算充足时建议数据库改用阿里云 RDS + 云 Redis，容器只跑 app）。
- 生产走 `prod` profile，需要的环境变量见 [deploy/.env.example](deploy/.env.example)。

