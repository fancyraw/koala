package com.koala.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.common.constant.ConfigKeys;
import com.koala.common.constant.PaymentChannels;
import com.koala.common.constant.RedisKeys;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.util.SerialNoGenerator;
import com.koala.common.result.PageResult;
import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderItemRequest;
import com.koala.dto.order.OrderItemView;
import com.koala.dto.order.OrderPreviewRequest;
import com.koala.dto.order.OrderPreviewView;
import com.koala.dto.order.OrderRefundRequest;
import com.koala.dto.order.OrderShipRequest;
import com.koala.dto.order.OrderSubmitRequest;
import com.koala.dto.order.OrderSubmitView;
import com.koala.dto.order.OrderView;
import com.koala.dto.order.PriceResult;
import com.koala.dto.order.PricingContext;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.entity.OrderItem;
import com.koala.entity.Payment;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.entity.User;
import com.koala.entity.UserAddress;
import com.koala.entity.UserCoupon;
import com.koala.enums.OrderStatus;
import com.koala.enums.PaymentStatus;
import com.koala.enums.ValidFlag;
import com.koala.event.OrderPaidEvent;
import com.koala.event.RefundedEvent;
import com.koala.infra.pay.NotifyResult;
import com.koala.infra.pay.PaymentChannel;
import com.koala.infra.pay.PaymentChannelFactory;
import com.koala.infra.pay.PrepayCommand;
import com.koala.infra.pay.PrepayResult;
import com.koala.infra.pay.RefundCommand;
import com.koala.infra.pay.RefundResult;
import com.koala.repository.CouponRepository;
import com.koala.repository.OrderCouponRepository;
import com.koala.repository.OrderItemRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.PaymentRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.repository.UserAddressRepository;
import com.koala.repository.UserCouponRepository;
import com.koala.repository.UserRepository;
import com.koala.service.ConfigService;
import com.koala.service.OrderService;
import com.koala.service.PriceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final int SUBMIT_TOKEN_TTL_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderCouponRepository orderCouponRepository;
    private final PaymentRepository paymentRepository;
    private final ProductSkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PriceService priceService;
    private final ConfigService configService;
    private final PaymentChannelFactory paymentChannelFactory;
    private final RedissonClient redisson;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher eventPublisher;

    /** 自注入代理：锁内调用事务方法需走代理，否则 @Transactional 失效。 */
    @Autowired
    @Lazy
    private OrderServiceImpl self;

    public OrderServiceImpl(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                            OrderCouponRepository orderCouponRepository, PaymentRepository paymentRepository,
                            ProductSkuRepository skuRepository, ProductRepository productRepository,
                            CouponRepository couponRepository,
                            UserCouponRepository userCouponRepository, UserAddressRepository addressRepository,
                            UserRepository userRepository, PriceService priceService, ConfigService configService,
                            PaymentChannelFactory paymentChannelFactory, RedissonClient redisson,
                            StringRedisTemplate redis, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderCouponRepository = orderCouponRepository;
        this.paymentRepository = paymentRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.priceService = priceService;
        this.configService = configService;
        this.paymentChannelFactory = paymentChannelFactory;
        this.redisson = redisson;
        this.redis = redis;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public OrderPreviewView preview(Long userId, OrderPreviewRequest req) {
        PricingContext ctx = priceService.calculate(userId, req.getItems(), true);
        OrderPreviewView view = new OrderPreviewView();
        view.setItems(ctx.getItems());
        PriceResult p = ctx.getPrice();
        view.setProductAmount(p.getProductAmount());
        view.setCouponDiscount(p.getCouponDiscount());
        view.setShippingFee(p.getShippingFee());
        view.setPayAmount(p.getPayAmount());
        view.setAppliedCoupons(p.getAppliedCoupons());
        view.setUpsell(ctx.getUpsell());
        if (req.getAddressId() != null) {
            UserAddress addr = addressRepository.findById(req.getAddressId());
            if (addr != null && addr.getUserId().equals(userId)) {
                view.setAddressId(addr.getId());
                view.setReceiverName(addr.getName());
                view.setReceiverPhone(addr.getPhone());
                view.setReceiverAddress(addr.getFullAddress());
            }
        }
        return view;
    }

    @Override
    public OrderSubmitView submit(Long userId, OrderSubmitRequest req) {
        // a. 幂等：submitToken SETNX
        Boolean firstSubmit = redis.opsForValue().setIfAbsent(
                RedisKeys.ORDER_SUBMIT_TOKEN + req.getSubmitToken(), "1",
                SUBMIT_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(firstSubmit)) {
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT);
        }

        UserAddress addr = addressRepository.findById(req.getAddressId());
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ADDRESS_INVALID);
        }

        RLock lock = redisson.getLock(RedisKeys.LOCK_ORDER_SUBMIT + userId);
        boolean locked = false;
        try {
            // leaseTime < 0：交给 Redisson watchdog 每 10s 续期，避免长事务（多商品扣库存 + 券锁定 + 多次 insert）
            // 超过固定租期导致锁提前释放、允许并发第二次提交。
            locked = lock.tryLock(3, -1, TimeUnit.SECONDS);
            if (!locked) {
                throw new BizException(ErrorCode.DUPLICATE_SUBMIT);
            }
            return self.doSubmit(userId, req, addr);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitView doSubmit(Long userId, OrderSubmitRequest req, UserAddress addr) {
        // 算价(权威)——内部已校验商品存在/上架
        PricingContext ctx = priceService.calculate(userId, req.getItems(), false);
        PriceResult price = ctx.getPrice();

        // b. 校验单次限购
        checkPurchaseLimit(req, ctx);

        // c. 扣库存：乐观锁条件更新
        for (int i = 0; i < req.getItems().size(); i++) {
            ProductSku sku = ctx.getSkus().get(i);
            int qty = req.getItems().get(i).getQuantity();
            if (skuRepository.deductStock(sku.getId(), qty) == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH.getCode(),
                        "「" + ctx.getItems().get(i).getProductName() + "」库存不足");
            }
        }

        // d/f. 建订单
        LocalDateTime now = LocalDateTime.now();
        int timeoutMinutes = configService.getInt(ConfigKeys.Order.GROUP, ConfigKeys.Order.PAY_TIMEOUT_MINUTES, 30);
        String orderNo = genOrderNo();

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setReceiverName(addr.getName());
        order.setReceiverPhone(addr.getPhone());
        order.setReceiverAddress(addr.getFullAddress());
        order.setProductAmount(price.getProductAmount());
        order.setCouponDiscount(price.getCouponDiscount());
        order.setShippingFee(price.getShippingFee());
        order.setPayAmount(price.getPayAmount());
        order.setStatus(OrderStatus.WAIT_PAY.code());
        order.setRemark(req.getRemark() != null ? req.getRemark() : "");
        order.setUserDeleted(ValidFlag.DISABLED.code());
        order.setExpireAt(now.plusMinutes(timeoutMinutes));
        order.setVersion(0);
        orderRepository.insert(order);

        for (OrderItemView v : ctx.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrderNo(orderNo);
            item.setProductId(v.getProductId());
            item.setSkuId(v.getSkuId());
            item.setProductName(v.getProductName());
            item.setSkuName(v.getSkuName());
            item.setProductImage(v.getProductImage());
            item.setUnitPrice(v.getUnitPrice());
            item.setQuantity(v.getQuantity());
            item.setSubtotal(v.getSubtotal());
            orderItemRepository.insert(item);
        }

        // e. 锁定券：0→3 + lock_order_no；写 order_coupon
        for (UserCoupon uc : ctx.getAppliedUserCoupons()) {
            if (userCouponRepository.lockForOrder(uc.getId(), orderNo) == 0) {
                throw new BizException(ErrorCode.COUPON_UNAVAILABLE.getCode(), "优惠券已失效，请重新下单");
            }
            PriceResult.AppliedCoupon av = price.getAppliedCoupons().stream()
                    .filter(a -> a.getUserCouponId().equals(uc.getId())).findFirst().orElse(null);
            OrderCoupon oc = new OrderCoupon();
            oc.setOrderNo(orderNo);
            oc.setUserCouponId(uc.getId());
            oc.setCouponId(uc.getCouponId());
            oc.setCouponType(av != null ? av.getCouponType() : 0);
            oc.setDiscountAmount(av != null ? av.getDiscountAmount() : BigDecimal.ZERO);
            orderCouponRepository.insert(oc);
        }

        // 建 payment(待支付)
        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setChannel(configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        payment.setPayAmount(price.getPayAmount());
        payment.setStatus(PaymentStatus.PENDING.code());
        paymentRepository.insert(payment);

        // 零元订单：满券叠免邮时 payAmount=0，无需真实调支付渠道，直接推进到已支付流程。
        // 所有副作用都在当前事务内完成——插入的 order/payment 尚未提交，CAS 一定成功。
        if (price.getPayAmount().signum() == 0) {
            orderRepository.markPaidCas(orderNo, now);
            for (UserCoupon uc : ctx.getAppliedUserCoupons()) {
                if (userCouponRepository.redeem(uc.getId(), now) > 0) {
                    couponRepository.incrementUsedCount(uc.getCouponId());
                }
            }
            paymentRepository.markSuccess(orderNo, null, now);
            eventPublisher.publishEvent(new OrderPaidEvent(this, orderNo));
        }

        OrderSubmitView view = new OrderSubmitView();
        view.setOrderNo(orderNo);
        view.setPayAmount(price.getPayAmount());
        return view;
    }

    @Override
    public PrepayResult pay(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不可支付");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单已超时");
        }
        // 零元订单在提交事务里已直接置为 WAIT_SHIP，正常不会到这里；保底再拒绝一次。
        if (order.getPayAmount() != null && order.getPayAmount().signum() == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "零元订单无需支付");
        }
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        PrepayCommand cmd = new PrepayCommand();
        cmd.setOrderNo(orderNo);
        cmd.setPayAmount(order.getPayAmount());
        cmd.setDescription("订单 " + orderNo);
        return channel.prepay(cmd);
    }

    @Override
    public boolean payNotify(HttpServletRequest request) {
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        NotifyResult notify = channel.parseNotify(request);
        if (notify == null || !notify.isSuccess() || notify.getOrderNo() == null) {
            log.warn("支付回调验签失败或未成功: {}", notify);
            return false;
        }
        return self.handlePaid(notify);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean handlePaid(NotifyResult notify) {
        String orderNo = notify.getOrderNo();
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            return false;
        }
        // 幂等：非待付款即视为已处理
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        // order 0→1 CAS
        if (orderRepository.markPaidCas(orderNo, now) == 0) {
            // 与超时取消 race：重新读订单，若已被取消而钱到账则登记待退款，事后由 admin/售后触发实际退款。
            Order latest = orderRepository.findByNo(orderNo);
            if (latest != null && OrderStatus.CANCELED.is(latest.getStatus())) {
                paymentRepository.markSuccess(orderNo, notify.getTransactionId(), now);
                log.error("订单已取消但收到成功支付回调，需人工介入退款: orderNo={}, txId={}, amount={}",
                        orderNo, notify.getTransactionId(), notify.getPayAmount());
                return true;
            }
            return true; // 已被其他节点处理
        }
        // 券 3→1 核销 + coupon.used_count+1
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(orderNo)) {
            if (userCouponRepository.redeem(oc.getUserCouponId(), now) > 0) {
                couponRepository.incrementUsedCount(oc.getCouponId());
            }
        }
        // payment 置成功(回调交易号)
        paymentRepository.markSuccess(orderNo, notify.getTransactionId(), now);
        eventPublisher.publishEvent(new OrderPaidEvent(this, orderNo));
        return true;
    }

    @Override
    public PageResult<OrderView> myOrders(Long userId, Integer status, long page, long size) {
        IPage<Order> p = orderRepository.pageByUser(userId, status, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNo(
                p.getRecords().stream().map(Order::getOrderNo).collect(Collectors.toList()));
        List<OrderView> list = p.getRecords().stream()
                .map(o -> toView(o, itemMap.getOrDefault(o.getOrderNo(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public OrderView detail(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        return toView(order, orderItemRepository.findByOrderNo(orderNo));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (!OrderStatus.WAIT_PAY.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待付款订单可取消");
        }
        releaseAssets(order, OrderStatus.CANCELED.code());
    }

    @Override
    public void confirm(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (OrderStatus.COMPLETED.is(order.getStatus())) {
            return; // 幂等
        }
        if (!OrderStatus.WAIT_RECEIVE.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待收货订单可确认收货");
        }
        if (orderRepository.markCompletedCas(orderNo, LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    @Override
    public void deleteByUser(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (!(OrderStatus.COMPLETED.is(order.getStatus()) || OrderStatus.CANCELED.is(order.getStatus())
                || OrderStatus.REFUNDED.is(order.getStatus()))) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成/已取消/已退款订单可删除");
        }
        orderRepository.markUserDeleted(orderNo);
    }

    // ---- 后台 ----

    @Override
    public PageResult<AdminOrderView> adminList(String keyword, Integer status, long page, long size) {
        final String kw = keyword != null ? keyword.trim() : null;
        Set<Long> matchedUserIds = Collections.emptySet();
        if (kw != null && !kw.isEmpty()) {
            matchedUserIds = new java.util.HashSet<>(userRepository.findIdsByNicknameLike(kw));
        }
        IPage<Order> p = orderRepository.pageForAdmin(status, kw, matchedUserIds, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNo(
                p.getRecords().stream().map(Order::getOrderNo).collect(Collectors.toList()));
        Map<Long, String> nickMap = userRepository.findByIds(
                        p.getRecords().stream().map(Order::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getNickname));
        List<AdminOrderView> list = p.getRecords().stream()
                .map(o -> toAdminView(o, itemMap.getOrDefault(o.getOrderNo(), Collections.emptyList()),
                        nickMap.get(o.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AdminOrderView adminDetail(String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        List<OrderItem> items = orderItemRepository.findByOrderNo(orderNo);
        User user = userRepository.findById(order.getUserId());
        return toAdminView(order, items, user != null ? user.getNickname() : null);
    }

    @Override
    public void ship(OrderShipRequest req) {
        Order order = orderRepository.findByNo(req.getNo());
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.WAIT_SHIP.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待发货订单可发货");
        }
        if (orderRepository.markShippedCas(req.getNo(), req.getLogisticsCompany(),
                req.getLogisticsNo(), LocalDateTime.now()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    @Override
    public void adminRefund(OrderRefundRequest req) {
        Order order = orderRepository.findByNo(req.getNo());
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.COMPLETED.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成订单可手动退款");
        }
        // 3→5 售后中：CAS 拿到独占权，返回 0 说明并发已进入售后流程。
        if (orderRepository.updateStatusCas(req.getNo(),
                OrderStatus.COMPLETED.code(), OrderStatus.AFTER_SALE.code()) == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单状态已变更，请刷新重试");
        }
        // CAS 成功后 order 已 == AFTER_SALE，reload 一次以获取最新 payment 关联字段。
        Order latest = orderRepository.findByNo(req.getNo());
        executeRefund(latest, req.getReason(), OrderStatus.COMPLETED.code());
    }

    @Override
    public void refundForAfterSale(String orderNo, String reason) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (!OrderStatus.AFTER_SALE.is(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不在售后中，不可退款");
        }
        // 售后侧已保证订单处于 AFTER_SALE；直接进入 CAS + channel + tx-B 流程。
        // rollbackStatus 传 -1 表示外部渠道失败时不做订单状态回滚（保持 AFTER_SALE，由售后侧处理）。
        executeRefund(order, reason, -1);
    }

    // ---- 定时任务(7.4) ----

    @Override
    public int autoCancelTimeout() {
        List<Order> timedOut = orderRepository.findTimedOutUnpaid(LocalDateTime.now());
        int count = 0;
        for (Order order : timedOut) {
            try {
                self.releaseTimedOut(order.getOrderNo());
                count++;
            } catch (Exception e) {
                log.error("超时取消订单失败: {}", order.getOrderNo(), e);
            }
        }
        return count;
    }

    /** 单订单超时释放(独立事务)：仅 status=0 时释放，CAS 防并发支付。 */
    @Transactional(rollbackFor = Exception.class)
    public void releaseTimedOut(String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null || !OrderStatus.WAIT_PAY.is(order.getStatus())) {
            return;
        }
        releaseAssets(order, OrderStatus.CANCELED.code());
    }

    @Override
    public int autoConfirmReceived() {
        int days = configService.getInt(ConfigKeys.Order.GROUP, ConfigKeys.Order.AUTO_CONFIRM_DAYS, 7);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<Order> due = orderRepository.findAutoConfirmDue(deadline);
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Order order : due) {
            if (orderRepository.markCompletedCas(order.getOrderNo(), now) > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * 退款处理(6.5)：
     * 1) 调外部支付渠道（无事务，避免持锁跨网络）
     * 2) 事务 B 内更新 payment.refunded + 未过期券回退 + order 5→6 + 发事件
     * 渠道失败：如果 rollbackStatus &gt;= 0，把订单 CAS 回滚到该状态；否则保持 AFTER_SALE 由上游处理。
     */
    private void executeRefund(Order order, String reason, int rollbackStatus) {
        Payment payment = paymentRepository.findByOrderNoAndStatus(
                order.getOrderNo(), PaymentStatus.SUCCESS.code());
        PaymentChannel channel = paymentChannelFactory.get(
                payment != null ? payment.getChannel() : configService.get(ConfigKeys.Payment.GROUP, ConfigKeys.Payment.ACTIVE_CHANNEL, PaymentChannels.WECHAT));
        RefundCommand cmd = new RefundCommand();
        cmd.setOrderNo(order.getOrderNo());
        cmd.setTransactionId(payment != null ? payment.getTransactionId() : null);
        cmd.setRefundAmount(order.getPayAmount());
        cmd.setTotalAmount(order.getPayAmount());
        cmd.setReason(reason);

        RefundResult result;
        try {
            result = channel.refund(cmd);
        } catch (RuntimeException e) {
            rollbackRefundStatus(order.getOrderNo(), rollbackStatus, "channel 抛出异常");
            throw e;
        }
        if (result == null || !result.isSuccess()) {
            rollbackRefundStatus(order.getOrderNo(), rollbackStatus, "channel 返回失败");
            throw new BizException(ErrorCode.REFUND_FAILED);
        }
        // 走一次独立事务完成本地状态收尾。渠道已成功，此处失败需要人工介入（不再回滚渠道退款）。
        self.finalizeRefund(order.getOrderNo(), payment != null ? payment.getId() : null);
    }

    private void rollbackRefundStatus(String orderNo, int rollbackStatus, String cause) {
        if (rollbackStatus < 0) {
            return;
        }
        int affected = orderRepository.updateStatusCas(orderNo,
                OrderStatus.AFTER_SALE.code(), rollbackStatus);
        if (affected == 0) {
            log.warn("退款渠道失败但订单状态已流转，回滚跳过 orderNo={}, cause={}", orderNo, cause);
        }
    }

    /** 事务 B：本地状态收尾。channel 已成功，此处失败需人工修复；不再回滚。 */
    @Transactional(rollbackFor = Exception.class)
    public void finalizeRefund(String orderNo, Long paymentId) {
        if (paymentId != null) {
            paymentRepository.markRefunded(paymentId);
        }
        LocalDateTime now = LocalDateTime.now();
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(orderNo)) {
            userCouponRepository.restoreIfNotExpired(oc.getUserCouponId(), now);
        }
        // 5→6 已退款(终态)
        orderRepository.updateStatusCas(orderNo,
                OrderStatus.AFTER_SALE.code(), OrderStatus.REFUNDED.code());
        eventPublisher.publishEvent(new RefundedEvent(this, orderNo));
    }

    /**
     * 释放资产(6.4)：CAS 先行——待付款(0)→targetStatus 成功后才回滚库存 & 释放券，
     * 避免与支付回调 / 定时超时任务并发时出现资产双倍释放。
     */
    private void releaseAssets(Order order, int targetStatus) {
        int affected = orderRepository.cancelFromUnpaidCas(
                order.getOrderNo(), targetStatus, LocalDateTime.now());
        if (affected == 0) {
            log.info("releaseAssets 跳过：订单状态已流转 orderNo={}", order.getOrderNo());
            return;
        }
        for (OrderItem item : orderItemRepository.findByOrderNo(order.getOrderNo())) {
            skuRepository.addStock(item.getSkuId(), item.getQuantity());
        }
        for (OrderCoupon oc : orderCouponRepository.findByOrderNo(order.getOrderNo())) {
            userCouponRepository.releaseLock(oc.getUserCouponId());
        }
    }

    // ---- helpers ----

    private void checkPurchaseLimit(OrderSubmitRequest req, PricingContext ctx) {
        Set<Long> productIds = ctx.getSkus().stream().map(ProductSku::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productRepository.findByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, ProductSku> skuMap = ctx.getSkus().stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        for (OrderItemRequest item : req.getItems()) {
            ProductSku sku = skuMap.get(item.getSkuId());
            if (sku == null) {
                continue;
            }
            Product product = productMap.get(sku.getProductId());
            if (product != null && product.getPerOrderLimit() != null && product.getPerOrderLimit() > 0
                    && item.getQuantity() > product.getPerOrderLimit()) {
                throw new BizException(ErrorCode.PURCHASE_LIMIT.getCode(),
                        "该商品每单限购 " + product.getPerOrderLimit() + " 件");
            }
        }
    }

    private Order requireOwnedOrder(Long userId, String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)
                || ValidFlag.ENABLED.is(order.getUserDeleted())) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return order;
    }

    private Map<String, List<OrderItem>> itemsByOrderNo(List<String> orderNos) {
        return orderItemRepository.findByOrderNos(orderNos)
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderNo));
    }

    private OrderView toView(Order o, List<OrderItem> items) {
        OrderView v = new OrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setExpireAt(o.getExpireAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(this::toItemView).collect(Collectors.toList()));
        return v;
    }

    private AdminOrderView toAdminView(Order o, List<OrderItem> items, String nickname) {
        AdminOrderView v = new AdminOrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setNickname(nickname);
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(this::toItemView).collect(Collectors.toList()));
        return v;
    }

    private OrderItemView toItemView(OrderItem item) {
        OrderItemView v = new OrderItemView();
        v.setProductId(item.getProductId());
        v.setSkuId(item.getSkuId());
        v.setProductName(item.getProductName());
        v.setSkuName(item.getSkuName());
        v.setProductImage(item.getProductImage());
        v.setUnitPrice(item.getUnitPrice());
        v.setQuantity(item.getQuantity());
        v.setSubtotal(item.getSubtotal());
        return v;
    }

    private String genOrderNo() {
        return SerialNoGenerator.next(SerialNoGenerator.ORDER_PREFIX);
    }
}
