# 服务端代码 Review

针对 `server/` 目录的整体质量、正确性、并发/幂等、事务边界、安全等问题的审查报告。按严重程度排序，已去重。

---

## 🔴 CRITICAL — 上线即被打穿

### 1. 支付回调完全不验签，可任意置任何订单为已支付

- `common/auth/AuthInterceptor.java:41` 把 `/order/pay-notify` 加进白名单。
- `infra/pay/WechatPayChannel.java:42-61` 的 `parseNotify` 里代码注释就写着「mock 验签：默认成功」，直接 JSON 反序列化后 `success = json.getBool("success", true)`。
- 攻击者 `POST /api/v1/order/pay-notify` `{"order_no":"<受害订单号>","transaction_id":"x","pay_amount":0.01,"success":true}` → `OrderServiceImpl.handlePaid` (`OrderServiceImpl.java:305-330`) 直接把订单翻成 PAID、核销优惠券、发 `OrderPaidEvent`。

**修复**：接入真实微信支付时必须做 V3 签名/证书 + 平台证书验签；在此之前 `parseNotify` 必须在 prod 直接抛异常。

### 2. 微信 Mock 模式默认开启，prod 未关

- `WechatProperties.mockWhenUnconfigured = true`（`WechatProperties.java:17`），`application-prod.yml` 未覆盖。
- 未配 appid 时 `WechatAuthClient.mockOpenid`（`:91-100`）返回 `"mock_mp_" + code`。攻击者可用任意 `code` 拿到任意用户 token；后台扫码登录同理，配合掌握 QR `state` 就能登入 super admin。

**修复**：prod profile 强制 `mockWhenUnconfigured=false`，或启动时 assert appid 非空。

### 3. JWT 密钥有硬编码默认值

- `application.yml:56`：`${KOALA_JWT_SECRET:koala-dev-secret-please-override-in-prod-env-min-256bit-length}`，`application-prod.yml` 未覆盖。
- 忘记设环境变量 → 使用公开已知密钥 → 任何人可伪造 C 端 / admin token（含 `isSuper=true`）。

**修复**：prod 无默认值，缺失即启动失败。

---

## 🟠 HIGH — 财务 / 库存一致性

### 4. `releaseAssets` / `doRefund` 先执行副作用再做状态 CAS，库存 & 优惠券会被双倍释放

- `OrderServiceImpl.java:508-546`：`skuRepository.addStock(...)`、`userCouponRepository.releaseLock(...)`、`paymentRepository.markRefunded(...)` 都跑在 `cancelFromUnpaidCas` / `updateStatusCas` **之前**。CAS 是唯一 re-entry 守卫，可副作用已提交。
- 场景：用户主动取消 与 `OrderScheduler.autoCancelTimeout` 并发；或双 Pod 同时跑定时任务（`task/OrderScheduler.java:11` 明确说要多 Pod）。

**修复**：CAS 先执行，`affected==1` 才做副作用。

### 5. `adminRefund` / `AfterSale.audit` 忽略 CAS 返回值 → 外部通道退款可能发两次

- `OrderServiceImpl.java:439-450`：`updateStatusCas(..., COMPLETED, AFTER_SALE)` 返回值被丢弃，之后无差别调用 `doRefund` → `channel.refund` (`:519`)。两个管理员同时点退款，钱走两次；DB 最终 5→6 CAS 只能去重状态。
- `AfterSaleServiceImpl.java:191-196`：`REFUND_ONLY` 分支先调 `orderService.refundForAfterSale(...)`，再 CAS 售后单 `PENDING_AUDIT→REFUNDED`。对照同文件 `confirmReceive`（`:218-225`）先 CAS 的写法。
- `refundForAfterSale`（`OrderServiceImpl.java:454-463`）本身是 read-check-act，也没有锁 / CAS 守卫 `channel.refund`。

**修复**：`AFTER_SALE→REFUNDED` 的 CAS 移到 `channel.refund` **之前**；失败时补偿。

### 6. 支付成功 与 超时取消 并发时钱丢了

- `handlePaid`（`:305-330`）与 `releaseTimedOut`（`:483-490`）都在 CAS `WAIT_PAY→...`。若定时任务的 `cancelFromUnpaidCas` 先提交，`markPaidCas` 返回 0，`handlePaid` 走 `:318` 判定为「已处理」提前 return —— 用户实际付了款，订单却是 CANCELED、库存已回、未发起退款。

