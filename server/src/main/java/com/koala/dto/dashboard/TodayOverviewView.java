package com.koala.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

/** 今日概览：销售额/订单/新增用户,各带较昨日环比(%)。 */
@Data
public class TodayOverviewView {

    private BigDecimal salesAmount;
    private long orderCount;
    private long newUserCount;

    /** 环比昨日(%)，正=增长负=下降；昨日为0时为null(前端显示"—")。 */
    private BigDecimal salesGrowthRate;
    private BigDecimal orderGrowthRate;
    private BigDecimal userGrowthRate;
}
