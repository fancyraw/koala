package com.koala.service;

import com.koala.dto.content.HomeView;
import com.koala.dto.coupon.GrantResultView;
import com.koala.service.BannerService;
import com.koala.service.CategoryService;
import com.koala.service.CouponService;
import com.koala.service.ProductService;
import org.springframework.stereotype.Service;

@Service
public class HomeService {

    private static final int HOT_LIMIT = 10;
    private static final int RECOMMEND_LIMIT = 6;

    private final BannerService bannerService;
    private final CategoryService categoryService;
    private final ProductService productService;
    private final CouponService couponService;

    public HomeService(BannerService bannerService, CategoryService categoryService,
                           ProductService productService, CouponService couponService) {
        this.bannerService = bannerService;
        this.categoryService = categoryService;
        this.productService = productService;
        this.couponService = couponService;
    }

    public HomeView home(Long userId) {
        HomeView view = new HomeView();
        view.setBanners(bannerService.listValid());
        view.setCategories(categoryService.listValid());
        view.setHotSelling(productService.hotSelling(HOT_LIMIT));
        view.setRecommended(productService.recommended(RECOMMEND_LIMIT));
        // 进首页自动下发可领券(幂等)，返回本次新到账券供领券弹窗
        GrantResultView grant = couponService.autoGrant(userId);
        view.setGrantedCoupons(grant.getCoupons());
        return view;
    }
}
