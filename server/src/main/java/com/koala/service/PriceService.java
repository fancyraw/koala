package com.koala.service;

import com.koala.common.constant.ConfigKeys;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.converter.CouponConverter;
import com.koala.dto.order.OrderItemRequest;
import com.koala.dto.order.OrderItemView;
import com.koala.dto.order.OrderPreviewView;
import com.koala.service.pricing.PriceResult;
import com.koala.service.pricing.PricingContext;
import com.koala.entity.Coupon;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.entity.UserCoupon;
import com.koala.enums.CouponType;
import com.koala.enums.ValidFlag;
import com.koala.repository.CouponRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.repository.UserCouponRepository;
import com.koala.service.ConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PriceService {

    private final ProductSkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final ConfigService configService;
    private final Clock clock;

    public PriceService(ProductSkuRepository skuRepository, ProductRepository productRepository,
                            CouponRepository couponRepository, UserCouponRepository userCouponRepository,
                            ConfigService configService, Clock clock) {
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.couponRepository = couponRepository;
        this.userCouponRepository = userCouponRepository;
        this.configService = configService;
        this.clock = clock;
    }

    public PricingContext calculate(Long userId, List<OrderItemRequest> items, boolean withUpsell) {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1. 解析商品项 + 商品合计
        Set<Long> skuIds = items.stream().map(OrderItemRequest::getSkuId).collect(Collectors.toSet());
        Map<Long, ProductSku> skuMap = skuRepository.findByIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSku::getId, s -> s));
        Set<Long> productIds = skuMap.values().stream().map(ProductSku::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? java.util.Collections.emptyMap()
                : productRepository.findByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderItemView> itemViews = new ArrayList<>();
        List<ProductSku> orderedSkus = new ArrayList<>();
        BigDecimal productAmount = BigDecimal.ZERO;
        for (OrderItemRequest req : items) {
            ProductSku sku = skuMap.get(req.getSkuId());
            if (sku == null) {
                throw new BizException(ErrorCode.DATA_NOT_FOUND.getCode(), "商品规格不存在");
            }
            Product product = productMap.get(sku.getProductId());
            if (product == null || !ValidFlag.isEnabled(product.getIsValid())) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "商品已下架");
            }
            BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(req.getQuantity()));
            productAmount = productAmount.add(subtotal);

            OrderItemView v = new OrderItemView();
            v.setProductId(product.getId());
            v.setSkuId(sku.getId());
            v.setProductName(product.getName());
            v.setSkuName(sku.getName());
            v.setProductImage(product.getMainImage());
            v.setUnitPrice(sku.getPrice());
            v.setQuantity(req.getQuantity());
            v.setSubtotal(subtotal);
            itemViews.add(v);
            orderedSkus.add(sku);
        }

        // 2. 券组合最优：无门槛(面额最大) + 满减(门槛≤合计中面额最大)
        List<UserCoupon> usable = userCouponRepository.findUsableByUser(userId, now);
        Map<Long, Coupon> couponMap = usable.isEmpty() ? java.util.Collections.emptyMap()
                : couponRepository.findByIds(usable.stream().map(UserCoupon::getCouponId).collect(Collectors.toSet()))
                        .stream().collect(Collectors.toMap(Coupon::getId, c -> c));

        UserCoupon bestNoThreshold = null;
        BigDecimal bestNoThresholdVal = BigDecimal.ZERO;
        UserCoupon bestFullReduce = null;
        BigDecimal bestFullReduceVal = BigDecimal.ZERO;
        for (UserCoupon uc : usable) {
            Coupon c = couponMap.get(uc.getCouponId());
            if (c == null) {
                continue;
            }
            if (CouponType.NO_THRESHOLD.is(c.getType())) {
                if (c.getDiscountAmount().compareTo(bestNoThresholdVal) > 0) {
                    bestNoThresholdVal = c.getDiscountAmount();
                    bestNoThreshold = uc;
                }
            } else if (CouponType.FULL_REDUCE.is(c.getType())) {
                BigDecimal threshold = c.getMinSpend() != null ? c.getMinSpend() : BigDecimal.ZERO;
                if (productAmount.compareTo(threshold) >= 0
                        && c.getDiscountAmount().compareTo(bestFullReduceVal) > 0) {
                    bestFullReduceVal = c.getDiscountAmount();
                    bestFullReduce = uc;
                }
            }
        }

        List<UserCoupon> applied = new ArrayList<>();
        List<PriceResult.AppliedCoupon> appliedViews = new ArrayList<>();
        BigDecimal rawDiscount = BigDecimal.ZERO;
        if (bestNoThreshold != null) {
            applied.add(bestNoThreshold);
            appliedViews.add(CouponConverter.appliedView(bestNoThreshold, couponMap.get(bestNoThreshold.getCouponId())));
            rawDiscount = rawDiscount.add(bestNoThresholdVal);
        }
        if (bestFullReduce != null) {
            applied.add(bestFullReduce);
            appliedViews.add(CouponConverter.appliedView(bestFullReduce, couponMap.get(bestFullReduce.getCouponId())));
            rawDiscount = rawDiscount.add(bestFullReduceVal);
        }
        // 券后小计下限 0
        BigDecimal couponDiscount = rawDiscount.min(productAmount);

        // 3. 运费：包邮门槛以商品合计判断，运费不参与券抵扣
        BigDecimal baseFee = configService.getDecimal(ConfigKeys.Shipping.GROUP, ConfigKeys.Shipping.BASE_FEE, new BigDecimal("8"));
        BigDecimal freeThreshold = configService.getDecimal(ConfigKeys.Shipping.GROUP, ConfigKeys.Shipping.FREE_THRESHOLD, new BigDecimal("99"));
        BigDecimal shippingFee = productAmount.compareTo(freeThreshold) >= 0 ? BigDecimal.ZERO : baseFee;

        // 4. 实付 = 商品合计 − 券抵扣 + 运费。允许 0：满券叠免邮时 OrderService 会走零元自动完成路径。
        BigDecimal payAmount = productAmount.subtract(couponDiscount).add(shippingFee);
        if (payAmount.signum() < 0) {
            payAmount = BigDecimal.ZERO;
        }

        PriceResult price = new PriceResult();
        price.setProductAmount(productAmount);
        price.setCouponDiscount(couponDiscount);
        price.setShippingFee(shippingFee);
        price.setPayAmount(payAmount);
        price.setAppliedCoupons(appliedViews);

        PricingContext ctx = new PricingContext();
        ctx.setItems(itemViews);
        ctx.setSkus(orderedSkus);
        ctx.setPrice(price);
        ctx.setAppliedUserCoupons(applied);

        // 凑单提示：持有但未达门槛的满减券，取补足差额后净增优惠最大的一张
        if (withUpsell) {
            ctx.setUpsell(computeUpsell(usable, couponMap, productAmount, bestFullReduceVal));
        }
        return ctx;
    }

    private OrderPreviewView.UpsellHint computeUpsell(List<UserCoupon> usable, Map<Long, Coupon> couponMap,
                                                      BigDecimal productAmount, BigDecimal currentFullReduce) {
        OrderPreviewView.UpsellHint best = null;
        BigDecimal bestExtra = BigDecimal.ZERO;
        for (UserCoupon uc : usable) {
            Coupon c = couponMap.get(uc.getCouponId());
            if (c == null || !CouponType.FULL_REDUCE.is(c.getType())) {
                continue;
            }
            BigDecimal threshold = c.getMinSpend() != null ? c.getMinSpend() : BigDecimal.ZERO;
            if (productAmount.compareTo(threshold) >= 0) {
                continue; // 已达门槛，非凑单目标
            }
            BigDecimal extraSave = c.getDiscountAmount().subtract(currentFullReduce);
            if (extraSave.compareTo(bestExtra) > 0) {
                bestExtra = extraSave;
                OrderPreviewView.UpsellHint hint = new OrderPreviewView.UpsellHint();
                hint.setCouponId(c.getId());
                hint.setCouponName(c.getName());
                hint.setNeedMore(threshold.subtract(productAmount));
                hint.setExtraSave(extraSave);
                best = hint;
            }
        }
        return best;
    }

}
