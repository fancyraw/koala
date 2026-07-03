package com.koala.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.koala.entity.CartItem;
import com.koala.mapper.CartItemMapper;
import com.koala.repository.CartItemRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public class CartItemRepositoryImpl implements CartItemRepository {

    private final CartItemMapper cartMapper;

    public CartItemRepositoryImpl(CartItemMapper cartMapper) {
        this.cartMapper = cartMapper;
    }

    @Override
    public CartItem findById(Long id) {
        return cartMapper.selectById(id);
    }

    @Override
    public java.util.List<CartItem> findByUser(Long userId) {
        return cartMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .orderByDesc(CartItem::getId));
    }

    @Override
    public CartItem findByUserAndSku(Long userId, Long skuId) {
        return cartMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .eq(CartItem::getSkuId, skuId));
    }

    @Override
    public void insert(CartItem item) {
        cartMapper.insert(item);
    }

    @Override
    public void updateById(CartItem item) {
        cartMapper.updateById(item);
    }

    @Override
    public void deleteByUserAndIds(Long userId, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        cartMapper.delete(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId)
                .in(CartItem::getId, ids));
    }
}
