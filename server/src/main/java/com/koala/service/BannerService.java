package com.koala.service;

import com.koala.dto.content.BannerSaveRequest;
import com.koala.dto.content.BannerView;
import com.koala.dto.product.SortItem;

import java.util.List;

public interface BannerService {

    /** C端：上线 Banner(sort_order 升序)。 */
    List<BannerView> listValid();

    /** 后台：全部 Banner(含下线)。 */
    List<BannerView> listAll();

    /** 后台：新增/编辑，返回主键。 */
    Long save(BannerSaveRequest req);

    /** 后台：删除。 */
    void delete(Long id);

    /** 后台：批量排序。 */
    void sort(List<SortItem> items);
}
