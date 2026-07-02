package com.koala.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.dto.dashboard.DashboardView;
import com.koala.dto.dashboard.HotProduct;
import com.koala.dto.dashboard.Pending;
import com.koala.dto.dashboard.TodayOverview;
import com.koala.dto.dashboard.TrendPoint;
import com.koala.entity.Order;
import com.koala.entity.OrderItem;
import com.koala.entity.User;
import com.koala.mapper.AfterSaleMapper;
import com.koala.mapper.OrderItemMapper;
import com.koala.mapper.OrderMapper;
import com.koala.mapper.UserMapper;
import com.koala.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 看板聚合：Java 内聚合(单店数据量小),结果按 range 缓存 60s。 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String CACHE_KEY = "admin:dashboard:";
    private static final long CACHE_TTL_SECONDS = 60;
    private static final int HOT_LIMIT = 5;
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;
    private final AfterSaleMapper afterSaleMapper;
    private final StringRedisTemplate redis;

    public DashboardServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                                UserMapper userMapper, AfterSaleMapper afterSaleMapper,
                                StringRedisTemplate redis) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.userMapper = userMapper;
        this.afterSaleMapper = afterSaleMapper;
        this.redis = redis;
    }

    @Override
    public DashboardView overview(int rangeDays) {
        String cacheKey = CACHE_KEY + rangeDays;
        String cached = redis.opsForValue().get(cacheKey);
        if (cached != null) {
            return JSONUtil.toBean(cached, DashboardView.class);
        }
        DashboardView view = aggregate(rangeDays);
        redis.opsForValue().set(cacheKey, JSONUtil.toJsonStr(view), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        return view;
    }

    private DashboardView aggregate(int rangeDays) {
        LocalDate today = LocalDate.now();
        LocalDateTime windowStart = today.minusDays(rangeDays - 1L).atStartOfDay();

        // 窗口内已支付订单(以 paid_at 计),覆盖今日概览/趋势/热销
        List<Order> paidOrders = orderMapper.selectList(Wrappers.<Order>lambdaQuery()
                .isNotNull(Order::getPaidAt)
                .ge(Order::getPaidAt, windowStart));

        DashboardView view = new DashboardView();
        view.setToday(buildToday(today, paidOrders));
        view.setPending(buildPending());
        view.setSalesTrend(buildTrend(today, rangeDays, paidOrders));
        view.setHotProducts(buildHotProducts(paidOrders));
        return view;
    }

    private TodayOverview buildToday(LocalDate today, List<Order> paidOrders) {
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);

        List<Order> todayOrders = paidOrders.stream()
                .filter(o -> !o.getPaidAt().isBefore(todayStart))
                .collect(Collectors.toList());
        List<Order> yesterdayOrders = paidOrders.stream()
                .filter(o -> !o.getPaidAt().isBefore(yesterdayStart) && o.getPaidAt().isBefore(todayStart))
                .collect(Collectors.toList());

        BigDecimal todaySales = sumPay(todayOrders);
        BigDecimal yesterdaySales = sumPay(yesterdayOrders);
        long todayNewUsers = countNewUsers(todayStart, todayStart.plusDays(1));
        long yesterdayNewUsers = countNewUsers(yesterdayStart, todayStart);

        TodayOverview t = new TodayOverview();
        t.setSalesAmount(todaySales);
        t.setOrderCount(todayOrders.size());
        t.setNewUserCount(todayNewUsers);
        t.setSalesGrowthRate(growthRate(todaySales, yesterdaySales));
        t.setOrderGrowthRate(growthRate(BigDecimal.valueOf(todayOrders.size()),
                BigDecimal.valueOf(yesterdayOrders.size())));
        t.setUserGrowthRate(growthRate(BigDecimal.valueOf(todayNewUsers),
                BigDecimal.valueOf(yesterdayNewUsers)));
        return t;
    }

    private Pending buildPending() {
        Pending p = new Pending();
        p.setToShip(orderMapper.selectCount(Wrappers.<Order>lambdaQuery()
                .eq(Order::getStatus, 1)));
        // 需店主动手的售后:0待审核 / 2买家已寄回待确认收货
        p.setAfterSale(afterSaleMapper.selectCount(Wrappers.<com.koala.entity.AfterSale>lambdaQuery()
                .in(com.koala.entity.AfterSale::getStatus, 0, 2)));
        return p;
    }

    private List<TrendPoint> buildTrend(LocalDate today, int rangeDays, List<Order> paidOrders) {
        Map<String, BigDecimal> byDay = new LinkedHashMap<>();
        for (int i = rangeDays - 1; i >= 0; i--) {
            byDay.put(today.minusDays(i).format(DAY), BigDecimal.ZERO);
        }
        for (Order o : paidOrders) {
            String day = o.getPaidAt().toLocalDate().format(DAY);
            BigDecimal cur = byDay.get(day);
            if (cur != null) {
                byDay.put(day, cur.add(nvl(o.getPayAmount())));
            }
        }
        List<TrendPoint> list = new ArrayList<>(byDay.size());
        byDay.forEach((date, amount) -> {
            TrendPoint tp = new TrendPoint();
            tp.setDate(date);
            tp.setAmount(amount);
            list.add(tp);
        });
        return list;
    }

    private List<HotProduct> buildHotProducts(List<Order> paidOrders) {
        if (paidOrders.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> orderNos = paidOrders.stream().map(Order::getOrderNo).collect(Collectors.toList());
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderNo, orderNos));

        Map<Long, HotProduct> agg = new LinkedHashMap<>();
        for (OrderItem it : items) {
            HotProduct hp = agg.computeIfAbsent(it.getProductId(), pid -> {
                HotProduct n = new HotProduct();
                n.setProductId(pid);
                n.setProductName(it.getProductName());
                n.setQuantity(0);
                n.setAmount(BigDecimal.ZERO);
                return n;
            });
            hp.setQuantity(hp.getQuantity() + (it.getQuantity() != null ? it.getQuantity() : 0));
            hp.setAmount(hp.getAmount().add(nvl(it.getSubtotal())));
        }
        return agg.values().stream()
                .sorted(Comparator.comparingLong(HotProduct::getQuantity).reversed()
                        .thenComparing(hp -> hp.getAmount(), Comparator.reverseOrder()))
                .limit(HOT_LIMIT)
                .collect(Collectors.toList());
    }

    private long countNewUsers(LocalDateTime start, LocalDateTime end) {
        return userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreatedAt, start)
                .lt(User::getCreatedAt, end));
    }

    private static BigDecimal sumPay(List<Order> orders) {
        return orders.stream().map(o -> nvl(o.getPayAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 环比(%)：昨日为0返回null(前端显示"—")；否则(今-昨)/昨*100,保留1位。 */
    private static BigDecimal growthRate(BigDecimal todayVal, BigDecimal yesterdayVal) {
        if (yesterdayVal == null || yesterdayVal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return todayVal.subtract(yesterdayVal)
                .multiply(BigDecimal.valueOf(100))
                .divide(yesterdayVal, 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
