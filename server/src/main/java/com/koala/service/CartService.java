package com.koala.service;

import com.koala.dto.cart.CartAddRequest;
import com.koala.dto.cart.CartUpdateRequest;
import com.koala.dto.cart.CartView;

import java.util.List;

public interface CartService {

    /** 查询购物车（价格/库存实时，失效/售罄派生），合计仅含勾选有效行。 */
    CartView list(Long userId);

    /** 加购：同 SKU 累加；校验上架、库存、单次限购。返回最新购物车。 */
    CartView add(Long userId, CartAddRequest req);

    /** 更新数量/勾选；改数量时校验库存与限购。返回最新购物车。 */
    CartView update(Long userId, CartUpdateRequest req);

    /** 删除行（支持批量），仅删除归属当前用户的行。返回最新购物车。 */
    CartView remove(Long userId, List<Long> ids);
}
