package com.koala.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
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
import com.koala.entity.Coupon;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.entity.OrderItem;
import com.koala.entity.Payment;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.entity.User;
import com.koala.entity.UserAddress;
import com.koala.entity.UserCoupon;
import com.koala.event.OrderPaidEvent;
import com.koala.event.RefundedEvent;
import com.koala.infra.pay.NotifyResult;
import com.koala.infra.pay.PaymentChannel;
import com.koala.infra.pay.PaymentChannelFactory;
import com.koala.infra.pay.PrepayCommand;
import com.koala.infra.pay.PrepayResult;
import com.koala.infra.pay.RefundCommand;
import com.koala.infra.pay.RefundResult;
import com.koala.mapper.CouponMapper;
import com.koala.mapper.OrderCouponMapper;
import com.koala.mapper.OrderItemMapper;
import com.koala.mapper.OrderMapper;
import com.koala.mapper.PaymentMapper;
import com.koala.mapper.ProductMapper;
import com.koala.mapper.ProductSkuMapper;
import com.koala.mapper.UserAddressMapper;
import com.koala.mapper.UserCouponMapper;
import com.koala.mapper.UserMapper;
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String SUBMIT_TOKEN_PREFIX = "order:submit:";
    private static final String LOCK_PREFIX = "lock:order:submit:";
    private static final int SUBMIT_TOKEN_TTL_MINUTES = 30;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final PaymentMapper paymentMapper;
    private final ProductSkuMapper skuMapper;
    private final ProductMapper productMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final UserAddressMapper addressMapper;
    private final UserMapper userMapper;
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

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            OrderCouponMapper orderCouponMapper, PaymentMapper paymentMapper,
                            ProductSkuMapper skuMapper, ProductMapper productMapper, CouponMapper couponMapper,
                            UserCouponMapper userCouponMapper, UserAddressMapper addressMapper,
                            UserMapper userMapper, PriceService priceService, ConfigService configService,
                            PaymentChannelFactory paymentChannelFactory, RedissonClient redisson,
                            StringRedisTemplate redis, ApplicationEventPublisher eventPublisher) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.orderCouponMapper = orderCouponMapper;
        this.paymentMapper = paymentMapper;
        this.skuMapper = skuMapper;
        this.productMapper = productMapper;
        this.couponMapper = couponMapper;
        this.userCouponMapper = userCouponMapper;
        this.addressMapper = addressMapper;
        this.userMapper = userMapper;
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
            UserAddress addr = addressMapper.selectById(req.getAddressId());
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
                SUBMIT_TOKEN_PREFIX + req.getSubmitToken(), "1",
                SUBMIT_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(firstSubmit)) {
            throw new BizException(ErrorCode.DUPLICATE_SUBMIT);
        }

        UserAddress addr = addressMapper.selectById(req.getAddressId());
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.ADDRESS_INVALID);
        }

        RLock lock = redisson.getLock(LOCK_PREFIX + userId);
        boolean locked = false;
        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
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
            int affected = skuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                    .setSql("stock = stock - " + qty)
                    .eq(ProductSku::getId, sku.getId())
                    .ge(ProductSku::getStock, qty));
            if (affected == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH.getCode(),
                        "「" + ctx.getItems().get(i).getProductName() + "」库存不足");
            }
        }

        // d/f. 建订单
        LocalDateTime now = LocalDateTime.now();
        int timeoutMinutes = configService.getInt("order", "pay_timeout_minutes", 30);
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
        order.setStatus(0);
        order.setRemark(req.getRemark() != null ? req.getRemark() : "");
        order.setUserDeleted(0);
        order.setExpireAt(now.plusMinutes(timeoutMinutes));
        order.setVersion(0);
        orderMapper.insert(order);

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
            orderItemMapper.insert(item);
        }

        // e. 锁定券：0→3 + lock_order_no；写 order_coupon
        for (UserCoupon uc : ctx.getAppliedUserCoupons()) {
            int affected = userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .set(UserCoupon::getStatus, 3)
                    .set(UserCoupon::getLockOrderNo, orderNo)
                    .eq(UserCoupon::getId, uc.getId())
                    .eq(UserCoupon::getStatus, 0));
            if (affected == 0) {
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
            orderCouponMapper.insert(oc);
        }

        // 建 payment(待支付)
        Payment payment = new Payment();
        payment.setOrderNo(orderNo);
        payment.setChannel(configService.get("payment", "active_channel", "wechat"));
        payment.setPayAmount(price.getPayAmount());
        payment.setStatus(0);
        paymentMapper.insert(payment);

        OrderSubmitView view = new OrderSubmitView();
        view.setOrderNo(orderNo);
        view.setPayAmount(price.getPayAmount());
        return view;
    }

    @Override
    public PrepayResult pay(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (order.getStatus() != 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不可支付");
        }
        if (order.getExpireAt() != null && order.getExpireAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单已超时");
        }
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get("payment", "active_channel", "wechat"));
        PrepayCommand cmd = new PrepayCommand();
        cmd.setOrderNo(orderNo);
        cmd.setPayAmount(order.getPayAmount());
        cmd.setDescription("订单 " + orderNo);
        return channel.prepay(cmd);
    }

    @Override
    public boolean payNotify(HttpServletRequest request) {
        PaymentChannel channel = paymentChannelFactory.get(
                configService.get("payment", "active_channel", "wechat"));
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
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            return false;
        }
        // 幂等：非待付款即视为已处理
        if (order.getStatus() != 0) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now();
        // order 0→1 CAS
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 1)
                .set(Order::getPaidAt, now)
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, 0));
        if (affected == 0) {
            return true; // 并发已处理
        }
        // 券 3→1 核销 + coupon.used_count+1
        List<OrderCoupon> ocs = orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .eq(OrderCoupon::getOrderNo, orderNo));
        for (OrderCoupon oc : ocs) {
            int ucAffected = userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .set(UserCoupon::getStatus, 1)
                    .set(UserCoupon::getUsedAt, now)
                    .eq(UserCoupon::getId, oc.getUserCouponId())
                    .eq(UserCoupon::getStatus, 3));
            if (ucAffected > 0) {
                couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                        .setSql("used_count = used_count + 1")
                        .eq(Coupon::getId, oc.getCouponId()));
            }
        }
        // payment 置成功(回调交易号)
        paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .set(Payment::getStatus, 1)
                .set(Payment::getTransactionId, notify.getTransactionId())
                .set(Payment::getPaidAt, now)
                .eq(Payment::getOrderNo, orderNo)
                .eq(Payment::getStatus, 0));
        eventPublisher.publishEvent(new OrderPaidEvent(this, orderNo));
        return true;
    }

    @Override
    public PageResult<OrderView> myOrders(Long userId, Integer status, long page, long size) {
        IPage<Order> p = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<Order>lambdaQuery()
                        .eq(Order::getUserId, userId)
                        .eq(Order::getUserDeleted, 0)
                        .eq(status != null, Order::getStatus, status)
                        .orderByDesc(Order::getId));
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
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));
        return toView(order, items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (order.getStatus() != 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待付款订单可取消");
        }
        releaseAssets(order, 4);
    }

    @Override
    public void confirm(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (order.getStatus() == 3) {
            return; // 幂等
        }
        if (order.getStatus() != 2) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待收货订单可确认收货");
        }
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 3)
                .set(Order::getCompletedAt, LocalDateTime.now())
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getStatus, 2));
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    @Override
    public void deleteByUser(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        if (!(order.getStatus() == 3 || order.getStatus() == 4 || order.getStatus() == 6)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成/已取消/已退款订单可删除");
        }
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getUserDeleted, 1)
                .eq(Order::getOrderNo, orderNo));
    }

    // ---- 后台 ----

    @Override
    public PageResult<AdminOrderView> adminList(String keyword, Integer status, long page, long size) {
        final String kw = keyword != null ? keyword.trim() : null;
        Set<Long> matchedUserIds = Collections.emptySet();
        if (kw != null && !kw.isEmpty()) {
            matchedUserIds = userMapper.selectList(Wrappers.<User>lambdaQuery()
                            .like(User::getNickname, kw).select(User::getId))
                    .stream().map(User::getId).collect(Collectors.toSet());
        }
        final Set<Long> uids = matchedUserIds;
        IPage<Order> p = orderMapper.selectPage(new Page<>(page, size),
                Wrappers.<Order>lambdaQuery()
                        .eq(status != null, Order::getStatus, status)
                        .and(kw != null && !kw.isEmpty(), w -> {
                            w.like(Order::getOrderNo, kw).or().like(Order::getReceiverName, kw);
                            if (!uids.isEmpty()) {
                                w.or().in(Order::getUserId, uids);
                            }
                        })
                        .orderByDesc(Order::getId));
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNo(
                p.getRecords().stream().map(Order::getOrderNo).collect(Collectors.toList()));
        Map<Long, String> nickMap = userMapper.selectBatchIds(
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
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, orderNo));
        User user = userMapper.selectById(order.getUserId());
        return toAdminView(order, items, user != null ? user.getNickname() : null);
    }

    @Override
    public void ship(OrderShipRequest req) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, req.getNo()));
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() != 1) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅待发货订单可发货");
        }
        int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 2)
                .set(Order::getLogisticsCompany, req.getLogisticsCompany())
                .set(Order::getLogisticsNo, req.getLogisticsNo())
                .set(Order::getShippedAt, LocalDateTime.now())
                .eq(Order::getOrderNo, req.getNo())
                .eq(Order::getStatus, 1));
        if (affected == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminRefund(OrderRefundRequest req) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, req.getNo()));
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() != 3) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "仅已完成订单可手动退款");
        }
        // 3→5 售后中
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 5)
                .eq(Order::getOrderNo, req.getNo())
                .eq(Order::getStatus, 3));
        doRefund(order, req.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundForAfterSale(String orderNo, String reason) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        if (order.getStatus() != 5) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR.getCode(), "订单不在售后中，不可退款");
        }
        doRefund(order, reason);
    }

    // ---- 定时任务(7.4) ----

    @Override
    public int autoCancelTimeout() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> timedOut = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, 0)
                .lt(Order::getExpireAt, now));
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
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null || order.getStatus() != 0) {
            return;
        }
        releaseAssets(order, 4);
    }

    @Override
    public int autoConfirmReceived() {
        int days = configService.getInt("order", "auto_confirm_days", 7);
        LocalDateTime deadline = LocalDateTime.now().minusDays(days);
        List<Order> due = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, 2)
                .isNotNull(Order::getShippedAt)
                .lt(Order::getShippedAt, deadline));
        int count = 0;
        LocalDateTime now = LocalDateTime.now();
        for (Order order : due) {
            int affected = orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                    .set(Order::getStatus, 3)
                    .set(Order::getCompletedAt, now)
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .eq(Order::getStatus, 2));
            if (affected > 0) {
                count++;
            }
        }
        return count;
    }

    /** 退款处理(6.5)：调渠道 → payment=3 → 未过期券原路退回 → order 5→6 → 发事件。 */
    private void doRefund(Order order, String reason) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getOrderNo, order.getOrderNo())
                .eq(Payment::getStatus, 1));
        PaymentChannel channel = paymentChannelFactory.get(
                payment != null ? payment.getChannel() : configService.get("payment", "active_channel", "wechat"));
        RefundCommand cmd = new RefundCommand();
        cmd.setOrderNo(order.getOrderNo());
        cmd.setTransactionId(payment != null ? payment.getTransactionId() : null);
        cmd.setRefundAmount(order.getPayAmount());
        cmd.setTotalAmount(order.getPayAmount());
        cmd.setReason(reason);
        RefundResult result = channel.refund(cmd);
        if (!result.isSuccess()) {
            throw new BizException(ErrorCode.REFUND_FAILED);
        }
        if (payment != null) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .set(Payment::getStatus, 3)
                    .eq(Payment::getId, payment.getId()));
        }
        // 未过期券原路退回 → 0
        LocalDateTime now = LocalDateTime.now();
        List<OrderCoupon> ocs = orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .eq(OrderCoupon::getOrderNo, order.getOrderNo()));
        for (OrderCoupon oc : ocs) {
            userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .set(UserCoupon::getStatus, 0)
                    .set(UserCoupon::getUsedAt, null)
                    .set(UserCoupon::getLockOrderNo, null)
                    .eq(UserCoupon::getId, oc.getUserCouponId())
                    .gt(UserCoupon::getExpireAt, now));
        }
        // 5→6 已退款(终态)
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, 6)
                .eq(Order::getOrderNo, order.getOrderNo())
                .eq(Order::getStatus, 5));
        eventPublisher.publishEvent(new RefundedEvent(this, order.getOrderNo()));
    }

    /** 释放资产(6.4)：回滚库存 + 释放券 3→0 + order→targetStatus。 */
    private void releaseAssets(Order order, int targetStatus) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderNo, order.getOrderNo()));
        for (OrderItem item : items) {
            skuMapper.update(null, Wrappers.<ProductSku>lambdaUpdate()
                    .setSql("stock = stock + " + item.getQuantity())
                    .eq(ProductSku::getId, item.getSkuId()));
        }
        orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                        .eq(OrderCoupon::getOrderNo, order.getOrderNo()))
                .forEach(oc -> userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                        .set(UserCoupon::getStatus, 0)
                        .set(UserCoupon::getLockOrderNo, null)
                        .eq(UserCoupon::getId, oc.getUserCouponId())
                        .eq(UserCoupon::getStatus, 3)));
        orderMapper.update(null, Wrappers.<Order>lambdaUpdate()
                .set(Order::getStatus, targetStatus)
                .set(Order::getCanceledAt, LocalDateTime.now())
                .eq(Order::getOrderNo, order.getOrderNo())
                .eq(Order::getStatus, 0));
    }

    // ---- helpers ----

    private void checkPurchaseLimit(OrderSubmitRequest req, PricingContext ctx) {
        Set<Long> productIds = ctx.getSkus().stream().map(ProductSku::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : productMapper.selectBatchIds(productIds).stream()
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
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo));
        if (order == null || !order.getUserId().equals(userId)
                || (order.getUserDeleted() != null && order.getUserDeleted() == 1)) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return order;
    }

    private Map<String, List<OrderItem>> itemsByOrderNo(List<String> orderNos) {
        return orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderNo, orderNos))
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
        return DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }
}
