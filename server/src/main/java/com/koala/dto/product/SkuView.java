package com.koala.dto.product;

import com.koala.entity.ProductSku;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SkuView {

    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;

    public static SkuView of(ProductSku s) {
        SkuView v = new SkuView();
        v.id = s.getId();
        v.name = s.getName();
        v.price = s.getPrice();
        v.stock = s.getStock();
        return v;
    }
}
