package com.koala.dto.product;

import com.koala.entity.ProductCategory;
import lombok.Data;

@Data
public class CategoryView {

    private Long id;
    private String name;
    private String iconUrl;
    private Integer sortOrder;
    private boolean valid;

    public static CategoryView of(ProductCategory c) {
        CategoryView v = new CategoryView();
        v.id = c.getId();
        v.name = c.getName();
        v.iconUrl = c.getIconUrl();
        v.sortOrder = c.getSortOrder();
        v.valid = c.getIsValid() != null && c.getIsValid() == 1;
        return v;
    }
}
