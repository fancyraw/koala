package com.koala.service;

import com.koala.dto.product.CategorySaveRequest;
import com.koala.dto.product.CategoryView;
import com.koala.dto.product.SortItem;

import java.util.List;

public interface CategoryService {

    /** C端：启用分类列表（sort_order 升序）。 */
    List<CategoryView> listValid();

    /** 后台：全部分类（含停用）。 */
    List<CategoryView> listAll();

    /** 后台：新增/编辑，返回主键。 */
    Long save(CategorySaveRequest req);

    /** 后台：删除（仅商品数为0可删）。 */
    void delete(Long id);

    /** 后台：批量排序。 */
    void sort(List<SortItem> items);
}
