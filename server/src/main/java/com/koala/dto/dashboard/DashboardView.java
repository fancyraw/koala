package com.koala.dto.dashboard;

import lombok.Data;

import java.util.List;

/** 数据看板聚合：今日概览 + 待处理 + 销售趋势 + 热销Top5。见 7.3(60s缓存)。 */
@Data
public class DashboardView {

    private TodayOverviewView today;
    private PendingView pending;
    private List<TrendPointView> salesTrend;
    private List<HotProductView> hotProducts;
}
