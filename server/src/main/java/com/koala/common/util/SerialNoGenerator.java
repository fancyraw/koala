package com.koala.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;

import java.util.Date;

/**
 * 统一的单号生成：`{prefix}yyyyMMddHHmmss{6位随机数字}`。
 * 前缀在下方常量定义，保证订单号、售后单号规则一致。
 */
public final class SerialNoGenerator {

    /** 订单号前缀（空串，保留纯 20 位数字，兼容既有历史）。 */
    public static final String ORDER_PREFIX = "";

    /** 售后单号前缀。 */
    public static final String AFTER_SALE_PREFIX = "AS";

    private SerialNoGenerator() {}

    public static String next(String prefix) {
        return prefix + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(6);
    }
}
