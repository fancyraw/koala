package com.koala.service;

import com.koala.dto.content.HomeView;

public interface HomeService {

    /** 首页聚合：Banner+品类+热销+推荐 + 顺带自动下发可领券(幂等)。 */
    HomeView home(Long userId);
}