**修复**：`markPaidCas` 返回 0 时，重新读订单状态；若已终结为 CANCELED，触发反向退款。

### 7. `@Transactional` 内执行外部 IO（`channel.refund`）

- `doRefund`（`OrderServiceImpl.java:508-535`）在事务里发起同步远程调用，握着行锁跨网络。若 DB 提交失败（连接断/死锁），通道已退款、DB 状态未变更、没有补偿路径。

**修复**：外部调用移出事务；本地事务只做状态 CAS + 记录退款流水，通过状态机 / 消息 / 重试补偿。

### 8. Redisson 锁 `leaseTime` 10s 可能小于事务耗时

- `OrderServiceImpl.java:174`：`tryLock(3, 10, SECONDS)`。`doSubmit` 内含 N 次 stock update、优惠券锁、多次 insert。业务超 10s 时锁提前释放，同一用户第二次 submit 可穿过。Redisson 只有在 `leaseTime<0` 时启用 watchdog 续期。

**修复**：改成 `tryLock(3, -1, ...)` 走 watchdog，或把 leaseTime 拉到 tx timeout 之上。

### 9. `AdminCouponServiceImpl.save` 编辑路径静默丢更新

- `AdminCouponServiceImpl.java:86-94`：`Coupon` 用 MP `@Version`（`entity/Coupon.java:37-38`），并发的 `incrementIssuedCas` 会让 admin 的 `updateById` 影响 0 行，返回值未检查 → 管理员改了 totalCount / 有效期，DB 没写入。

---

## 🟡 MEDIUM

### 10. 时区依赖 JVM 默认时区

Jackson 与 JDBC 都固定 `Asia/Shanghai`，但代码里全部 `LocalDateTime.now()`（`OrderServiceImpl:209,315,469,495`、`PriceServiceImpl:55`、`CouponServiceImpl:50,77,94`、`common/entity/MyMetaObjectHandler:17`），取的是 JVM 默认时区。Docker/JRE 基础镜像通常是 UTC → 订单超时、优惠券有效期整体差 8 小时。

**修复**：`server/Dockerfile` 加 `-Duser.timezone=Asia/Shanghai` 或安装 tzdata + `ENV TZ=Asia/Shanghai`。

### 11. 分页 size 无上限

- `config/MyBatisPlusConfig.java:14-19` 未 `setMaxLimit`。
- 所有 list 接口 `@RequestParam(defaultValue="20") long size` 无 `@Max`：`OrderController:66`、`ProductController:32`、`AdminUserController:37`、`AdminOrderController:37`、`AdminCouponController:39`、`AdminAfterSaleController:37`、`AdminProductController:40`、`AfterSaleController:58`。`?size=1000000` 直接命中数据库。

**修复**：`PaginationInnerInterceptor.setMaxLimit(200L)` + DTO 加 `@Max`。

### 12. `AdminCouponServiceImpl` 内存分页

`:46-60` 走 `couponRepository.findAll()` + `subList`，每次全表扫描；配合 #11 更危险。

### 13. `MIN_PAY = 0.01` 强制补 1 分

`PriceServiceImpl.java:35,146-149`：满券 + 免运费本应 payAmount=0，代码强制补到 0.01，可这 1 分不出现在 `productAmount` / `couponDiscount` / `shippingFee` 里 —— `product - coupon + shipping ≠ pay`，一致性方程破。且用户实际被多收 1 分。

**修复**：允许 0 元支付路径，或服务端拒绝「全额覆盖 + 免运费」组合。

### 14. Actuator 匿名可访问

`config/WebMvcConfig.java:25,29` `.excludePathPatterns("/actuator/**")`；`application.yml` 暴露 `health,metrics,info`。`/actuator/metrics/**` 完全匿名可访问，泄露 JVM / HTTP 内部指标。

**修复**：prod 只暴露 `health,info`，或绑 management port 独立。

### 15. `isSuper` 从 JWT claim 取，未在请求时回读 DB

`common/auth/AuthInterceptor.java:95-102` 已经 reload 了 admin 检查 `is_valid`，但 `principal.isSuperAdmin()` 还从 token（`JwtUtil.java:64`）读；被降级的超管仍持有超管权限最长 12h。

