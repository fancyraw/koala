package com.koala.dto.product;

import com.koala.entity.ProductTag;
import lombok.Data;

@Data
public class TagView {

    private Long id;
    private String name;
    private Integer sortOrder;
    private boolean valid;

    public static TagView of(ProductTag t) {
        TagView v = new TagView();
        v.id = t.getId();
        v.name = t.getName();
        v.sortOrder = t.getSortOrder();
        v.valid = t.getIsValid() != null && t.getIsValid() == 1;
        return v;
    }
}
