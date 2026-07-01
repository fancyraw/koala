package com.koala.dto.content;

import com.koala.dto.coupon.UserCouponView;
import com.koala.dto.product.CategoryView;
import com.koala.dto.product.ProductCardView;
import lombok.Data;

import java.util.List;

/** 首页聚合：Banner + 品类 + 热销 + 推荐 + 本次自动下发的可领券。 */
@Data
public class HomeView {

    private List<BannerView> banners;
    private List<CategoryView> categories;
    private List<ProductCardView> hotSelling;
    private List<ProductCardView> recommended;
    /** 本次进首页自动下发到账的券(供领券弹窗)。 */
    private List<UserCouponView> grantedCoupons;
}
