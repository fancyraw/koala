package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.order.OrderItemRequest;
import com.koala.dto.order.PricingContext;
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
import com.koala.service.impl.PriceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

    @Mock
    private ProductSkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private ConfigService configService;

    @InjectMocks
    private PriceServiceImpl service;

    @BeforeEach
    void stubShipping() {
        // base_fee=8, free_threshold=99（与生产默认一致）
        lenient().when(configService.getDecimal(eq("shipping"), eq("base_fee"), any()))
                .thenReturn(new BigDecimal("8"));
        lenient().when(configService.getDecimal(eq("shipping"), eq("free_threshold"), any()))
                .thenReturn(new BigDecimal("99"));
    }

    private ProductSku sku(long id, long productId, String price) {
        ProductSku s = new ProductSku();
        s.setId(id);
        s.setProductId(productId);
        s.setName("规格");
        s.setPrice(new BigDecimal(price));
        s.setStock(999);
        return s;
    }

    private Product product(long id, boolean onSale) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品");
        p.setMainImage("img");
        p.setIsValid(onSale ? ValidFlag.ENABLED.code() : ValidFlag.DISABLED.code());
        return p;
    }

    private OrderItemRequest item(long skuId, int qty) {
        OrderItemRequest r = new OrderItemRequest();
        r.setSkuId(skuId);
        r.setQuantity(qty);
        return r;
    }

    private UserCoupon userCoupon(long id, long couponId) {
        UserCoupon uc = new UserCoupon();
        uc.setId(id);
        uc.setCouponId(couponId);
        return uc;
    }

    private Coupon noThreshold(long id, String amount) {
        Coupon c = new Coupon();
        c.setId(id);
        c.setName("无门槛券");
        c.setType(CouponType.NO_THRESHOLD.code());
        c.setDiscountAmount(new BigDecimal(amount));
        return c;
    }

    private Coupon fullReduce(long id, String threshold, String amount) {
        Coupon c = new Coupon();
        c.setId(id);
        c.setName("满减券");
        c.setType(CouponType.FULL_REDUCE.code());
        c.setMinSpend(new BigDecimal(threshold));
        c.setDiscountAmount(new BigDecimal(amount));
        return c;
    }

    private void stubItems(ProductSku sku, Product product) {
        when(skuRepository.findByIds(anySet())).thenReturn(Collections.singletonList(sku));
        when(productRepository.findByIds(anySet())).thenReturn(Collections.singletonList(product));
    }

    @Test
    void calculate_skuMissing_throws() {
        when(skuRepository.findByIds(anySet())).thenReturn(Collections.emptyList());
        lenient().when(productRepository.findByIds(anySet())).thenReturn(Collections.emptyList());
        lenient().when(userCouponRepository.findUsableByUser(eq(1L), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.calculate(1L, Collections.singletonList(item(9L, 1)), false))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
    }

    @Test
    void calculate_offSale_throws() {
        stubItems(sku(9L, 100L, "10.00"), product(100L, false));
        lenient().when(userCouponRepository.findUsableByUser(eq(1L), any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.calculate(1L, Collections.singletonList(item(9L, 1)), false))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.BIZ_ERROR.getCode()));
    }

    @Test
    void calculate_noCoupons_underFreeThreshold_addsShipping() {
        stubItems(sku(9L, 100L, "10.00"), product(100L, true));
        when(userCouponRepository.findUsableByUser(eq(1L), any())).thenReturn(Collections.emptyList());

        PricingContext ctx = service.calculate(1L, Collections.singletonList(item(9L, 2)), false);

        // 20 商品 + 8 运费，无券
        assertThat(ctx.getPrice().getProductAmount()).isEqualByComparingTo("20.00");
        assertThat(ctx.getPrice().getCouponDiscount()).isEqualByComparingTo("0");
        assertThat(ctx.getPrice().getShippingFee()).isEqualByComparingTo("8");
        assertThat(ctx.getPrice().getPayAmount()).isEqualByComparingTo("28.00");
    }

    @Test
    void calculate_overFreeThreshold_noShipping() {
        stubItems(sku(9L, 100L, "50.00"), product(100L, true));
        when(userCouponRepository.findUsableByUser(eq(1L), any())).thenReturn(Collections.emptyList());

        PricingContext ctx = service.calculate(1L, Collections.singletonList(item(9L, 2)), false);

        // 100 >= 99 免运费
        assertThat(ctx.getPrice().getShippingFee()).isEqualByComparingTo("0");
        assertThat(ctx.getPrice().getPayAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculate_combinesNoThresholdAndFullReduce_pickingBest() {
        stubItems(sku(9L, 100L, "50.00"), product(100L, true));
        // 商品合计 100：无门槛券取面额最大(15)，满减券取门槛≤100且面额最大(20)
        List<UserCoupon> usable = java.util.Arrays.asList(
                userCoupon(1L, 11L), userCoupon(2L, 12L), userCoupon(3L, 13L));
        when(userCouponRepository.findUsableByUser(eq(1L), any())).thenReturn(usable);
        when(couponRepository.findByIds(anySet())).thenReturn(java.util.Arrays.asList(
                noThreshold(11L, "15"),
                fullReduce(12L, "80", "20"),
                fullReduce(13L, "200", "50") // 门槛超过合计，不可用
        ));

        PricingContext ctx = service.calculate(1L, Collections.singletonList(item(9L, 2)), false);

        // 折扣 15 + 20 = 35；100 - 35 = 65（免运费）
        assertThat(ctx.getPrice().getCouponDiscount()).isEqualByComparingTo("35");
        assertThat(ctx.getPrice().getPayAmount()).isEqualByComparingTo("65.00");
        assertThat(ctx.getAppliedUserCoupons()).hasSize(2);
    }

    @Test
    void calculate_discountCappedAtProductAmount_payFloorApplies() {
        stubItems(sku(9L, 100L, "5.00"), product(100L, true));
        // 商品合计 5，券 20 → 折扣封顶到 5，实付 = 5-5+8运费 = 8
        when(userCouponRepository.findUsableByUser(eq(1L), any()))
                .thenReturn(Collections.singletonList(userCoupon(1L, 11L)));
        when(couponRepository.findByIds(anySet()))
                .thenReturn(Collections.singletonList(noThreshold(11L, "20")));

        PricingContext ctx = service.calculate(1L, Collections.singletonList(item(9L, 1)), false);

        assertThat(ctx.getPrice().getCouponDiscount()).isEqualByComparingTo("5.00");
        assertThat(ctx.getPrice().getShippingFee()).isEqualByComparingTo("8");
        assertThat(ctx.getPrice().getPayAmount()).isEqualByComparingTo("8.00");
    }

    @Test
    void calculate_upsellHint_suggestsUnreachedFullReduce() {
        stubItems(sku(9L, 100L, "50.00"), product(100L, true));
        // 合计 50，满减券门槛 80/减 20 未达标 → 凑单提示
        when(userCouponRepository.findUsableByUser(eq(1L), any()))
                .thenReturn(Collections.singletonList(userCoupon(1L, 12L)));
        when(couponRepository.findByIds(anySet()))
                .thenReturn(Collections.singletonList(fullReduce(12L, "80", "20")));

        PricingContext ctx = service.calculate(1L, Collections.singletonList(item(9L, 1)), true);

        assertThat(ctx.getUpsell()).isNotNull();
        assertThat(ctx.getUpsell().getNeedMore()).isEqualByComparingTo("30"); // 80 - 50
        assertThat(ctx.getUpsell().getExtraSave()).isEqualByComparingTo("20");
    }
}
