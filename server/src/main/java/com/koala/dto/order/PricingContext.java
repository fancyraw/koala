package com.koala.dto.order;

import com.koala.entity.ProductSku;
import com.koala.entity.UserCoupon;
import lombok.Data;

import java.util.List;

/** 算价上下文：预览与下单共用，携带解析后的商品项快照、价格结果与选中券实体。 */
@Data
public class PricingContext {

    private List<OrderItemView> items;
    private List<ProductSku> skus;
    private PriceResult price;
    /** 本次选中锁定的用户券实体(无门槛/满减各≤1)。 */
    private List<UserCoupon> appliedUserCoupons;
    /** 凑单提示(仅预览需要)。 */
    private OrderPreviewView.UpsellHint upsell;
}
