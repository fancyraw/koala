package com.koala.dto.content;

import com.koala.entity.Banner;
import lombok.Data;

@Data
public class BannerView {

    private Long id;
    private String imageUrl;
    private String linkUrl;
    private Integer sortOrder;
    private boolean valid;

    public static BannerView of(Banner b) {
        BannerView v = new BannerView();
        v.id = b.getId();
        v.imageUrl = b.getImageUrl();
        v.linkUrl = b.getLinkUrl();
        v.sortOrder = b.getSortOrder();
        v.valid = b.getIsValid() != null && b.getIsValid() == 1;
        return v;
    }
}
