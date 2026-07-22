package com.koala.service.order;

import com.koala.common.constant.ConfigKeys;
import com.koala.common.constant.PaymentChannels;
import com.koala.common.constant.RedisKeys;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.util.SerialNoGenerator;
import com.koala.dto.order.OrderItemRequest;
import com.koala.dto.order.OrderItemView;
import com.koala.dto.order.OrderSubmitRequest;
import com.koala.dto.order.OrderSubmitView;
import com.koala.service.ConfigService;
import com.koala.service.PriceService;
import com.koala.bo.PriceResult;
import com.koala.bo.PricingContext;
import com.koala.entity.Order;
import com.koala.entity.OrderCoupon;
import com.koala.entity.OrderItem;
import com.koala.entity.Payment;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.entity.UserAddress;
import com.koala.entity.UserCoupon;
import com.koala.enums.OrderStatus;
import com.koala.enums.PaymentStatus;
import com.koala.enums.ValidFlag;
import com.koala.event.OrderPaidEvent;
import com.koala.repository.CouponRepository;
import com.koala.repository.OrderCouponRepository;
import com.koala.repository.OrderItemRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.PaymentRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.repository.UserAddressRepository;
import com.koala.repository.UserCouponRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 订单提交：submitToken SETNX + Redisson 用户级锁 + 事务化 doSubmit（扣库存 / 建 order / 锁券 / 建 payment）。
 * 零元订单在提交事务里直接推进到已支付流程。
 */
@Service
public class OrderSubmitService {

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
    private final PriceService priceService;
    private final ConfigService configService;
    private final RedissonClient redisson;
    private final StringRedisTemplate redis;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** 自注入代理：锁内调用事务方法需走代理，否则 @Transactional 失效。 */
    @Autowired
    @Lazy
    private OrderSubmitService self;

    public OrderSubmitService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                              OrderCouponRepository orderCouponRepository, PaymentRepository paymentRepository,
                              ProductSkuRepository skuRepository, ProductRepository productRepository,
                              CouponRepository couponRepository, UserCouponRepository userCouponRepository,
                              UserAddressRepository addressRepository, PriceService priceService,
                              ConfigService configService, RedissonClient redisson,
                              StringRedisTemplate redis, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderCouponRepository = orderCouponRepository;
        this.paymentRepository = paymentRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.addressRepository = addressRepository;
        this.priceService = priceService;
        this.configService = configService;
        this.redisson = redisson;
        this.redis = redis;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

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
        LocalDateTime now = LocalDateTime.now(clock);
        int timeoutMinutes = configService.getInt(ConfigKeys.Order.GROUP, ConfigKeys.Order.PAY_TIMEOUT_MINUTES, 30);
        String orderNo = SerialNoGenerator.next(SerialNoGenerator.ORDER_PREFIX);

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
}
