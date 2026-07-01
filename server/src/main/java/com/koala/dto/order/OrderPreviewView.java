package com.koala.dto.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 下单预览：算价明细 + 商品行 + 收货地址回填 + 凑单提示。 */
@Data
public class OrderPreviewView {

    private List<OrderItemView> items;
    private BigDecimal productAmount;
    private BigDecimal couponDiscount;
    private BigDecimal shippingFee;
    private BigDecimal payAmount;
    private List<PriceResult.AppliedCoupon> appliedCoupons;

    /** 回填的收货地址(无则空)。 */
    private Long addressId;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    /** 凑单提示：再买 ¥needMore 可用该满减券多省 ¥extraSave(无则空)。 */
    private UpsellHint upsell;

    @Data
    public static class UpsellHint {
        private Long couponId;
        private String couponName;
        private BigDecimal needMore;
        private BigDecimal extraSave;
    }
}