**修复**：`/admin/admins/**` 检查处直接用已加载 `admin.isSuper` 而非 claim；降级时把旧 token 加入 `TokenBlacklist`。

### 16. `ConfigServiceImpl` 内存缓存无跨 Pod 失效

`ConfigServiceImpl.java:19,89` 只在写入的那台 Pod 更新本地 `ConcurrentHashMap`；其他 Pod 服务陈旧 `shipping.base_fee` / `payment.active_channel` / `order.pay_timeout_minutes` 直到重启。`OrderScheduler:11` 明确规划了多 Pod。

**修复**：Redis pub/sub 广播 reload，或加 TTL 定期 refresh。

### 17. CORS `allowedOriginPatterns("*") + allowCredentials(true)`

`config/WebMvcConfig.java:34-40`。Bearer token 场景直接风险较低，但配合任一被允许来源的 XSS 就能直接驱动登录态请求。

**修复**：显式白名单。

### 18. `OrderServiceImpl` 是 651 行 / 15 个协作者的 god-service

`@Lazy self` 自注入（`:102-104`）本身是 tx-through-lock 的 code smell，提示 pay / notify / refund 该拆成独立 service。

---

## 🟢 LOW

- **超管 openid 打 INFO 日志**：`SuperAdminInitializer.java:44`；`WechatAuthClient:47,67` 也整个 dump 微信响应 JSON（含 `openid`/`session_key`）。PII 泄露。
- **售后 evidenceImages 无校验**：`AfterSaleApplyRequest.java:21` 不限 URL scheme / 长度 / 域名 → 存 DB 后管理端渲染成 `<img>`；`javascript:` 或非 CDN 域可能构造钓鱼/XSS。
- **`grantOne` 不是事务**：`CouponServiceImpl.java:109-152`。`incrementIssuedCas` 是 autocommit，若进程死在 `insert` 之前且补偿 `decrementIssued` 未跑，`issued_count` 永久偏高。
- **`AdminConfigController.java:33` 返回实体 `SysConfig`**（唯一泄漏 entity 到 controller 层的地方）。
- **`WechatAuthClient` 是具体类不是接口**（`infra/wechat/WechatAuthClient.java`），被三处 Service 直接依赖 —— 违反 CLAUDE.md「扩展点定义为接口」的约定，换渠道要动业务代码。
- **N+1 类别名回填**：`ProductServiceImpl.java:286` 用 `ids.stream().map(categoryRepository::findById)`，同文件 `:276` 已经有正确的 `findByIds` 姿势。
- **缺 `order.paid_at` 索引**：`db/migration/V1__init_schema.sql:170,178-181`。`OrderRepositoryImpl.findPaidSince` 是 dashboard 主查询，会全表扫。
- **`payment.uk_transaction` on nullable**：若通道返回 `""` 而非 `null`，会碰撞。入库前 `""→null`。
- **`GlobalExceptionHandler` 未覆盖 `HttpMessageNotReadableException` / `MaxUploadSizeExceededException`**：畸形 JSON body 目前落到 500 而非 `PARAM_ERROR`。

---

## 建议修复顺序

**上线前必须**：#1 #2 #3（安全底线） → #4 #5 #6 #7（钱和库存） → #10 #11 #14 #15（时区、DoS、Actuator、超管权限）。

---

## 验证过没问题的（避免误改）

- 优惠券每人一张：DB `uk_user_coupon` 唯一键 + Redisson 锁 + version CAS，三重保险已就位。
- 订单提交：submitToken SETNX + `self.doSubmit` 保代理 + `UPDATE ... WHERE stock >= qty` 原子扣减，写法正确。
- BigDecimal 全链路，无 float/double。
- 所有权校验（`requireOwnedOrder` / `requireOwned`）在订单、售后、地址、购物车都有。
- JWT 签名 + 过期校验、token type 双向拒绝均已实现。
- MyBatis 无 `${}` 拼接，全部 `LambdaQueryWrapper` / `#{}`。
- 全局异常处理器覆盖 `BizException` / `MethodArgumentNotValidException` / `BindException` / `ConstraintViolationException` 等。
- Service 层无直接依赖 Mapper，全部走 Repository（符合 CLAUDE.md 分层约定）。
- 状态机通过 DB 层 CAS SQL 保护，重复/非法状态迁移在 SQL 层被拒绝。
- 订单列表已用 `findByOrderNos` 批量加载，无 N+1。
