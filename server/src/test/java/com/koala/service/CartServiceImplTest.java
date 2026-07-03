package com.koala.service;

import com.koala.common.exception.BizException;
import com.koala.common.result.ErrorCode;
import com.koala.dto.cart.CartAddRequest;
import com.koala.dto.cart.CartView;
import com.koala.entity.CartItem;
import com.koala.entity.Product;
import com.koala.entity.ProductSku;
import com.koala.enums.ValidFlag;
import com.koala.repository.CartItemRepository;
import com.koala.repository.ProductRepository;
import com.koala.repository.ProductSkuRepository;
import com.koala.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartItemRepository cartRepository;
    @Mock
    private ProductSkuRepository skuRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl service;

    private ProductSku sku(long id, long productId, String price, int stock) {
        ProductSku s = new ProductSku();
        s.setId(id);
        s.setProductId(productId);
        s.setName("规格");
        s.setPrice(new BigDecimal(price));
        s.setStock(stock);
        return s;
    }

    private Product product(long id, boolean onSale, Integer perOrderLimit) {
        Product p = new Product();
        p.setId(id);
        p.setName("商品");
        p.setMainImage("img");
        p.setIsValid(onSale ? ValidFlag.ENABLED.code() : ValidFlag.DISABLED.code());
        p.setPerOrderLimit(perOrderLimit);
        return p;
    }

    private CartAddRequest addReq(long skuId, int qty) {
        CartAddRequest req = new CartAddRequest();
        req.setSkuId(skuId);
        req.setQuantity(qty);
        return req;
    }

    @Test
    void add_skuNotFound_throwsDataNotFound() {
        when(skuRepository.findById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.add(1L, addReq(9L, 1)))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode()));
        verify(cartRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void add_offSaleProduct_rejected() {
        when(skuRepository.findById(9L)).thenReturn(sku(9L, 100L, "10.00", 50));
        when(productRepository.findById(100L)).thenReturn(product(100L, false, null));

        assertThatThrownBy(() -> service.add(1L, addReq(9L, 1)))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.BIZ_ERROR.getCode()));
    }

    @Test
    void add_exceedStock_rejected() {
        when(skuRepository.findById(9L)).thenReturn(sku(9L, 100L, "10.00", 2));
        when(productRepository.findById(100L)).thenReturn(product(100L, true, null));
        when(cartRepository.findByUserAndSku(1L, 9L)).thenReturn(null);

        assertThatThrownBy(() -> service.add(1L, addReq(9L, 3)))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.STOCK_NOT_ENOUGH.getCode()));
    }

    @Test
    void add_exceedPerOrderLimit_rejected() {
        when(skuRepository.findById(9L)).thenReturn(sku(9L, 100L, "10.00", 100));
        when(productRepository.findById(100L)).thenReturn(product(100L, true, 2));
        when(cartRepository.findByUserAndSku(1L, 9L)).thenReturn(null);

        assertThatThrownBy(() -> service.add(1L, addReq(9L, 3)))
                .satisfies(e -> assertThat(((BizException) e).getCode())
                        .isEqualTo(ErrorCode.PURCHASE_LIMIT.getCode()));
    }

    @Test
    void add_existingItem_accumulatesQuantity() {
        when(skuRepository.findById(9L)).thenReturn(sku(9L, 100L, "10.00", 100));
        when(productRepository.findById(100L)).thenReturn(product(100L, true, null));
        CartItem existing = new CartItem();
        existing.setId(7L);
        existing.setUserId(1L);
        existing.setSkuId(9L);
        existing.setQuantity(2);
        when(cartRepository.findByUserAndSku(1L, 9L)).thenReturn(existing);
        // list() 回查购物车
        when(cartRepository.findByUser(1L)).thenReturn(Collections.emptyList());

        service.add(1L, addReq(9L, 3));

        verify(cartRepository).updateById(existing);
        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(existing.getChecked()).isEqualTo(ValidFlag.ENABLED.code());
        verify(cartRepository, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void add_newItem_inserts() {
        when(skuRepository.findById(9L)).thenReturn(sku(9L, 100L, "10.00", 100));
        when(productRepository.findById(100L)).thenReturn(product(100L, true, null));
        when(cartRepository.findByUserAndSku(1L, 9L)).thenReturn(null);
        when(cartRepository.findByUser(1L)).thenReturn(Collections.emptyList());

        service.add(1L, addReq(9L, 3));

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartRepository).insert(captor.capture());
        CartItem saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(3);
        assertThat(saved.getProductId()).isEqualTo(100L);
        assertThat(saved.getChecked()).isEqualTo(ValidFlag.ENABLED.code());
    }

    @Test
    void list_emptyCart_returnsEmptyView() {
        when(cartRepository.findByUser(1L)).thenReturn(Collections.emptyList());

        CartView view = service.list(1L);

        assertThat(view.getItems()).isEmpty();
        assertThat(view.getCheckedAmount()).isEqualByComparingTo("0");
        verify(skuRepository, never()).findByIds(anySet());
    }

    @Test
    void list_checkedValidItem_sumsCheckedAmount_skipsInvalid() {
        CartItem checked = new CartItem();
        checked.setId(1L);
        checked.setUserId(1L);
        checked.setSkuId(9L);
        checked.setProductId(100L);
        checked.setQuantity(2);
        checked.setChecked(ValidFlag.ENABLED.code());

        CartItem invalid = new CartItem();
        invalid.setId(2L);
        invalid.setUserId(1L);
        invalid.setSkuId(8L);
        invalid.setProductId(200L);
        invalid.setQuantity(1);
        invalid.setChecked(ValidFlag.ENABLED.code());

        when(cartRepository.findByUser(1L)).thenReturn(Arrays.asList(checked, invalid));
        // sku 8 缺失 → invalid 项，sku 9 有效
        when(skuRepository.findByIds(anySet())).thenReturn(Collections.singletonList(sku(9L, 100L, "10.00", 50)));
        when(productRepository.findByIds(anySet())).thenReturn(Collections.singletonList(product(100L, true, null)));

        CartView view = service.list(1L);

        assertThat(view.getItems()).hasSize(2);
        // 仅 sku9 计入：10 * 2 = 20
        assertThat(view.getCheckedAmount()).isEqualByComparingTo("20.00");
        assertThat(view.getCheckedCount()).isEqualTo(2);
        assertThat(view.getTotalCount()).isEqualTo(3);
    }

    @Test
    void remove_delegatesToRepository() {
        lenient().when(cartRepository.findByUser(anyLong())).thenReturn(Collections.emptyList());

        service.remove(1L, Arrays.asList(1L, 2L));

        verify(cartRepository).deleteByUserAndIds(1L, Arrays.asList(1L, 2L));
    }
}
