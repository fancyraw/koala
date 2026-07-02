package com.koala.dto.dashboard;

import lombok.Data;

import java.math.BigDecimal;

/** 销售趋势一日：日期(yyyy-MM-dd) + 当日销售额。 */
@Data
public class TrendPoint {

    private String date;
    private BigDecimal amount;
}
