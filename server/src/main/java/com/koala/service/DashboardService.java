package com.koala.service;

import com.koala.dto.dashboard.DashboardView;

public interface DashboardService {

    /** 看板聚合(60s Redis 缓存)。range=趋势天数窗口(7 或 30)。 */
    DashboardView overview(int rangeDays);
}
