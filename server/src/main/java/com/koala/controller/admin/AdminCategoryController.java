package com.koala.controller.admin;

import com.koala.common.result.Result;
import com.koala.common.web.IdRequest;
import com.koala.dto.product.CategorySaveRequest;
import com.koala.dto.product.CategoryView;
import com.koala.dto.product.SortRequest;
import com.koala.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "后台-商品分类")
@RestController
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表(含停用)")
    @GetMapping
    public Result<List<CategoryView>> list() {
        return Result.success(categoryService.listAll());
    }

    @Operation(summary = "新增/编辑分类")
    @PostMapping("/save")
    public Result<Long> save(@Valid @RequestBody CategorySaveRequest req) {
        return Result.success(categoryService.save(req));
    }

    @Operation(summary = "删除分类(商品数0可删)")
    @PostMapping("/delete")
    public Result<Void> delete(@Valid @RequestBody IdRequest req) {
        categoryService.delete(req.getId());
        return Result.success();
    }

    @Operation(summary = "批量排序")
    @PostMapping("/sort")
    public Result<Void> sort(@Valid @RequestBody SortRequest req) {
        categoryService.sort(req.getItems());
        return Result.success();
    }
}
