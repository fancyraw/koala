package com.koala.repository;

import com.koala.entity.CartItem;

import java.util.Collection;
import java.util.List;

public interface CartItemRepository {

    CartItem findById(Long id);

    /** 用户购物车,按 id 倒序。 */
    List<CartItem> findByUser(Long userId);

    CartItem findByUserAndSku(Long userId, Long skuId);

    void insert(CartItem item);

    void updateById(CartItem item);

    /** 删除指定用户名下的多条购物项。 */
    void deleteByUserAndIds(Long userId, Collection<Long> ids);
}
