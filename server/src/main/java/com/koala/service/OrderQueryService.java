package com.koala.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.common.result.PageResult;
import com.koala.dto.order.AdminOrderView;
import com.koala.dto.order.OrderItemView;
import com.koala.dto.order.OrderPreviewRequest;
import com.koala.dto.order.OrderPreviewView;
import com.koala.dto.order.OrderView;
import com.koala.dto.order.PriceResult;
import com.koala.dto.order.PricingContext;
import com.koala.entity.Order;
import com.koala.entity.OrderItem;
import com.koala.entity.User;
import com.koala.entity.UserAddress;
import com.koala.enums.ValidFlag;
import com.koala.repository.OrderItemRepository;
import com.koala.repository.OrderRepository;
import com.koala.repository.UserAddressRepository;
import com.koala.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单查询：C 端 preview / myOrders / detail 与后台 adminList / adminDetail。
 * 只读逻辑集中在这里；跨 Service 共用的 requireOwnedOrder 也放在此处对外暴露。
 */
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PriceService priceService;

    public OrderQueryService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                             UserAddressRepository addressRepository, UserRepository userRepository,
                             PriceService priceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.priceService = priceService;
    }

    public OrderPreviewView preview(Long userId, OrderPreviewRequest req) {
        PricingContext ctx = priceService.calculate(userId, req.getItems(), true);
        OrderPreviewView view = new OrderPreviewView();
        view.setItems(ctx.getItems());
        PriceResult p = ctx.getPrice();
        view.setProductAmount(p.getProductAmount());
        view.setCouponDiscount(p.getCouponDiscount());
        view.setShippingFee(p.getShippingFee());
        view.setPayAmount(p.getPayAmount());
        view.setAppliedCoupons(p.getAppliedCoupons());
        view.setUpsell(ctx.getUpsell());
        if (req.getAddressId() != null) {
            UserAddress addr = addressRepository.findById(req.getAddressId());
            if (addr != null && addr.getUserId().equals(userId)) {
                view.setAddressId(addr.getId());
                view.setReceiverName(addr.getName());
                view.setReceiverPhone(addr.getPhone());
                view.setReceiverAddress(addr.getFullAddress());
            }
        }
        return view;
    }

    public PageResult<OrderView> myOrders(Long userId, Integer status, long page, long size) {
        IPage<Order> p = orderRepository.pageByUser(userId, status, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNo(
                p.getRecords().stream().map(Order::getOrderNo).collect(Collectors.toList()));
        List<OrderView> list = p.getRecords().stream()
                .map(o -> toView(o, itemMap.getOrDefault(o.getOrderNo(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public OrderView detail(Long userId, String orderNo) {
        Order order = requireOwnedOrder(userId, orderNo);
        return toView(order, orderItemRepository.findByOrderNo(orderNo));
    }

    public PageResult<AdminOrderView> adminList(String keyword, Integer status, long page, long size) {
        final String kw = keyword != null ? keyword.trim() : null;
        Set<Long> matchedUserIds = Collections.emptySet();
        if (kw != null && !kw.isEmpty()) {
            matchedUserIds = new java.util.HashSet<>(userRepository.findIdsByNicknameLike(kw));
        }
        IPage<Order> p = orderRepository.pageForAdmin(status, kw, matchedUserIds, page, size);
        if (p.getRecords().isEmpty()) {
            return PageResult.empty(page, size);
        }
        Map<String, List<OrderItem>> itemMap = itemsByOrderNo(
                p.getRecords().stream().map(Order::getOrderNo).collect(Collectors.toList()));
        Map<Long, String> nickMap = userRepository.findByIds(
                        p.getRecords().stream().map(Order::getUserId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getNickname));
        List<AdminOrderView> list = p.getRecords().stream()
                .map(o -> toAdminView(o, itemMap.getOrDefault(o.getOrderNo(), Collections.emptyList()),
                        nickMap.get(o.getUserId())))
                .collect(Collectors.toList());
        return new PageResult<>(list, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AdminOrderView adminDetail(String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        List<OrderItem> items = orderItemRepository.findByOrderNo(orderNo);
        User user = userRepository.findById(order.getUserId());
        return toAdminView(order, items, user != null ? user.getNickname() : null);
    }

    /** 校验订单归属并返回实体；用户软删的订单不再对本人可见。跨 Service 共享的守卫。 */
    public Order requireOwnedOrder(Long userId, String orderNo) {
        Order order = orderRepository.findByNo(orderNo);
        if (order == null || !order.getUserId().equals(userId)
                || ValidFlag.ENABLED.is(order.getUserDeleted())) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return order;
    }

    private Map<String, List<OrderItem>> itemsByOrderNo(List<String> orderNos) {
        return orderItemRepository.findByOrderNos(orderNos)
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderNo));
    }

    private OrderView toView(Order o, List<OrderItem> items) {
        OrderView v = new OrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setExpireAt(o.getExpireAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(this::toItemView).collect(Collectors.toList()));
        return v;
    }

    private AdminOrderView toAdminView(Order o, List<OrderItem> items, String nickname) {
        AdminOrderView v = new AdminOrderView();
        v.setOrderNo(o.getOrderNo());
        v.setUserId(o.getUserId());
        v.setNickname(nickname);
        v.setReceiverName(o.getReceiverName());
        v.setReceiverPhone(o.getReceiverPhone());
        v.setReceiverAddress(o.getReceiverAddress());
        v.setProductAmount(o.getProductAmount());
        v.setCouponDiscount(o.getCouponDiscount());
        v.setShippingFee(o.getShippingFee());
        v.setPayAmount(o.getPayAmount());
        v.setLogisticsCompany(o.getLogisticsCompany());
        v.setLogisticsNo(o.getLogisticsNo());
        v.setStatus(o.getStatus());
        v.setRemark(o.getRemark());
        v.setPaidAt(o.getPaidAt());
        v.setShippedAt(o.getShippedAt());
        v.setCompletedAt(o.getCompletedAt());
        v.setCanceledAt(o.getCanceledAt());
        v.setCreatedAt(o.getCreatedAt());
        v.setItems(items.stream().map(this::toItemView).collect(Collectors.toList()));
        return v;
    }

    private OrderItemView toItemView(OrderItem item) {
        OrderItemView v = new OrderItemView();
        v.setProductId(item.getProductId());
        v.setSkuId(item.getSkuId());
        v.setProductName(item.getProductName());
        v.setSkuName(item.getSkuName());
        v.setProductImage(item.getProductImage());
        v.setUnitPrice(item.getUnitPrice());
        v.setQuantity(item.getQuantity());
        v.setSubtotal(item.getSubtotal());
        return v;
    }
}
