package com.koala.service;

import com.koala.dto.product.SortItem;
import com.koala.dto.product.TagSaveRequest;
import com.koala.dto.product.TagView;

import java.util.List;

public interface TagService {

    /** 后台：全部标签。 */
    List<TagView> listAll();

    /** 后台：新增/编辑（名称唯一），返回主键。 */
    Long save(TagSaveRequest req);

    /** 后台：删除（仅引用数为0可删）。 */
    void delete(Long id);

    /** 后台：批量排序。 */
    void sort(List<SortItem> items);
}
